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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class contains units tests for the
 * {@link MonitoringDataReceiver#removeCallback(MonitoringDataReceptionCallback)} method.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class MonitoringDataReceiverCallbackRemovalTests {

    /**
     * The {@link TestMonitoringDataReceptionCallback} instances used in the tests of this class. Before any test is
     * executed, all instances will be added to the {@link MonitoringDataReceiver} via {@link #addTestCallback()}.
     */
    private static final MonitoringDataReceptionCallback[] TEST_CALLBACKS = {
        new TestMonitoringDataReceptionCallback("Test Callback 1"),
        new TestMonitoringDataReceptionCallback("Test Callback 2")
    };

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The {@link TestMonitoringDataReceptionCallback} instance to remove</li>
     * <li>The expected return value, if {@link MonitoringDataReceiver#removeCallback(MonitoringDataReceptionCallback)}
     *     is called with the current callback instance</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
        {null, false},
        {TEST_CALLBACKS[0], true},
        {TEST_CALLBACKS[0], false},
        {TEST_CALLBACKS[1], true}
    };
    
    /**
     * The {@link TestMonitoringDataReceptionCallback} instance to remove.
     */
    private MonitoringDataReceptionCallback testCallback;
    
    /**
     * The expected return value, if {@link MonitoringDataReceiver#removeCallback(MonitoringDataReceptionCallback)} is
     * called with the current {@link #testCallback}.
     */
    private boolean expectedRemovalReturnValue;
    
    /**
     * Constructs a new {@link MonitoringDataReceiverCallbackRemovalTests} instance for testing one particular subset of
     * the {@link #TEST_DATA}.
     * 
     * @param testCallback the {@link TestMonitoringDataReceptionCallback} instance to remove
     * @param expectedRemovalReturnValue the expected return value, if
     *        {@link MonitoringDataReceiver#removeCallback(MonitoringDataReceptionCallback)} is called with the current
     *        {@link #testCallback}
     */
    public MonitoringDataReceiverCallbackRemovalTests(MonitoringDataReceptionCallback testCallback,
            boolean expectedRemovalReturnValue) {
        this.testCallback = testCallback;
        this.expectedRemovalReturnValue = expectedRemovalReturnValue;
    }
    
    /**
     * Returns the {@link #TEST_DATA} for the following tests in this class.
     * 
     * @return the {@link #TEST_DATA}
     */
    @Parameters
    public static Object[][] getTestData() {
        return TEST_DATA;
    }
    
    /**
     * Adds all {@link #TEST_CALLBACKS} to the {@link MonitoringDataReceiver}.
     */
    @BeforeClass
    public static void addTestCallback() {
        for (int i = 0; i < TEST_CALLBACKS.length; i++) {            
            if (!MonitoringDataReceiver.INSTANCE.addCallback(TEST_CALLBACKS[i])) {
                fail("Adding test callback \"" + TEST_CALLBACKS[i] + "\" failed");
            }
        }
    }
    
    /**
     * Tests whether {@link MonitoringDataReceiver#removeCallback(MonitoringDataReceptionCallback)} returns the
     * {@link #expectedRemovalReturnValue}, if the {@link #testCallback} is used as parameter.
     */
    @Test
    public void testRemoveCallback() {
        assertEquals(expectedRemovalReturnValue, MonitoringDataReceiver.INSTANCE.removeCallback(testCallback),
                "Wrong callback removal result");
    }
    
}
