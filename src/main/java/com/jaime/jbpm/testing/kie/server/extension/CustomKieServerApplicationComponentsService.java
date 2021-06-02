package com.jaime.jbpm.testing.kie.server.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.kie.server.services.api.KieServerApplicationComponentsService;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jaime.jbpm.testing.kie.server.extension.resources.ProcessInstanceResource;
import com.jaime.jbpm.testing.kie.server.extension.resources.ProcessModelsResource;
import com.jaime.jbpm.testing.kie.server.extension.resources.UserTaskResource;

/**
 * This class is in charge of registering the new Resources we want to publish,
 * aka wrapper
 * <p>
 * This extends KieServerApplicationComponentsService which has a method that
 * the is invoked when the kie server is launched: getAppComponents
 * <p>
 * In this methods we can scan the diferent services that have been launched
 * and, in this case, we use some of then to launch our resources (that extends
 * the ProcessResource and the UserTaskResource).
 * <p>
 * The services we need are for our resources are:
 * <p>
 * * BBVACustomResource: ProcessService, DefinitionService, KieServerRegistry,
 * RuntimeDataService * BBVACustomTaskResouce: UserTaskResourc,
 * KieServerRegistry
 * <p>
 * Once finished we return our new components as a list of object.
 */
public class CustomKieServerApplicationComponentsService implements KieServerApplicationComponentsService {

	private static final String OWNER_EXTENSION = "jBPM";

	private static final Logger LOGGING = LoggerFactory.getLogger(CustomKieServerApplicationComponentsService.class);

	public Collection<Object> getAppComponents(String extension, SupportedTransports type, Object... services) {
		// skip calls from other than owning extension
		if (!OWNER_EXTENSION.equals(extension)) {
			return Collections.emptyList();
		}

		List<Object> components = new ArrayList<Object>();
		KieServerRegistry kieServerRegistry = null;
		RuntimeDataService runtimeDataService = null;
		UserTaskService userTaskService = null;
		ProcessService processService = null;
		DefinitionService definitionService = null;
		KModuleDeploymentService kmService = null;

		/*
		 * List of available services:
		 * 
		 * Service org.jbpm.kie.services.impl.KModuleDeploymentService Service
		 * org.jbpm.kie.services.impl.bpmn2.BPMN2DataServiceImpl Service
		 * org.jbpm.kie.services.impl.ProcessServiceImpl Service
		 * org.jbpm.kie.services.impl.UserTaskServiceImpl Service
		 * org.jbpm.kie.services.impl.RuntimeDataServiceImpl Service
		 * org.jbpm.executor.impl.ExecutorServiceImpl Service
		 * org.jbpm.kie.services.impl.FormManagerServiceImpl Service
		 * org.jbpm.kie.services.impl.query.QueryServiceImpl Service
		 * org.jbpm.kie.services.impl.admin.ProcessInstanceMigrationServiceImpl Service
		 * org.jbpm.kie.services.impl.admin.ProcessInstanceAdminServiceImpl Service
		 * org.jbpm.kie.services.impl.admin.UserTaskAdminServiceImpl Service
		 * org.kie.server.services.impl.KieServerRegistryImpl Service
		 * org.jbpm.kie.services.impl.AdvanceRuntimeDataServiceImpl
		 * 
		 */
		for (Object service : services) {
			LOGGING.info("Service " + service.getClass().getCanonicalName());
			if (KieServerRegistry.class.isAssignableFrom(service.getClass())) {
				kieServerRegistry = (KieServerRegistry) service;
			}

			if (UserTaskService.class.isAssignableFrom(service.getClass())) {
				userTaskService = (UserTaskService) service;
			}

			if (ProcessService.class.isAssignableFrom(service.getClass())) {
				processService = (ProcessService) service;
			}

			if (DefinitionService.class.isAssignableFrom(service.getClass())) {
				definitionService = (DefinitionService) service;
			}

			if (KModuleDeploymentService.class.isAssignableFrom(service.getClass())) {
				kmService = (KModuleDeploymentService) service;
			}
			
			if (RuntimeDataService.class.isAssignableFrom(service.getClass())) {
				runtimeDataService = (RuntimeDataService) service;
			}
		}

		components.add(new ProcessModelsResource(kieServerRegistry, definitionService, processService, kmService));
		components.add(new ProcessInstanceResource(processService, kieServerRegistry, kmService, definitionService));
		components.add(new UserTaskResource(kieServerRegistry, definitionService, runtimeDataService, userTaskService, kmService));

		return components;
	}

}