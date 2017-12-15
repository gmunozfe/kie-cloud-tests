/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.cloud.performancetests.timers;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.kie.cloud.api.DeploymentScenarioBuilderFactory;
import org.kie.cloud.api.scenario.WorkbenchWithKieServerScenario;
import org.kie.cloud.common.time.TimeUtils;
import org.kie.cloud.maven.constants.MavenConstants;
import org.kie.cloud.performancetests.AbstractCloudPerformanceTest;
import org.kie.cloud.performancetests.util.RunnableWrapper;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.controller.management.client.KieServerMgmtControllerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimersAtSinglePointOfTimeIntegrationTest extends AbstractCloudPerformanceTest<WorkbenchWithKieServerScenario> {

    private static final Logger logger = LoggerFactory.getLogger(TimersAtSinglePointOfTimeIntegrationTest.class);

    private static final int KIE_SERVER_INSTANCES_COUNT = 2;

    private static final String BRMS_PERF_PREFIX = "brmsperf";
    private static final String PERF_LAB_SUFFIX = ".mw.lab.eng.bos.redhat.com";

    private static final String KIE_SERVER_PORT = "8080";
    private static final String KIE_SERVER_CONTEXT = "/kie-server/services/rest/server";
    private static final String KIE_SERVER_USER = "kieserver";
    private static final String KIE_SERVER_PASSWORD = "kieserver1!";
    private static final int KIE_SERVER_TIMEOUT = 300000;

    private static final String KIE_ROUTER_PORT = "9000";
    private static final String KIE_ROUTER_NODE = "01";

    private static final int PROCESSES_COUNT = Integer.parseInt(System.getProperty("processesCount"));
    private static final int STARTING_THREADS_COUNT = 20;
    private static final int PROCESSES_PER_THREAD = PROCESSES_COUNT / STARTING_THREADS_COUNT;

    public static final double PERF_INDEX = Double.parseDouble(System.getProperty("perfIndex", "3.0"));


    private KieServerMgmtControllerClient kieServerMgmtControllerClient;
    private KieServicesClient kieServicesClient;
    private ProcessServicesClient processServicesClient;
    private QueryServicesClient queryServicesClient;


    /**
     * Since we are not running openshift, we will skip deployment handling for now
     */
    @Override
    public void initializeDeployment() {
        /*for (String node: new String[] {"01", "02"}) {
            KieServicesClient kieServicesClient = createKieServerClient(BRMS_PERF_PREFIX + node + PERF_LAB_SUFFIX + ":" + KIE_SERVER_PORT + KIE_SERVER_CONTEXT,
                    KIE_SERVER_USER, KIE_SERVER_PASSWORD);
            ServiceResponse<KieContainerResource> response = kieServicesClient.createContainer(CONTAINER_ID, new KieContainerResource(CONTAINER_ID, new ReleaseId(PROJECT_GROUP_ID, PROJECT_NAME, PROJECT_VERSION)));
            Assertions.assertThat(response.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);
        }*/
    }

    @Override
    public void cleanEnvironment() {
        /*for (String node: new String[] {"01", "02"}) {
            KieServicesClient kieServicesClient = createKieServerClient(BRMS_PERF_PREFIX + node + PERF_LAB_SUFFIX + ":" + KIE_SERVER_PORT + KIE_SERVER_CONTEXT,
                    KIE_SERVER_USER, KIE_SERVER_PASSWORD);
            kieServicesClient.listContainers().getResult().getContainers().forEach(container -> kieServicesClient.disposeContainer(container.getContainerId()));
        }*/
    }

    @Override
    protected WorkbenchWithKieServerScenario createDeploymentScenario(DeploymentScenarioBuilderFactory deploymentScenarioFactory) {
        return deploymentScenarioFactory.getWorkbenchWithKieServerScenarioBuilder()
                .withExternalMavenRepo(MavenConstants.getMavenRepoUrl(), MavenConstants.getMavenRepoUser(), MavenConstants.getMavenRepoPassword())
                .build();
    }

    @Before
    public void setUp() {
        /*kieServerMgmtControllerClient = KieServerControllerClientProvider.getKieServerMgmtControllerClient(deploymentScenario.getWorkbenchDeployment());



        kieServicesClient = KieServerClientProvider.getKieServerClient(deploymentScenario.getKieServerDeployment());
        processServicesClient = KieServerClientProvider.getProcessClient(deploymentScenario.getKieServerDeployment());
        queryServicesClient = KieServerClientProvider.getQueryClient(deploymentScenario.getKieServerDeployment());

        MavenDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource(PROJECT_PATH).getFile());

        KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();
        WorkbenchUtils.saveContainerSpec(kieServerMgmtControllerClient, serverInfo.getServerId(), serverInfo.getName(), CONTAINER_ID, CONTAINER_ALIAS, PROJECT_GROUP_ID, PROJECT_NAME, PROJECT_VERSION, KieContainerStatus.STARTED);
        KieServerClientProvider.waitForContainerStart(deploymentScenario.getKieServerDeployment(), CONTAINER_ID);

        logger.debug("Waiting for KIE Server deployment to scale to {} pods", KIE_SERVER_INSTANCES_COUNT);
        deploymentScenario.getKieServerDeployment().scale(KIE_SERVER_INSTANCES_COUNT);*/

        String kieRouterUrl = "http://" + BRMS_PERF_PREFIX + KIE_ROUTER_NODE + PERF_LAB_SUFFIX + ":" + KIE_ROUTER_PORT;
        logger.info("KIE Router Address: {}", kieRouterUrl);
        kieServicesClient = createKieServerClient(kieRouterUrl, KIE_SERVER_USER, KIE_SERVER_PASSWORD);

        processServicesClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
        queryServicesClient = kieServicesClient.getServicesClient(QueryServicesClient.class);

    }

    @Test
    public void testScenario() {
        logger.info("==== STARTING SCENARIO ====");
        runSingleScenario(false);
        logger.info("==== SCENARIO COMPLETE ====");

        logger.info("==== GATHERING STATISTICS ====");
        gatherAndAssertStatistics();
        logger.info("==== STATISTICS GATHERED ====");
    }

    // ========== HELPER METHODS ==========

    private void runSingleScenario(boolean warmUp) {
        OffsetDateTime fireAtTime = calculateFireAtTime();

        logger.info("Starting {} processes", PROCESSES_COUNT);

        Instant startTime = Instant.now();
        // This is just to provide virtually "infinite" time to finish all iterations, i.e. maximum possible duration minus 1 hour,
        // so we can be sure there won't be overflow
        Duration maxDuration = Duration.between(startTime.plus(1, ChronoUnit.HOURS), Instant.MAX);
        startAndWaitForStartingThreads(STARTING_THREADS_COUNT, maxDuration, PROCESSES_PER_THREAD, getStartingRunnable(fireAtTime));

        logger.info("Starting processes took: {}", Duration.between(startTime, Instant.now()));

        Assertions.assertThat(Instant.now()).isBefore(fireAtTime.toInstant());

        Duration waitForCompletionDuration = Duration.between(Instant.now(), fireAtTime).plus(Duration.of(30, ChronoUnit.MINUTES));

        logger.info("Waiting for process instances to be completed, max waiting time is {}", waitForCompletionDuration);
        waitForAllProcessesToComplete(waitForCompletionDuration);
        logger.info("Process instances completed, took approximately {}", Duration.between(fireAtTime.toInstant(), Instant.now()));

    }


    private void startAndWaitForStartingThreads(int numberOfThreads, Duration duration, Integer iterations, Runnable runnable) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            Thread t = new Thread(new RunnableWrapper(duration, iterations, runnable));
            t.start();
            threads.add(t);
            logger.debug("Thread no. " + i + " started." );
        }

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private KieServicesClient createKieServerClient(String url, String username, String password) {
        KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(url, username, password, KIE_SERVER_TIMEOUT);

        return KieServicesFactory.newKieServicesClient(configuration);
    }

    private OffsetDateTime calculateFireAtTime() {
        OffsetDateTime currentTime = OffsetDateTime.now();
        long offsetInSeconds = Math.round(PROCESSES_COUNT / STARTING_THREADS_COUNT / PERF_INDEX);
        OffsetDateTime fireAtTime = currentTime.plus(offsetInSeconds, ChronoUnit.SECONDS);
        logger.info("fireAtTime set to {} which is the offset of {} seconds", fireAtTime, offsetInSeconds);

        return fireAtTime;
    }

    private Runnable getStartingRunnable(OffsetDateTime fireAtTime) {
        return () -> {
            try {
                long pid = processServicesClient.startProcess(CONTAINER_ID, ONE_TIMER_PROCESS_ID, Collections.singletonMap("fireAt", fireAtTime.toString()));
                logger.trace("Process with process id {} created.", pid);
            } catch (KieServicesHttpException e) {
                logger.error("There has been an error while starting processes", e);
                throw e;
            }
        };
    }

    private void waitForAllProcessesToComplete(Duration waitForCompletionDuration) {
        BooleanSupplier completionCondition = () -> queryServicesClient.findProcessInstancesByStatus(Collections.singletonList(org.jbpm.process.instance.ProcessInstance.STATE_ACTIVE), 0, 1).isEmpty();
        TimeUtils.wait(waitForCompletionDuration, Duration.of(5, ChronoUnit.SECONDS), completionCondition);
    }

    private void updateDistribution(Map<String, Integer> distribution, String hostName) {
        Integer count = distribution.get(hostName);
        if (count == null) {
            distribution.put(hostName, 1);
        } else {
            distribution.put(hostName, ++count);
        }
    }

    private void gatherAndAssertStatistics() {
        // to speed up pagination
        KieServicesClient kieStatisticsClient = createKieServerClient("http://" + BRMS_PERF_PREFIX + "01" + PERF_LAB_SUFFIX + ":" + KIE_SERVER_PORT + KIE_SERVER_CONTEXT, KIE_SERVER_USER, KIE_SERVER_PASSWORD);
        QueryServicesClient queryClient = kieStatisticsClient.getServicesClient(QueryServicesClient.class);

        List<ProcessInstance> activeProcesses = queryClient.findProcessInstancesByStatus(Collections.singletonList(org.jbpm.process.instance.ProcessInstance.STATE_ACTIVE), 0, 10);
        logger.debug("Active processes count: {}", activeProcesses.size());
        Assertions.assertThat(activeProcesses).isEmpty();

        /*QueryDefinition query = new QueryDefinition();
        query.setName("completedProcessInstances");
        query.setSource("java:jboss/datasources/jbpmDS");
        query.setExpression("select processinstanceid from ProcessInstanceLog where status = 2");
        query.setTarget("PROCESS");

        queryClient.registerQuery(query);*/

        int numberOfPages = 1 + (PROCESSES_COUNT / 5000);// including one additional page to check there are no more processes

        List<ProcessInstance> completedProcesses = new ArrayList<>(PROCESSES_COUNT);

        for (int i = 0; i < numberOfPages; i++) {
            logger.debug("Receiving page no. {}", i);
            List<ProcessInstance> response = queryClient.findProcessInstancesByStatus(Collections.singletonList(org.jbpm.process.instance.ProcessInstance.STATE_COMPLETED), i, 5000);
            //List<ProcessInstance> response = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, i, 5000, ProcessInstance.class);
            logger.debug("Received page no. {}", i);

            logger.debug("Adding page no. {} to collection", i);
            completedProcesses.addAll(response);
            logger.debug("Page no. {} added to collection", i);
        }

        logger.debug("Completed processes count: {}", completedProcesses.size());
        Assertions.assertThat(completedProcesses).hasSize(PROCESSES_COUNT);

        Map<String, Integer> startedHostNameDistribution = new HashMap<>();
        Map<String, Integer> completedHostNameDistribution = new HashMap<>();

        int i = 0;
        for (ProcessInstance pi : completedProcesses) {
            long pid = pi.getId();
            List<VariableInstance> hostNameHistory = queryClient.findVariableHistory(pid, "hostName", 0, 10);
            Assertions.assertThat(hostNameHistory).hasSize(2);
            // Values are DESC by ID, i.e. time
            updateDistribution(startedHostNameDistribution, hostNameHistory.get(0).getOldValue());
            updateDistribution(completedHostNameDistribution, hostNameHistory.get(0).getValue());

            if (++i % 1000 == 0) {
                logger.debug("{} processes validated", i);
            }
        }

        logger.info("Processes were started with this distribution: {}", startedHostNameDistribution);
        logger.info("Processes were completed with this distribution: {}", completedHostNameDistribution);
    }

}
