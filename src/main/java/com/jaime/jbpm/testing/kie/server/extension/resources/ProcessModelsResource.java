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
	
	private Map<String, String> mapaEquivalenciasClasesPrimitivas = new HashMap<String, String>();

	public ProcessModelsResource(KieServerRegistry kieServerRegistry, DefinitionService definitionService, ProcessService processService) {
		this.kieServerRegistry = kieServerRegistry;
		this.definitionService = definitionService;
		this.processService = processService;
	}
	
	{
		mapaEquivalenciasClasesPrimitivas.put("String", "java.lang.String");
		mapaEquivalenciasClasesPrimitivas.put("Boolean", "java.lang.Boolean");
		mapaEquivalenciasClasesPrimitivas.put("Integer", "java.lang.Integer");
		mapaEquivalenciasClasesPrimitivas.put("Float", "java.lang.Float");
		mapaEquivalenciasClasesPrimitivas.put("Object", "java.lang.Object");
	}

	@POST
	@Path("/containers/{containerId}/process-models/{process-model-id}/versions/{version}:instance")
	@Produces({MediaType.APPLICATION_JSON})
	public Response startProcessInstance(@Context HttpHeaders headers, @PathParam("containerId") String
			containerId, @PathParam("process-model-id") String
			processModelId, @PathParam("version") String version, String payload) {

		// Find kie-container associated with processModelVersion.
		KieContainerInstance kieContainerInstance = kieServerRegistry.getContainer(containerId);
		ClassLoader kieContainerClassLoader = kieContainerInstance.getKieContainer().getClassLoader();
		Set<Class<?>> classes = kieContainerInstance.getExtraClasses();
		Map<String, Object> instanceInputs = new HashMap<String, Object>();


		System.out.println("Comenzamos a iterar clases extra");
		for (Class<?> class1 : classes) {
			System.out.println("Clase extra: " + class1.getPackageName() + "." + class1.getName());
		}
		System.out.println("Fin iteración clases extra");

		ObjectMapper mapper = new ObjectMapper();
		try {
			ProcessDefinition def = definitionService.getProcessDefinition(containerId, processModelId);
			Map<String, String> processDefinitionVariables = def.getProcessVariables();

			for (Entry<String, String> variableDefinition : processDefinitionVariables.entrySet()) {
				System.out.println("AAA VAR: " + variableDefinition.getKey() + "  ----  " + variableDefinition.getValue());
			}

			JsonNode rootNode = mapper.readTree(payload);
			
			Iterator<Entry<String, JsonNode>> iter = rootNode.fields();
			
			System.out.println("Inicio de la iteración del payload de entrada");
			while (iter.hasNext()) {
			    Entry<String, JsonNode> fieldSet = iter.next();
				
				try {
					String nombreClaseCampo = resolveClassName(processDefinitionVariables.get(fieldSet.getKey()));
					String nombreCampo = fieldSet.getKey();
					String valorCampo = fieldSet.getValue().toString();
					System.out.println("Se serializará el campo " + nombreCampo + " con el valor " + valorCampo + " a la clase " + nombreClaseCampo);
					
					instanceInputs.put(nombreCampo, mapper.readValue(valorCampo, Class.forName(nombreClaseCampo, true, kieContainerClassLoader)));
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			System.out.println("Final de la iteración del payload de entrada");

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
	
	private String resolveClassName(String initialClassName) {
		if(mapaEquivalenciasClasesPrimitivas.get(initialClassName) != null)
			return mapaEquivalenciasClasesPrimitivas.get(initialClassName);
		else
			return initialClassName;	
	}
}
