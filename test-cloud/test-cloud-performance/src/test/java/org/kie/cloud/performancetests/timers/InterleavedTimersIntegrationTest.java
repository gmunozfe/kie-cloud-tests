package org.kie.cloud.performancetests.timers;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kie.cloud.common.time.TimeUtils;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterleavedTimersIntegrationTest extends AbstractTimersCloudPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(InterleavedTimersIntegrationTest.class);

    private static final int BATCH_COUNT = 5;
    private static final int BATCH_SIZE = 1000;
    private static final int BATCH_DELAY = 1000;
    private static final int TIMER_DELAY = 1;

    protected static final int PROCESSES_PER_THREAD = BATCH_SIZE / STARTING_THREADS_COUNT;

    protected static final String ONE_TIMER_DURATION_PROCESS_ID = "timers-testing.OneTimerDate";


    @Test
    public void testScenario() {
        logger.info("==== STARTING SCENARIO ====");
        runSingleScenario();
        logger.info("==== SCENARIO COMPLETE ====");

        logger.info("==== GATHERING STATISTICS ====");
        gatherAndAssertStatistics();
        logger.info("==== STATISTICS GATHERED ====");
    }

    // ========== HELPER METHODS ==========

    private void runSingleScenario() {
        Instant scenarioStartTime = Instant.now();

        logger.info("Starting {} batches", BATCH_COUNT);
        for (int i = 0; i < BATCH_COUNT;) {
            logger.info("Starting batch no. {}", i);

            logger.info("Starting {} processes", BATCH_SIZE);

            Instant startTime = Instant.now();
            // This is just to provide virtually "infinite" time to finish all iterations, i.e. maximum possible duration minus 1 hour,
            // so we can be sure there won't be overflow
            Duration maxDuration = Duration.between(startTime.plus(1, ChronoUnit.HOURS), Instant.MAX);
            Map<String, Object> params = Collections.singletonMap("timerDelay", TIMER_DELAY);

            startAndWaitForStartingThreads(STARTING_THREADS_COUNT, maxDuration, PROCESSES_PER_THREAD, getStartingRunnable(CONTAINER_ID, ONE_TIMER_DURATION_PROCESS_ID, params));

            logger.info("Starting processes took: {}", Duration.between(startTime, Instant.now()));

            Duration waitForCompletionDuration = Duration.of(20, ChronoUnit.MINUTES);
            logger.info("Batch created. Waiting for a batch to be processed, max waiting time is {}", waitForCompletionDuration);
            waitForAllProcessesToComplete(waitForCompletionDuration);

            logger.info("Batch no. {} processed, took approximately {}", i, Duration.between(startTime, Instant.now()));

            if (++i < BATCH_COUNT) {
                logger.info("Waiting for another batch to be run in {} seconds", BATCH_DELAY);
                TimeUtils.wait(Duration.of(BATCH_DELAY, ChronoUnit.SECONDS));
            }
        }
        logger.info("Scenario took: {}", Duration.between(scenarioStartTime, Instant.now()));





        /*OffsetDateTime fireAtTime = calculateFireAtTime();

        logger.info("Starting {} processes", PROCESSES_COUNT);

        Instant startTime = Instant.now();
        // This is just to provide virtually "infinite" time to finish all iterations, i.e. maximum possible duration minus 1 hour,
        // so we can be sure there won't be overflow
        Duration maxDuration = Duration.between(startTime.plus(1, ChronoUnit.HOURS), Instant.MAX);
        Map<String, Object> params = Collections.singletonMap("timerDelay", TIMER_DELAY);

        startAndWaitForStartingThreads(STARTING_THREADS_COUNT, maxDuration, PROCESSES_PER_THREAD, getStartingRunnable(CONTAINER_ID, ONE_TIMER_DATE_PROCESS_ID, params));

        logger.info("Starting processes took: {}", Duration.between(startTime, Instant.now()));

        Assertions.assertThat(Instant.now()).isBefore(fireAtTime.toInstant());

        Duration waitForCompletionDuration = Duration.between(Instant.now(), fireAtTime).plus(Duration.of(40, ChronoUnit.MINUTES));

        logger.info("Waiting for process instances to be completed, max waiting time is {}", waitForCompletionDuration);
        waitForAllProcessesToComplete(waitForCompletionDuration);
        logger.info("Process instances completed, took approximately {}", Duration.between(fireAtTime.toInstant(), Instant.now()));*/

    }

    private void updateDistribution(Map<String, Integer> distribution, String hostName) {
        distribution.merge(hostName, 1, (oldValue, newValue) -> ++oldValue);
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

        //int i = 0;
        for (int i = 0; i < completedProcesses.size();) {
            ProcessInstance pi = completedProcesses.get(i);
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
