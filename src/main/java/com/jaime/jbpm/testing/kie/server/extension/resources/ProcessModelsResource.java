package com.jaime.jbpm.testing.kie.server.extension.resources;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.model.ProcessDefinition;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("business-process-management/v1")
public class ProcessModelsResource {

	private KieServerRegistry kieServerRegistry = null;
	private DefinitionService definitionService = null;
	private ProcessService processService = null;
	
	private Map<String, String> primitiveClassesTranslatorMap = new HashMap<String, String>();

	public ProcessModelsResource(KieServerRegistry kieServerRegistry, DefinitionService definitionService, ProcessService processService) {
		this.kieServerRegistry = kieServerRegistry;
		this.definitionService = definitionService;
		this.processService = processService;
	}
	
	{
		// Adding primitive types as defined in the BPMN file and their equivalent types in Java.
		primitiveClassesTranslatorMap.put("String", "java.lang.String");
		primitiveClassesTranslatorMap.put("Boolean", "java.lang.Boolean");
		primitiveClassesTranslatorMap.put("Integer", "java.lang.Integer");
		primitiveClassesTranslatorMap.put("Float", "java.lang.Float");
		primitiveClassesTranslatorMap.put("Object", "java.lang.Object");
	}

	@POST
	@Path("/containers/{containerId}/process-models/{process-model-id}/versions/{version}:instance")
	@Produces({MediaType.APPLICATION_JSON})
	public Response startProcessInstance(@Context HttpHeaders headers, @PathParam("containerId") String
			containerId, @PathParam("process-model-id") String
			processModelId, @PathParam("version") String version, String payload) {

		// Retrieving ClassLoader from kieContainerInstance.
		KieContainerInstance kieContainerInstance = kieServerRegistry.getContainer(containerId);
		ClassLoader kieContainerClassLoader = kieContainerInstance.getKieContainer().getClassLoader();
		Map<String, Object> instanceInputs = new HashMap<String, Object>();

		ObjectMapper mapper = new ObjectMapper();
		
		try {
			// Retrieving process definition to introspect explicitely-defined variables.
			ProcessDefinition def = definitionService.getProcessDefinition(containerId, processModelId);
			Map<String, String> processDefinitionVariables = def.getProcessVariables();

			JsonNode rootNode = mapper.readTree(payload);
			
			Iterator<Entry<String, JsonNode>> iter = rootNode.fields();
			
			System.out.println("Starting input payload iteration");
			while (iter.hasNext()) {
			    Entry<String, JsonNode> fieldSet = iter.next();
				
				try {
					// Class name will be 
					//    a) The FQCN as defined in the BPMN, 
					//    b) the primitive type with its FQCN retrieved from primitiveClassesTranslatorMap, or 
					//    c) java.lang.Object if variable is not defined in the BPMN.
					String fieldClassName = resolveClassName(processDefinitionVariables.get(fieldSet.getKey()));
					String fieldName = fieldSet.getKey();
					String fieldValue = fieldSet.getValue().toString();
					System.out.println("About serializing " + fieldName + " with value " + fieldValue + " regarding class " + fieldClassName);
					
					instanceInputs.put(fieldName, mapper.readValue(fieldValue, Class.forName(fieldClassName, true, kieContainerClassLoader)));
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			System.out.println("End of input payload iteration");

		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Long processInstanceId = processService.startProcess(containerId, processModelId, instanceInputs);
		System.out.println("Process instance " + processInstanceId + " started");
		return Response.status(Status.CREATED).build();
	}

	/**
	 * 
	 * @param initialClassName
	 * @return the class name that will be used to create the instance of an object with reflection.
	 */
	private String resolveClassName(String initialClassName) {
		if(initialClassName == null)
			return "java.lang.Object";
		else if(primitiveClassesTranslatorMap.get(initialClassName) != null)
			return primitiveClassesTranslatorMap.get(initialClassName);
		else
			return initialClassName;	
	}
}