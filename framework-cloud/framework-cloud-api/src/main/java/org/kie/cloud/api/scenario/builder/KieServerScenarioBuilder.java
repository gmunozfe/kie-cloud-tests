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

package org.kie.cloud.api.scenario.builder;

import org.kie.cloud.api.scenario.KieServerScenario;
import org.kie.cloud.api.settings.LdapSettings;

public interface KieServerScenarioBuilder extends KieDeploymentScenarioBuilder<KieServerScenarioBuilder, KieServerScenario> {

    /**
     * Return setup builder with additional configuration of external maven repo.
     * @param repoUrl Maven repo URL.
     * @param repoUserName Maven repo user name.
     * @param repoPassword Maven repo user password.
     * @return Builder with configured external maven repo.
     */
    KieServerScenarioBuilder withExternalMavenRepo(String repoUrl, String repoUserName, String repoPassword);

    /**
     * @param kieServerId kie-server id
     * @return Builder with environment variable for kie-server id set to given id
     */
    KieServerScenarioBuilder withKieServerId(String kieServerId);

    /**
     * Return configured builder with Kie Container deployment.
     *
     * @param kieContainerDeployment Kie Container deployment.
     * @return Builder with configured Kie container deployment
     */
    KieServerScenarioBuilder withContainerDeployment(String kieContainerDeployment);

    /**
     * Return setup builder with additional configuration for SSO deployment.
     *
     * @return Builder
     */
    KieServerScenarioBuilder deploySso();

    /**
     * Return setup builder with configure Kie Server http hostname.
     *
     * @param hostname HTTP hostname for Kie Server
     * @return Builder
     */
    KieServerScenarioBuilder withHttpKieServerHostname(String hostname);

    /**
     * Return setup builder with configure Kie Server https hostname.
     *
     * @param hostname HTTPS hostname for Kie Server
     * @return Builder
     */
    KieServerScenarioBuilder withHttpsKieServerHostname(String hostname);

    /**
     *
     * Return setup builder with configured LDAP.
     *
     * @param ldapSettings configuration of LDAP represented by a class.
     * @return Builder
     */
    KieServerScenarioBuilder withLdapSettings(LdapSettings ldapSettings);
}
