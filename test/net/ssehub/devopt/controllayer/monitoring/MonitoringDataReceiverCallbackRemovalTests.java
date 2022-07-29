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

import net.ssehub.devopt.controllayer.utilities.GenericCallback;

/**
 * This class contains units tests for the {@link MonitoringDataReceiver#removeCallback(GenericCallback)} method.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class MonitoringDataReceiverCallbackRemovalTests extends AbstractMonitoringDataReceiverTest {
    
    /**
     * One of two {@link GenericCallback} instances used in the tests of this class. Before any test is executed, this
     * instance will be added to the {@link MonitoringDataReceiver} via {@link #addTestCallbacks()}.
     */
    private static final GenericCallback<MonitoringData> TEST_CALLBACK_1 =
            new TestMonitoringDataReceptionCallback("Test Callback 1");
    
    /**
     * One of two {@link GenericCallback} instances used in the tests of this class. Before any test is executed, this
     * instance will be added to the {@link MonitoringDataReceiver} via {@link #addTestCallbacks()}.
     */
    private static final GenericCallback<MonitoringData> TEST_CALLBACK_2 =
            new TestMonitoringDataReceptionCallback("Test Callback 2");

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The {@link GenericCallback} instance to remove</li>
     * <li>The expected return value, if {@link MonitoringDataReceiver#removeCallback(GenericCallback)} is called with
     *     the current callback instance</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
        {null, false},
        {TEST_CALLBACK_1, true},
        {TEST_CALLBACK_1, false},
        {TEST_CALLBACK_2, true}
    };
    
    /**
     * The {@link GenericCallback} instance to remove.
     */
    private GenericCallback<MonitoringData> testCallback;
    
    /**
     * The expected return value, if {@link MonitoringDataReceiver#removeCallback(GenericCallback)} is called with the
     * current {@link #testCallback}.
     */
    private boolean expectedRemovalReturnValue;
    
    /**
     * Constructs a new {@link MonitoringDataReceiverCallbackRemovalTests} instance for testing one particular subset of
     * the {@link #TEST_DATA}.
     * 
     * @param testCallback the {@link GenericCallback} instance to remove
     * @param expectedRemovalReturnValue the expected return value, if
     *        {@link MonitoringDataReceiver#removeCallback(GenericCallback)} is called with the current
     *        {@link #testCallback}
     */
    public MonitoringDataReceiverCallbackRemovalTests(GenericCallback<MonitoringData> testCallback,
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
     * Adds {@link #TEST_CALLBACK_1} and {@link #TEST_CALLBACK_2} to the {@link MonitoringDataReceiver}.
     */
    @BeforeClass
    public static void addTestCallbacks() {
        if (!testMonitoringDataReceiverInstance.addCallback(TEST_CALLBACK_1)) {
            fail("Adding test callback \"" + TEST_CALLBACK_1 + "\" failed");
        }
        if (!testMonitoringDataReceiverInstance.addCallback(TEST_CALLBACK_2)) {
            fail("Adding test callback \"" + TEST_CALLBACK_2 + "\" failed");
        }
    }
    
    /**
     * Tests whether {@link MonitoringDataReceiver#removeCallback(GenericCallback)} returns the
     * {@link #expectedRemovalReturnValue}, if the {@link #testCallback} is used as parameter.
     */
    @Test
    public void testRemoveCallback() {
        assertEquals(expectedRemovalReturnValue, testMonitoringDataReceiverInstance.removeCallback(testCallback),
                "Wrong callback removal result");
    }
    
}
