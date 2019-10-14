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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.cloud.tests.common.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class JbpmEJBTimersPartialFailoverPerfIntegrationTest extends JbpmEJBSinglePointTimersPerfIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(JbpmEJBTimersPartialFailoverPerfIntegrationTest.class);

    protected static final int PROCESSES_PER_THREAD = 1000;
    protected static final int STARTING_THREADS_COUNT = PROCESSES_COUNT / PROCESSES_PER_THREAD;
    
    protected static final String CSV_FILE = "./failoverTimer__"+PROCESSES_COUNT+"_processes__"+SCALE_COUNT+"_pods.csv";
    
    protected static final int DOWNSCALE_OFFSET = 10;
    
    private String startingTime, processTime;
    
    
    @Before
    public void checkParams() {
        if (SCALE_COUNT <= 1) {
            throw new RuntimeException("wrong scale parameter, should be greater than 1 for this scenario");
        }
    }
    
    @Override
    protected void runSingleScenario() {
        OffsetDateTime fireAtTime = calculateFireAtTime();
        OffsetDateTime downScaleTime = calculateDownScaleTime(fireAtTime);
        logger.info("Starting {} processes", PROCESSES_COUNT);
        
        Instant startTime = Instant.now();
        Duration maxDuration = Duration.between(startTime.plus(1, ChronoUnit.HOURS), Instant.MAX);
        Map<String, Object> params = Collections.singletonMap("fireAt", fireAtTime.toString());
        
        logger.info("Starting timers-testing.OneTimerDate");
        
        startAndWaitForStartingThreads(STARTING_THREADS_COUNT, maxDuration, PROCESSES_PER_THREAD, getStartingRunnable(CONTAINER_ID, "timers-testing.OneTimerDate", params));
        startingTime = Duration.between(startTime, Instant.now()).toString();
        logger.info("Starting processes took: {}", startingTime);

        assertThat(Instant.now()).isBefore(fireAtTime.toInstant());

        Duration waitForCompletionDuration = Duration.between(Instant.now(), fireAtTime).plus(Duration.of(40, ChronoUnit.MINUTES));

        TimeUtils.wait(Duration.between(Instant.now(), fireAtTime.toInstant()));
        
        logger.info("Waiting for process instances to be completed, max waiting time is {}", waitForCompletionDuration);
        TimeUtils.wait(Duration.between(Instant.now(), downScaleTime.toInstant()));
        
        logger.info("Downscaling delay done");
        scaleKieServerTo(SCALE_COUNT-1);
        
        waitForAllProcessesToComplete(waitForCompletionDuration);
        processTime = Duration.between(fireAtTime.toInstant(), Instant.now()).toString();
        logger.info("Process instances completed, took approximately {}", processTime);
    }
    
        
    private OffsetDateTime calculateDownScaleTime(OffsetDateTime fireAtTime) {
        OffsetDateTime downScaleTime = fireAtTime.plus(DOWNSCALE_OFFSET, ChronoUnit.SECONDS);
        logger.info("failoverTime set to {} which is the offset of {} seconds", downScaleTime, DOWNSCALE_OFFSET);

        return downScaleTime;
    }
    
   
}
