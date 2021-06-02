package com.jaime.jbpm.testing.kie.server.extension.resources;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.POST;
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
import org.jbpm.services.api.model.ProcessDefinition;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.marshal.MarshallerHelper;

import com.bbva.ether.bpmaas.marshalling.json.JsonCustomMarshallerHelper;

@Path("business-process-management/v1")
public class ProcessModelsResource {

	private KieServerRegistry kieServerRegistry = null;
	private DefinitionService definitionService = null;
	private ProcessService processService = null;
	private KModuleDeploymentService kmoduleDeploymentService = null;

	public ProcessModelsResource(KieServerRegistry kieServerRegistry, DefinitionService definitionService, ProcessService processService, KModuleDeploymentService kmoduleDeploymentService) {
		this.kieServerRegistry = kieServerRegistry;
		this.definitionService = definitionService;
		this.kmoduleDeploymentService = kmoduleDeploymentService;
		this.processService = processService;
	}

	@POST
	@Path("/containers/{containerId}/process-models/{process-model-id}/versions/{version}:instance")
	@Produces({MediaType.APPLICATION_JSON})
	public Response startProcessInstance(@Context HttpHeaders headers, @PathParam("containerId") String
			containerId, @PathParam("process-model-id") String
			processModelId, @PathParam("version") String version, String payload) {

		// Retrieving ClassLoader from kieContainerInstance.
		KieContainerInstance kieContainerInstance = kieServerRegistry.getContainer(containerId);
		Boolean customBBVAUnmarshallingMode = false;

		customBBVAUnmarshallingMode = 
				(Boolean)((InternalRuntimeManager)kmoduleDeploymentService.getRuntimeManager(kieContainerInstance.getContainerId()))
				.getEnvironment().getEnvironment().get("BBVACustomUnmarshallingMode");

		System.out.println("Unmarshalling mode: " + customBBVAUnmarshallingMode);

		Map<String, Object> instanceInputs = new HashMap<String, Object>();

		if(customBBVAUnmarshallingMode == null || !customBBVAUnmarshallingMode) {
			MarshallerHelper marshallerHelper = new MarshallerHelper(kieServerRegistry);

			instanceInputs = marshallerHelper.unmarshal(containerId, payload, MarshallingFormat.JSON.getType(), Map.class);
		}
		else {
			ClassLoader kieContainerClassLoader = kieContainerInstance.getKieContainer().getClassLoader();
			
			// Retrieving process definition to introspect explicitely-defined variables.
			ProcessDefinition def = definitionService.getProcessDefinition(containerId, processModelId);
			Map<String, String> processDefinitionVariables = def.getProcessVariables();
			
			instanceInputs = JsonCustomMarshallerHelper.unmarshall(payload, processDefinitionVariables, kieContainerClassLoader);
		}

		Long processInstanceId = processService.startProcess(containerId, processModelId, instanceInputs);
		System.out.println("Process instance " + processInstanceId + " started");
		return Response.status(Status.CREATED).build();
	}
}