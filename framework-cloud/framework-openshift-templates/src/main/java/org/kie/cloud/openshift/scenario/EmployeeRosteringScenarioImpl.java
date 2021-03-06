/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.cloud.openshift.scenario;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.cloud.api.deployment.Deployment;
import org.kie.cloud.api.deployment.EmployeeRosteringDeployment;
import org.kie.cloud.api.scenario.EmployeeRosteringScenario;
import org.kie.cloud.openshift.constants.OpenShiftConstants;
import org.kie.cloud.openshift.constants.OpenShiftTemplateConstants;
import org.kie.cloud.openshift.deployment.EmployeeRosteringDeploymentImpl;
import org.kie.cloud.openshift.deployment.external.ExternalDeployment;
import org.kie.cloud.openshift.deployment.external.ExternalDeploymentTemplates;
import org.kie.cloud.openshift.template.OpenShiftTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmployeeRosteringScenarioImpl extends OpenShiftScenario<EmployeeRosteringScenario> implements EmployeeRosteringScenario {

    private static final String OPTAWEB_HTTPS_SECRET = "OPTAWEB_HTTPS_SECRET";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeRosteringScenarioImpl.class);

    private Map<String, String> env = new HashMap<>();
    private EmployeeRosteringDeployment employeeRosteringDeployment;

    @Override
    protected void deployKieDeployments() {
        this.employeeRosteringDeployment = new EmployeeRosteringDeploymentImpl(project);

        env.put(OpenShiftTemplateConstants.IMAGE_STREAM_NAMESPACE, project.getName());
        env.put(OpenShiftTemplateConstants.POSTGRESQL_IMAGE_STREAM_NAMESPACE, project.getName());
        env.put(OPTAWEB_HTTPS_SECRET, OpenShiftConstants.getKieApplicationSecretName());

        project.processTemplateAndCreateResources(OpenShiftTemplate.OPTAWEB_EMPLOYEE_ROSTERING.getTemplateUrl(), env);

        LOGGER.info("Waiting for OptaWeb Employee Rostering deployment to become ready.");
        employeeRosteringDeployment.waitForScale();
        LOGGER.info("Waiting for OptaWeb Employee Rostering has been deployed.");
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void configureWithExternalDeployment(ExternalDeployment<?, ?> externalDeployment) {
        ((ExternalDeploymentTemplates) externalDeployment).configure(env);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void removeConfigurationFromExternalDeployment(ExternalDeployment<?, ?> externalDeployment) {
        ((ExternalDeploymentTemplates) externalDeployment).removeConfiguration(env);
    }

    @Override
    public List<Deployment> getDeployments() {
        return Collections.singletonList(employeeRosteringDeployment);
    }

    public EmployeeRosteringDeployment getEmployeeRosteringDeployment() {
        return this.employeeRosteringDeployment;
    }
}
