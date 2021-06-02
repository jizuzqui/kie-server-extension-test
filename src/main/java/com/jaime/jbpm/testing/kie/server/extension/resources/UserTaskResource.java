package com.jaime.jbpm.testing.kie.server.extension.resources;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.kie.services.impl.model.UserTaskInstanceDesc;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.UserTaskDefinition;
import org.kie.api.task.model.Task;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.marshal.MarshallerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.ether.bpmaas.marshalling.json.JsonCustomMarshallerHelper;

/**
 * Representes the Task resource.
 * <p>
 * The different operations that can be done with a task are in Restfull form:
 * GET - to search tasks GET {task-id} - to retrieve task data POST start a task
 * stop a task claim a task release a task assign a task complete a task suspend
 * a task cancel a task resume a task PATCH update task data cancel
 */
@Path("business-process-management/v1")
public class UserTaskResource {

	private Logger logger = LoggerFactory.getLogger(UserTaskResource.class);

	private UserTaskService userTaskService = null;
	private KieServerRegistry kieServerRegistry = null;
	private DefinitionService definitionService = null;
	private KModuleDeploymentService kmoduleDeploymentService = null;
	private RuntimeDataService runtimeDataService = null;

	public UserTaskResource(KieServerRegistry kieServerRegistry, DefinitionService definitionService, RuntimeDataService runtimeDataService, UserTaskService userTaskService,
			KModuleDeploymentService kmoduleDeploymentService) {
		this.kieServerRegistry = kieServerRegistry;
		this.definitionService = definitionService;
		this.runtimeDataService = runtimeDataService;
		this.userTaskService = userTaskService;
		this.kmoduleDeploymentService = kmoduleDeploymentService;
	}

	@POST
	@Path("/containers/{containerId}/tasks/{task-id}:complete")
	@Produces({MediaType.APPLICATION_JSON})
	public Response actionComplete(@Context HttpHeaders headers, @PathParam("containerId") String containerId, @PathParam("task-id") Long taskId, String payload) {
		logger.info("[BBVA] Executing Complete Task [{}]", taskId);

			Boolean customBBVAUnmarshallingMode = false;

			customBBVAUnmarshallingMode = (Boolean)((InternalRuntimeManager)kmoduleDeploymentService.getRuntimeManager(containerId))
					.getEnvironment()
					.getEnvironment()
					.get(JsonCustomMarshallerHelper.BBVA_MARSHALLING_ENVIRONMENT_ENTRY);

			Map<String, Object> taskOutputs = new HashMap<String, Object>();

			if(customBBVAUnmarshallingMode == null || !customBBVAUnmarshallingMode) {

				// Unmarshalling data objects with jBPM default Marshaller Helper.
				logger.debug("Using jBPM default unmarshalling mechanism");

				MarshallerHelper marshallerHelper = new MarshallerHelper(kieServerRegistry);
				taskOutputs = marshallerHelper.unmarshal(containerId, payload, MarshallingFormat.JSON.getType(), Map.class);
			}
			else {
				// Unmarshalling data objects with BBVA custom Marshaller Helper.
				logger.debug("Using BBVA custom unmarshalling mechanism");

				// Assuming that the Composite Task Id contiains the containerID, we retrieve the KieContainerInstance from it.
				KieContainerInstance kieContainerInstance = kieServerRegistry.getContainer(containerId);

				ClassLoader kieContainerClassLoader = kieContainerInstance.getKieContainer().getClassLoader();

				// Retrieving process definition to introspect explicitely-defined variables.
				Task userTaskInstance = userTaskService.getTask(containerId, taskId);

				Collection<UserTaskDefinition> taskDefinitionCollection = definitionService.getTasksDefinitions(containerId, userTaskInstance.getTaskData().getProcessId());

				Map<String, String> taskDefinitionVariables = new HashMap<String, String>();

				for (Iterator<UserTaskDefinition> userTaskDefinitionIterator = taskDefinitionCollection.iterator(); userTaskDefinitionIterator.hasNext();) {
					UserTaskDefinition userTaskDefinition = (UserTaskDefinition)userTaskDefinitionIterator.next();

					if(userTaskDefinition.getName().equals(userTaskInstance.getName())) {

						taskDefinitionVariables = userTaskDefinition.getTaskOutputMappings();
					}
				}

				taskOutputs = JsonCustomMarshallerHelper.unmarshall(payload, taskDefinitionVariables, kieContainerClassLoader);
			}

			userTaskService.complete(containerId, taskId, null, taskOutputs);

			return Response.ok().build();

	}

	@PUT
	@Path("/containers/{containerId}/tasks/{task-id}")
	@Produces({MediaType.APPLICATION_JSON})
	public Response updateTask(@Context HttpHeaders headers, @PathParam("containerId") String containerId, @PathParam("task-id") Long taskId, String payload) {
		logger.info("Executing Update Task {}", taskId);

		Boolean customBBVAUnmarshallingMode = false;

		customBBVAUnmarshallingMode = (Boolean)((InternalRuntimeManager)kmoduleDeploymentService.getRuntimeManager(containerId))
				.getEnvironment()
				.getEnvironment()
				.get(JsonCustomMarshallerHelper.BBVA_MARSHALLING_ENVIRONMENT_ENTRY);

		Map<String, Object> taskOutputs = new HashMap<String, Object>();

		if(customBBVAUnmarshallingMode == null || !customBBVAUnmarshallingMode) {

			// Unmarshalling data objects with jBPM default Marshaller Helper.
			logger.debug("Using jBPM default unmarshalling mechanism");

			MarshallerHelper marshallerHelper = new MarshallerHelper(kieServerRegistry);
			taskOutputs = marshallerHelper.unmarshal(containerId, payload, MarshallingFormat.JSON.getType(), Map.class);
		}
		else {
			// Unmarshalling data objects with BBVA custom Marshaller Helper.
			logger.debug("Using BBVA custom unmarshalling mechanism");

			// Assuming that the Composite Task Id contiains the containerID, we retrieve the KieContainerInstance from it.
			KieContainerInstance kieContainerInstance = kieServerRegistry.getContainer(containerId);

			ClassLoader kieContainerClassLoader = kieContainerInstance.getKieContainer().getClassLoader();

			// Retrieving process definition to introspect explicitely-defined variables.
			Task userTask = userTaskService.getTask(containerId, taskId);
			Map<String, String> processDefinitionVariables = definitionService.getTaskOutputMappings(containerId, userTask.getTaskData().getProcessId(), userTask.getName());

			taskOutputs = JsonCustomMarshallerHelper.unmarshall(payload, processDefinitionVariables, kieContainerClassLoader);
		}

		UserTaskInstanceDesc userTaskInstanceDesc = (UserTaskInstanceDesc)runtimeDataService.getTaskById(taskId);

		userTaskService.updateTask(containerId, taskId, null, userTaskInstanceDesc, null, taskOutputs);

		return Response.ok().build();

	}

}