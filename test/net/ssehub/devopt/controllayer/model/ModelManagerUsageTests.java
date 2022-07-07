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
     * <li>The expected identifier of the entity described by the received model; must be <code>null</code>, if model
     *     reception is expected to fail</li>
     * <li>The expected number of {@link EntityInfo} instances available by the {@link ModelReceiver}</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {"ProjectMissingClosingBracket.ivml", null, 0},
            {"InvalidDevOptProject.ivml", null, 0},
            {"EmptyProject.ivml", null, 0},
            {"MonitoringDevOptProject.ivml", "System under Monitoring", 1},
            {"MonitoringDevOptProject.ivml", "System under Monitoring", 1}, /* Same count, if adding same model twice */
    };
    
    /**
     * The string representation of the IVML model used for the current test iteration.
     */
    private String testIvmlModel;
    
    /**
     * The expected identifier of the entity described by the received model; may be <code>null</code>, if model
     * reception is expected to fail.
     */
    private String expectedReceivedModelIdentifier;
    
    /**
     * The expected number of {@link EntityInfo} instances available by the {@link ModelReceiver}.
     */
    private int expectedAvailableModelCount;

    /**
     * Constructs a new {@link ModelManagerUsageTests} instance for testing one particular subset of the
     * {@link #TEST_DATA}.
     * 
     * @param testIvmlModelFileName the string representation of the IVML model used for the current test iteration
     * @param expectedReceivedModelIdentifier the expected identifier of the entity described by the received model; may
     *        be <code>null</code>, if model reception is expected to fail
     * @param expectedAvailableModelCount the expected number of {@link EntityInfo} instances available by the
     *        {@link ModelReceiver}
     */
    public ModelManagerUsageTests(String testIvmlModelFileName, String expectedReceivedModelIdentifier,
            int expectedAvailableModelCount) {
        super(); // Creates the test model manager instance, if not already created by previous tests
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        testIvmlModel = getIvmlModelString(testIvmlModelFile);
        this.expectedReceivedModelIdentifier = expectedReceivedModelIdentifier;
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
     * Tests whether the call of {@link ModelManager#modelReceived(String)} with the {@link #testIvmlModel} results in
     * the {@link #expectedReceivedModelIdentifier} identifying an available {@link EntityInfo} instance in the
     * {@link ModelManager} instance.
     */
    @Test
    public void testModelReceived() {
        testModelManagerInstance.modelReceived(testIvmlModel);
        assertEquals(expectedReceivedModelIdentifier, getAvailableEntityInfoIdentifier(expectedReceivedModelIdentifier),
                "Incorrect available entity information");
        
    }
    
    /**
     * Tests whether {@link ModelManager#getEntityInfoCount()} is equal to the {@link #expectedAvailableModelCount}.
     */
    @Test
    public void testModelCount() {
        assertEquals(expectedAvailableModelCount, testModelManagerInstance.getEntityInfoCount(),
                "Incorrect number of available entity information");
    }
    
    /**
     * Returns the entity identifier of an {@link EntityInfo} instance available by the {@link ModelManager} instance,
     * if it is equal to the given identifier.
     *  
     * @param identifier the entity identifier to search for 
     * @return the available entity identifier matching the given one, or <code>null</code>, if no such entity
     *         identifier is available by the {@link ModelManager} instance
     */
    private String getAvailableEntityInfoIdentifier(String identifier) {
        String entityInfoIdentifier = null;
        int entityInfoCounter = 0;
        while (entityInfoIdentifier == null && entityInfoCounter < testModelManagerInstance.getEntityInfoCount()) {
            if (testModelManagerInstance.getEntityInfo(entityInfoCounter).getIdentifier().equals(identifier)) {
                entityInfoIdentifier = testModelManagerInstance.getEntityInfo(entityInfoCounter).getIdentifier();
            }
            entityInfoCounter++;
        }
        return entityInfoIdentifier;
    }
    
}
