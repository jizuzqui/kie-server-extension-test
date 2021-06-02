package com.jaime.jbpm.testing.kie.server.extension.resources;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessDefinition;
import org.jbpm.services.api.query.QueryService;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.marshal.MarshallerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.ether.bpmaas.marshalling.json.JsonCustomMarshallerHelper;

import io.swagger.annotations.ApiOperation;

@Path("business-process-management/v1")
public class ProcessInstanceResource  {

	private Logger logger = LoggerFactory.getLogger(ProcessInstanceResource.class);

	private ProcessService processService = null;
	private KieServerRegistry kieServerRegistry = null;
	private DefinitionService definitionService = null;
	private KModuleDeploymentService kmoduleDeploymentService = null;

	public ProcessInstanceResource(ProcessService processService, KieServerRegistry kieServerRegistry, KModuleDeploymentService kmoduleDeploymentService, 
			DefinitionService definitionService) {
		this.processService = processService;
		this.kmoduleDeploymentService = kmoduleDeploymentService;
		this.kieServerRegistry = kieServerRegistry;
		this.definitionService = definitionService;
	}

	@GET
	@Path("/containers/{containerId}/instances/{instance-id}/variables")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation( value = "GetProcessInstanceVariables", notes = "Get process instance variables ", response = Response.class, responseContainer = "" )
	public Response getProcessInstanceVariables (@Context HttpHeaders headers, @PathParam("containerId") String containerId, 
			@PathParam("instance-id") Long processInstanceId) {
		logger.info("Executing Get Process Variables[{}]", processInstanceId);
		
		String responsePayload = "{}";

		// Retrieving ClassLoader from kieContainerInstance.
		KieContainerInstance kieContainerInstance = kieServerRegistry.getContainer(containerId);
		Boolean customBBVAUnmarshallingMode = false;

		customBBVAUnmarshallingMode = 
				(Boolean)((InternalRuntimeManager)kmoduleDeploymentService.getRuntimeManager(kieContainerInstance.getContainerId()))
				.getEnvironment().getEnvironment().get("BBVACustomUnmarshallingMode");

		System.out.println("Unmarshalling mode: " + customBBVAUnmarshallingMode);

		Map<String, Object> instanceOutputs = processService.getProcessInstanceVariables(containerId, processInstanceId);

		if(customBBVAUnmarshallingMode == null || !customBBVAUnmarshallingMode) {
			MarshallerHelper marshallerHelper = new MarshallerHelper(kieServerRegistry);

			responsePayload = marshallerHelper.marshal(containerId, MarshallingFormat.JSON.getType(), instanceOutputs);
		}
		else {
//			ClassLoader kieContainerClassLoader = kieContainerInstance.getKieContainer().getClassLoader();

			responsePayload = JsonCustomMarshallerHelper.marshall(instanceOutputs);
		}

		return Response.status(Status.OK).entity(responsePayload).build();

	}

	@PUT
	@Path("/containers/{containerId}/instances/{instance-id}/variables")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation( value = "UpdateProcessVariables", notes = "Update process variables", response = Response.class, responseContainer = "" )
	public Response updateProcessVariables (@Context HttpHeaders headers, @PathParam("containerId") String containerId, @PathParam("instance-id") Long processInstanceId, String processVariables) {
		logger.info("Executing Update Process Variables[{}]", processInstanceId);

			KieContainerInstance kieContainerInstance = kieServerRegistry.getContainer(containerId);

			Boolean customBBVAUnmarshallingMode = false;

			customBBVAUnmarshallingMode = (Boolean)((InternalRuntimeManager)kmoduleDeploymentService.getRuntimeManager(containerId))
					.getEnvironment()
					.getEnvironment()
					.get(JsonCustomMarshallerHelper.BBVA_MARSHALLING_ENVIRONMENT_ENTRY);

			Map<String, Object> instanceInputs = new HashMap<String, Object>();

			if(customBBVAUnmarshallingMode == null || !customBBVAUnmarshallingMode) {

				// Unmarshalling data objects with jBPM default Marshaller Helper.
				logger.debug("Using jBPM default unmarshalling mechanism");

				MarshallerHelper marshallerHelper = new MarshallerHelper(kieServerRegistry);
				instanceInputs = marshallerHelper.unmarshal(containerId, processVariables, MarshallingFormat.JSON.getType(), Map.class);
			}
			else {

				// Unmarshalling data objects with BBVA custom Marshaller Helper.
				logger.debug("Using BBVA custom unmarshalling mechanism");

				ClassLoader kieContainerClassLoader = kieContainerInstance.getKieContainer().getClassLoader();

				// Retrieving process definition to introspect explicitely-defined variables.
				//				ProcessDefinition def = processService.getProcessInstance(processInstanceId).getProcess(); 
				ProcessDefinition def = definitionService.getProcessDefinition(containerId, processService.getProcessInstance(processInstanceId).getProcessId());
				Map<String, String> processDefinitionVariables = def.getProcessVariables();

				instanceInputs = JsonCustomMarshallerHelper.unmarshall(processVariables, processDefinitionVariables, kieContainerClassLoader);
			}

			logger.info("Update Process Variables Inicio: ", processInstanceId);

			processService.setProcessVariables(containerId, processInstanceId, instanceInputs);
			
			return Response.ok().build();

	}
}