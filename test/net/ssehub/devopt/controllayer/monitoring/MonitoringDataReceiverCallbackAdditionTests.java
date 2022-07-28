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

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.utilities.GenericCallback;

/**
 * This class contains units tests for the {@link MonitoringDataReceiver#addCallback(GenericCallback)} method.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class MonitoringDataReceiverCallbackAdditionTests {
    
    /**
     * One of two {@link GenericCallback} instances used in the tests of this class. After all tests are
     * done, all instances will be removed from the {@link MonitoringDataReceiver} via {@link #removeTestCallbacks()}.
     */
    private static final GenericCallback<MonitoringData> TEST_CALLBACK_1 =
            new TestMonitoringDataReceptionCallback("Test Callback 1");
    
    /**
     * One of two {@link GenericCallback} instances used in the tests of this class. After all tests are
     * done, all instances will be removed from the {@link MonitoringDataReceiver} via {@link #removeTestCallbacks()}.
     */
    private static final GenericCallback<MonitoringData> TEST_CALLBACK_2 =
            new TestMonitoringDataReceptionCallback("Test Callback 2");

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The {@link GenericCallback} instance to add</li>
     * <li>The expected return value, if {@link MonitoringDataReceiver#addCallback(GenericCallback)} is called with the
     *     current callback instance</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
        {null, false},
        {TEST_CALLBACK_1, true},
        {TEST_CALLBACK_1, false},
        {TEST_CALLBACK_2, true}
    };
    
    /**
     * The {@link GenericCallback} instance to add.
     */
    private GenericCallback<MonitoringData> testCallback;
    
    /**
     * The expected return value, if {@link MonitoringDataReceiver#addCallback(GenericCallback)} is called with the
     * current {@link #testCallback}.
     */
    private boolean expectedAdditionReturnValue;
    
    /**
     * Constructs a new {@link MonitoringDataReceiverCallbackAdditionTests} instance for testing one particular subset
     * of the {@link #TEST_DATA}.
     * 
     * @param testCallback the {@link GenericCallback} instance to add
     * @param expectedAdditionReturnValue the expected return value, if
     *        {@link MonitoringDataReceiver#addCallback(GenericCallback)} is called with the current
     *        {@link #testCallback}
     */
    public MonitoringDataReceiverCallbackAdditionTests(GenericCallback<MonitoringData> testCallback,
            boolean expectedAdditionReturnValue) {
        this.testCallback = testCallback;
        this.expectedAdditionReturnValue = expectedAdditionReturnValue;
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
     * Removes {@link #TEST_CALLBACK_1} and {@link #TEST_CALLBACK_2} from the {@link MonitoringDataReceiver}.
     */
    @AfterClass
    public static void removeTestCallbacks() {
        if (!MonitoringDataReceiver.getInstance().removeCallback(TEST_CALLBACK_1)) {
            fail("Removing test callback \"" + TEST_CALLBACK_1 + "\" failed");
        }
        if (!MonitoringDataReceiver.getInstance().removeCallback(TEST_CALLBACK_2)) {
            fail("Removing test callback \"" + TEST_CALLBACK_2 + "\" failed");
        }
    }
    
    /**
     * Tests whether {@link MonitoringDataReceiver#addCallback(GenericCallback)} returns the
     * {@link #expectedAdditionReturnValue}, if the {@link #testCallback} is used as parameter.
     */
    @Test
    public void testAddCallback() {
        assertEquals(expectedAdditionReturnValue, MonitoringDataReceiver.getInstance().addCallback(testCallback),
                "Wrong callback addition result");
    }
    
}
