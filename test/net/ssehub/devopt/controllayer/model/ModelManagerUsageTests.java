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
package net.ssehub.devopt.controllayer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.AllTests;

/**
 * This class contains unit tests for the usage of the {@link ModelManager}. 
 *  
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class ModelManagerUsageTests extends AbstractModelManagerTests {

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The file denoting the IVML model file, which is used to load an IVML model string that in turn is input to
     *     {@link ModelManager#modelReceived(String)} during {@link #testModelReceived()} </li>
     * <li>The expected number of {@link EntityInfo} instances available by the {@link ModelReceiver}</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {"ProjectMissingClosingBracket.ivml", 0},
            {"InvalidDevOptProject.ivml", 0},
            {"EmptyProject.ivml", 0},
            {"MonitoringHiveBrokerDevOptProject.ivml", 1},
            {"MonitoringDevOptProject.ivml", 2},
            {"MonitoringDevOptProject.ivml", 2}, /* Same count, if adding same model twice */
    };
    
    /**
     * The string representation of the IVML model used for the current test iteration.
     */
    private String testIvmlModel;
    
    /**
     * The expected number of {@link EntityInfo} instances available by the {@link ModelReceiver}.
     */
    private int expectedAvailableModelCount;

    /**
     * Constructs a new {@link ModelManagerUsageTests} instance for testing one particular subset of the
     * {@link #TEST_DATA}.
     * 
     * @param testIvmlModelFileName the string representation of the IVML model used for the current test iteration
     * @param expectedAvailableModelCount the expected number of {@link EntityInfo} instances available by the
     *        {@link ModelReceiver}
     */
    public ModelManagerUsageTests(String testIvmlModelFileName, int expectedAvailableModelCount) {
        super(); // Creates the test model manager instance, if not already created by previous tests
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        testIvmlModel = getIvmlModelString(testIvmlModelFile);
        this.expectedAvailableModelCount = expectedAvailableModelCount;
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
     * Tests whether {@link ModelManager#getEntityInfoCount()} is equal to the {@link #expectedAvailableModelCount}.As
     * the model manager creates keys to retrieve {@link EntityInfo} instance internally based on the current time, it
     * is not possible to check for correct addition of a new model (entity information) by retrieving that instance.
     * The necessary key is not known in this class. 
     */
    @Test
    public void testModelCount() {
        testModelManagerInstance.modelReceived(testIvmlModel);
        assertEquals(expectedAvailableModelCount, testModelManagerInstance.getEntityInfoCount(),
                "Incorrect number of available entity information");
    }
    
}
