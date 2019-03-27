/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.cloud.integrationtests.s2i;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kie.cloud.api.DeploymentScenarioBuilderFactory;
import org.kie.cloud.api.DeploymentScenarioBuilderFactoryLoader;
import org.kie.cloud.api.deployment.Instance;
import org.kie.cloud.api.scenario.GenericScenario;
import org.kie.cloud.api.settings.DeploymentSettings;
import org.kie.cloud.api.settings.builder.KieServerS2ISettingsBuilder;
import org.kie.cloud.common.provider.KieServerClientProvider;
import org.kie.cloud.integrationtests.Kjar;
import org.kie.cloud.integrationtests.category.JBPMOnly;
import org.kie.cloud.integrationtests.util.RunnableWrapper;
import org.kie.cloud.integrationtests.util.TimeUtils;
import org.kie.cloud.provider.git.Git;
import org.kie.cloud.tests.common.AbstractMethodIsolatedCloudIntegrationTest;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.util.QueryFilterSpecBuilder;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.process.instance.ProcessInstance.*;

@RunWith(Parameterized.class)
public class KieServerS2iJbpmEJBTimerPerfIntegrationTest extends AbstractMethodIsolatedCloudIntegrationTest<GenericScenario> {

    private static final List<Integer> ACTIVE_STATUS = Collections.singletonList(STATE_ACTIVE);
    private static final List<Integer> COMPLETED_STATUS = Collections.singletonList(STATE_COMPLETED);
    
	private static final Logger logger = LoggerFactory.getLogger(KieServerS2iJbpmEJBTimerPerfIntegrationTest.class);

    protected static final int PROCESSES_COUNT = Integer.parseInt(System.getProperty("processesCount", "40000"));
    protected static final int STARTING_THREADS_COUNT = 40;
    protected static final int PROCESSES_PER_THREAD = PROCESSES_COUNT / STARTING_THREADS_COUNT;
    protected static final double PERF_INDEX = Double.parseDouble(System.getProperty("perfIndex", "2.0"));
    
    protected static final int SCALE_COUNT = Integer.parseInt(System.getProperty("scale", "1"));
    protected static final int REPETITIONS = Integer.parseInt(System.getProperty("repetitions", "1"));
    private static final String HEAP = System.getProperty("heap","4Gi");
    protected static final String SAMPLE_CSV_FILE = "./ejbTimer__"+PROCESSES_COUNT+"_processes__"+SCALE_COUNT+"_pods.csv";
    
    @Parameter(value = 0)
    public String testScenarioName;

    @Parameter(value = 1)
    public KieServerS2ISettingsBuilder kieServerS2ISettingsBuilder;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> scenarios = new ArrayList<>();
        DeploymentScenarioBuilderFactory deploymentScenarioFactory = DeploymentScenarioBuilderFactoryLoader.getInstance();

        try {
            KieServerS2ISettingsBuilder kieServerHttpsS2ISettings = deploymentScenarioFactory.getKieServerHttpsS2ISettingsBuilder();
            for (int i=0;i<REPETITIONS;i++) {
            	scenarios.add(new Object[] { "KIE Server HTTPS S2I", kieServerHttpsS2ISettings });
            }
        } catch (UnsupportedOperationException ex) {
            logger.info("KIE Server HTTPS S2I is skipped.", ex);
        }

