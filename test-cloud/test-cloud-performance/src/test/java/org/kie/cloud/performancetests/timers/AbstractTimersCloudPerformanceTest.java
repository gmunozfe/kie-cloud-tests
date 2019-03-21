package org.kie.cloud.performancetests.timers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.junit.Before;
import org.kie.cloud.api.DeploymentScenarioBuilderFactory;
import org.kie.cloud.api.scenario.WorkbenchWithKieServerScenario;
import org.kie.cloud.common.time.TimeUtils;
import org.kie.cloud.maven.constants.MavenConstants;
import org.kie.cloud.performancetests.AbstractCloudPerformanceTest;
import org.kie.cloud.performancetests.util.RunnableWrapper;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.controller.management.client.KieServerMgmtControllerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTimersCloudPerformanceTest extends AbstractCloudPerformanceTest<WorkbenchWithKieServerScenario> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTimersCloudPerformanceTest.class);

    private static final int KIE_SERVER_INSTANCES_COUNT = 2;

    protected static final String BRMS_PERF_PREFIX = "brmsperf";
    protected static final String PERF_LAB_SUFFIX = ".mw.lab.eng.bos.redhat.com";

    protected static final String KIE_SERVER_PORT = "8080";
    protected static final String KIE_SERVER_CONTEXT = "/kie-server/services/rest/server";
    protected static final String KIE_SERVER_USER = "kieserver";
    protected static final String KIE_SERVER_PASSWORD = "kieserver1!";
    protected static final int KIE_SERVER_TIMEOUT = 300000;

    private static final String KIE_ROUTER_PORT = "9000";
    private static final String KIE_ROUTER_NODE = "01";

    protected static final int PROCESSES_COUNT = Integer.parseInt(System.getProperty("processesCount"));
    protected static final int STARTING_THREADS_COUNT = 20;
    protected static final int PROCESSES_PER_THREAD = PROCESSES_COUNT / STARTING_THREADS_COUNT;

    public static final double PERF_INDEX = Double.parseDouble(System.getProperty("perfIndex", "3.0"));


    private KieServerMgmtControllerClient kieServerMgmtControllerClient;
    private KieServicesClient kieServicesClient;
    protected ProcessServicesClient processServicesClient;
    protected QueryServicesClient queryServicesClient;


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

    // ========== HELPER METHODS ==========

    protected KieServicesClient createKieServerClient(String url, String username, String password) {
        KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(url, username, password, KIE_SERVER_TIMEOUT);

        return KieServicesFactory.newKieServicesClient(configuration);
    }

    protected void startAndWaitForStartingThreads(int numberOfThreads, Duration duration, Integer iterations, Runnable runnable) {
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

    protected Runnable getStartingRunnable(String containerId, String processId, Map<String, Object> parameters) {
        return () -> {
            try {
                long pid = processServicesClient.startProcess(containerId, processId, parameters);
                logger.trace("Process with process id {} created.", pid);
            } catch (KieServicesHttpException e) {
                logger.error("There has been an error while starting processes", e);
                throw e;
            }
        };
    }

    protected void waitForAllProcessesToComplete(Duration waitForCompletionDuration) {
        BooleanSupplier completionCondition = () -> queryServicesClient.findProcessInstancesByStatus(Collections.singletonList(org.jbpm.process.instance.ProcessInstance.STATE_ACTIVE), 0, 1).isEmpty();
        TimeUtils.wait(waitForCompletionDuration, Duration.of(5, ChronoUnit.SECONDS), completionCondition);
    }


}
