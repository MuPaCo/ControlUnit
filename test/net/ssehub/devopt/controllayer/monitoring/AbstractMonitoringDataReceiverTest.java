/*
 * Copyright 2022 University of Hildesheim, Software Systems Engineering
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.ssehub.devopt.controllayer.monitoring;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * This abstract class realizes preparation and post-processing methods, which are necessary for tests involving the
 * {@link MonitoringDataReceiver} instance. Hence, each specific test class requiring that instance should extend this
 * abstract class.
 * 
 * @author kroeher
 *
 */
public class AbstractMonitoringDataReceiverTest {

    /**
     * The local reference to the global {@link MonitoringDataReceiver} instance. This instance is created exactly once,
     * if the {@link AbstractMonitoringDataReceiverTest} constructor is called by one of the extending classes for the
     * first time.
     */
    protected static MonitoringDataReceiver testMonitoringDataReceiverInstance;
    
    /**
     * Creates the {@link #testMonitoringDataReceiverInstance}.
     */
    @BeforeClass
    public static void createInstance() {
        if (testMonitoringDataReceiverInstance != null) {
            fail("Monitoring data receiver instance already created");
        }
        MonitoringDataReceiver.createInstance();
        testMonitoringDataReceiverInstance = MonitoringDataReceiver.getInstance();
    }
    
    /**
     * Deletes the {@link #testMonitoringDataReceiverInstance}.
     */
    @AfterClass
    public static void deleteInstance() {
        if (testMonitoringDataReceiverInstance == null) {
            fail("Monitoring data receiver instance already deleted");
        }
        try {
            testMonitoringDataReceiverInstance.stop();
        } catch (MonitoringException e) {
            fail("Stopping monitoring data receiver instance failed unexpectedly", e);
        }
        testMonitoringDataReceiverInstance = null;
    }
    
}