        return scenarios;
    }
    
    protected KieServicesClient kieServicesClient;
    protected ProcessServicesClient processServicesClient;
    protected UserTaskServicesClient taskServicesClient;
    
    protected QueryServicesClient queryServicesClient;

    private static final String KIE_CONTAINER_DEPLOYMENT = CONTAINER_ID + "=" + Kjar.DEFINITION.toString();

    private static final String REPO_BRANCH = "master";
    private static final String PROJECT_SOURCE_FOLDER = "/kjars-sources";
	

    private String repositoryName;
    
    private List<String> pods = new ArrayList<String>();
    
    private String startingTime, processTime;
    private Map<String, Integer> completedHostNameDistribution;

    @Override
    protected GenericScenario createDeploymentScenario(DeploymentScenarioBuilderFactory deploymentScenarioFactory) {
        
    	
    	logger.info("git provider:"+Git.getProvider());
    	repositoryName = Git.getProvider().createGitRepositoryWithPrefix("KieServerS2iJbpmRepository", ClassLoader.class.getResource(PROJECT_SOURCE_FOLDER).getFile());

        DeploymentSettings kieServerS2Isettings = kieServerS2ISettingsBuilder
                .withContainerDeployment(KIE_CONTAINER_DEPLOYMENT)
                .withSourceLocation(Git.getProvider().getRepositoryUrl(repositoryName), REPO_BRANCH, DEFINITION_PROJECT_NAME)
                .withTimerServiceDataStoreRefreshInterval(Duration.ofSeconds(120))
                .withKieServerMemoryLimit("4Gi")
                .build();

        return deploymentScenarioFactory.getGenericScenarioBuilder()
                .withKieServer(kieServerS2Isettings)
                .build();
    }

    @Before
    public void setUp() {
    	
    	if (SCALE_COUNT > 1) {
    		logger.info("starting to scale");
    		scaleKieServerTo(SCALE_COUNT);
    		logger.info("scaled to {} pods", SCALE_COUNT);
    	}
        
    	deploymentScenario.getKieServerDeployments().get(0).setRouterTimeout(Duration.ofMinutes(60));
    	
    	deploymentScenario.getKieServerDeployments().get(0).setRouterBalance("roundrobin");
    	logger.info("balance roundrobin");
    	
        kieServicesClient = KieServerClientProvider.getKieServerClient(deploymentScenario.getKieServerDeployments().get(0), Duration.ofMinutes(60).toMillis());
        
        logger.info("Setting timeout for kieServerClient to {}", Duration.ofMinutes(60).toMillis());
        
        processServicesClient = KieServerClientProvider.getProcessClient(deploymentScenario.getKieServerDeployments().get(0));
        //taskServicesClient = KieServerClientProvider.getTaskClient(deploymentScenario.getKieServerDeployments().get(0));
        queryServicesClient = KieServerClientProvider.getQueryClient(deploymentScenario.getKieServerDeployments().get(0));
        //queryServicesClient = kieServicesClient.getServicesClient(QueryServicesClient.class);
        

        
    }

    @After
    public void deleteRepo() {
        Git.getProvider().deleteGitRepository(repositoryName);
        deploymentScenario.getKieServerDeployments().get(0).resetRouterTimeout();        
    }

    @Test
    @Category(JBPMOnly.class)
    public void testContainerAfterExecServerS2IStart() throws IOException {
        List<KieContainerResource> containers = kieServicesClient.listContainers().getResult().getContainers();
        assertThat(containers).isNotNull().hasSize(1);

        KieContainerResource container = containers.get(0);
        assertThat(container).isNotNull();
        assertThat(container.getContainerId()).isNotNull().isEqualTo(CONTAINER_ID);

        ReleaseId containerReleaseId = container.getResolvedReleaseId();
        assertThat(containerReleaseId).isNotNull();
        assertThat(containerReleaseId.getGroupId()).isNotNull().isEqualTo(PROJECT_GROUP_ID);
        assertThat(containerReleaseId.getArtifactId()).isNotNull().isEqualTo(DEFINITION_PROJECT_NAME);
        assertThat(containerReleaseId.getVersion()).isNotNull().isEqualTo(DEFINITION_PROJECT_VERSION);

        logger.info("============================= STARTING SCENARIO =============================");
	    runSingleScenario();
	    logger.info("============================= SCENARIO COMPLETE =============================");
	
	    logger.info("============================= GATHERING STATISTICS =============================");
	    gatherAndAssertStatistics();
	    writeCSV();
	    logger.info("============================= STATISTICS GATHERED =============================");
	    
    }

	private void writeCSV() throws IOException {
		List<String> headerCompleted = new ArrayList<String>();
		for (int i=1; i<=pods.size(); i++) {
			headerCompleted.add("Completed Pod"+i);
		}
		ArrayList<Object> record = new ArrayList<Object>();
		try (
	            //BufferedWriter writer = Files.newBufferedWriter(Paths.get(SAMPLE_CSV_FILE));
	            //CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
	            //       .withHeader("Processes", "Threads", "Starting Time", "Process Time", "Heap"));
				
				CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(Paths.get(SAMPLE_CSV_FILE).toAbsolutePath().toFile(), true), 
                        CSVFormat.DEFAULT);
	        ) {
			 record.add(PROCESSES_COUNT); 
			 record.add(STARTING_THREADS_COUNT);
			 record.add(startingTime.substring(2));
			 record.add(processTime.substring(2));
			 record.add(HEAP);
			 record.addAll(completedHostNameDistribution.values());
			 csvPrinter.printRecord(record);
	         csvPrinter.flush();            
	        }
		
	}

	private void runSingleScenario() {
		OffsetDateTime fireAtTime = calculateFireAtTime();
        logger.info("Starting {} processes", PROCESSES_COUNT);
        
        Instant startTime = Instant.now();
        Duration maxDuration = Duration.between(startTime.plus(1, ChronoUnit.HOURS), Instant.MAX);
        Map<String, Object> params = Collections.singletonMap("fireAt", fireAtTime.toString());
        
        logger.info("Starting timers-testing.OneTimerDate");
        
        startAndWaitForStartingThreads(STARTING_THREADS_COUNT, maxDuration, PROCESSES_PER_THREAD, getStartingRunnable(CONTAINER_ID, "timers-testing.OneTimerDate", params));
        startingTime = Duration.between(startTime, Instant.now()).toString();
        logger.info("Starting processes took: {}", startingTime);

        assertThat(Instant.now()).isBefore(fireAtTime.toInstant());

        
        Duration waitForCompletionDuration = Duration.between(Instant.now(), fireAtTime).plus(Duration.of(/*15*/ 40, ChronoUnit.MINUTES));

        TimeUtils.wait(Duration.between(Instant.now(), fireAtTime.toInstant()));
        
        logger.info("Waiting for process instances to be completed, max waiting time is {}", waitForCompletionDuration);
        waitForAllProcessesToComplete(waitForCompletionDuration);
        processTime = Duration.between(fireAtTime.toInstant(), Instant.now()).toString();
        logger.info("Process instances completed, took approximately {}", processTime);
	}
    
    private OffsetDateTime calculateFireAtTime() {
        OffsetDateTime currentTime = OffsetDateTime.now();
        long offsetInSeconds = Math.round(PROCESSES_COUNT / STARTING_THREADS_COUNT / PERF_INDEX);
        offsetInSeconds = offsetInSeconds<45 ? 45 : offsetInSeconds;
        OffsetDateTime fireAtTime = currentTime.plus(offsetInSeconds, ChronoUnit.SECONDS);
        logger.info("fireAtTime set to {} which is the offset of {} seconds", fireAtTime, offsetInSeconds);

        return fireAtTime;
    }
    
    protected void startAndWaitForStartingThreads(int numberOfThreads, Duration duration, Integer iterations, Runnable runnable) {
       List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            Thread t = new Thread(new RunnableWrapper(duration, iterations, runnable));
            t.start();
            threads.add(t);
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
                assertThat(pid).isNotNull();
            } catch (KieServicesHttpException e) {
                logger.error("There has been an error while starting processes", e);
                throw e;
            }
        };
    }
    
    protected void waitForAllProcessesToComplete(Duration waitForCompletionDuration) {
    	BooleanSupplier completionCondition = () -> queryServicesClient.findProcessInstancesByStatus(ACTIVE_STATUS, 0, 1).isEmpty();
        TimeUtils.wait(waitForCompletionDuration, Duration.of(1, ChronoUnit.SECONDS), completionCondition);
    }
    
    private void gatherAndAssertStatistics() {
        // to speed up pagination
        //QueryServicesClient queryClient = kieServicesClient.getServicesClient(QueryServicesClient.class);
    
        int numberOfPages = 1 + (PROCESSES_COUNT / 5000);// including one additional page to check there are no more processes

        List<ProcessInstance> completedProcesses = new ArrayList<>(PROCESSES_COUNT);

        for (int i = 0; i < numberOfPages; i++) {
            //logger.info("Receiving page no. {}", i);
            List<ProcessInstance> response = queryServicesClient.findProcessInstancesByStatus(Collections.singletonList(org.jbpm.process.instance.ProcessInstance.STATE_COMPLETED), i, 5000);
            //List<ProcessInstance> response = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, i, 5000, ProcessInstance.class);
            //logger.info("Received page no. {}", i);

            //logger.info("Adding page no. {} to collection", i);
            completedProcesses.addAll(response);
            //logger.info("Page no. {} added to collection", i);
        }

        logger.info("Completed processes count: {}", completedProcesses.size());
        
        List<ProcessInstance> activeProcesses = queryServicesClient.findProcessInstancesByStatus(Collections.singletonList(org.jbpm.process.instance.ProcessInstance.STATE_ACTIVE), 0, 100);
        logger.info("Active processes count: {}", activeProcesses.size());
        
        assertThat(activeProcesses).isEmpty();
        
        assertThat(completedProcesses).hasSize(PROCESSES_COUNT);

        
        Map<String, Integer> startedHostNameDistribution = new HashMap<>();
        completedHostNameDistribution = new HashMap<>();

        /*
        for (int i = 0; i < completedProcesses.size();) {
            ProcessInstance pi = completedProcesses.get(i);
            long pid = pi.getId();
            List<VariableInstance> hostNameHistory = queryServicesClient.findVariableHistory(pid, "hostName", 0, 10);
            assertThat(hostNameHistory).hasSize(2);
            // Values are DESC by ID, i.e. time
            updateDistribution(startedHostNameDistribution, hostNameHistory.get(0).getOldValue());
            updateDistribution(completedHostNameDistribution, hostNameHistory.get(0).getValue());

            if (++i % 1000 == 0) {
                logger.info("{} processes validated", i);
            }
        }

        logger.info("Processes were started with this distribution: {}", startedHostNameDistribution);
        logger.info("Processes were completed with this distribution: {}", completedHostNameDistribution);
        */
        
        for (String pod : pods) {
            completedHostNameDistribution.put(pod, 0);
            startedHostNameDistribution.put(pod, 0);
        }
        
        for (int i = 0; i < numberOfPages-1; i++) {
          for (String pod : pods) {
           int sizeCompleted = queryServicesClient.findProcessInstancesByVariableAndValue("hostName", pod, COMPLETED_STATUS, i, 5000).size();
           //logger.info("Processes completed page {}  hostName {}  size: {}", i, pod, sizeCompleted);
           completedHostNameDistribution.put(pod, completedHostNameDistribution.get(pod)+sizeCompleted);
           
           int sizeStarted = queryOldValue(i, pod);
           //logger.info("Processes started page {}  hostName {}  size: {}", i, pod, sizeStarted);
           startedHostNameDistribution.put(pod, startedHostNameDistribution.get(pod)+sizeStarted);
          }
        }
        
        logger.info("Processes were completed with this distribution: {}", completedHostNameDistribution);
        logger.info("Processes were started with this distribution: {}", startedHostNameDistribution);
        
    }
    
    
    private void updateDistribution(Map<String, Integer> distribution, String hostName) {
        Integer count = distribution.get(hostName);
        if (count == null) {
            distribution.put(hostName, 1);
        } else {
            distribution.put(hostName, ++count);
        }
    }
    
    private void scaleKieServerTo(int count) {
    	deploymentScenario.getKieServerDeployments().get(0).scale(count);
        deploymentScenario.getKieServerDeployments().get(0).waitForScale();
        List<Instance> osInstances = deploymentScenario.getKieServerDeployments().get(0).getInstances();
        for (Instance i : osInstances) {
        	pods.add(i.getName());
        	//logger.info("Running pods: {}", i);
        }
    }
    
    private int queryOldValue(int page, String oldValue) { 	
    	QueryFilterSpec spec = new QueryFilterSpecBuilder()
        		.equalsTo("variableId", "hostName")
        		.equalsTo("oldValue", oldValue)
        		.get();
          
        return queryServicesClient.query("jbpmOldValueVarSearch", QueryServicesClient.QUERY_MAP_PI_WITH_VARS, spec, page, 5000, ProcessInstance.class).size();
    }


}
