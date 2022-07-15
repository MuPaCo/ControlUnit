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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.AbstractEASyBasedTests;
import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.devopt.controllayer.utilities.EASyUtilities;
import net.ssehub.devopt.controllayer.utilities.EASyUtilitiesException;

/**
 * This class contains unit tests for the {@link EntityInfo} class.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class EntityInfoTests extends AbstractEASyBasedTests {

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the expected model
     * information provided by the model utilities for the respective test iteration as well as the input for loading
     * the respective test model. For each subset, the values represent:
     * <ul>
     * <li>The name and extension of the IVML model file containing the IVML project definition to load</li>
     * <li>The name of the IVML project to load from the IVML model file</li>
     * <li>The text of the exception expected to be thrown during construction of the {@link #testEntityInfo} or
     *     <code>null</code>, if the construction must be successful</li>
     * <li>The expected entity identifier provided by the {@link #testEntityInfo} or <code>null</code>, if the
     *     construction of the {@link #testEntityInfo} must fail</li>
     * <li>The expected entity host (URL) provided by the {@link #testEntityInfo} or <code>null</code>, if the
     *     construction of the {@link #testEntityInfo} must fail</li>
     * <li>The expected entity port number provided by the {@link #testEntityInfo} or <code>-1</code>, if the
     *     construction of the {@link #testEntityInfo} must fail</li>
     * <li>The expected URL of the entity's monitoring scope provided by the {@link #testEntityInfo} or
     *     <code>null</code>, if the construction of the {@link #testEntityInfo} must fail</li>
     * <li>The expected port number of the entity's monitoring scope provided by the {@link #testEntityInfo} or
     *     <code>-1</code>, if the construction of the {@link #testEntityInfo} must fail</li>
     * <li>The expected channel of the entity's monitoring scope provided by the {@link #testEntityInfo} or
     *     <code>null</code>, if the construction of the {@link #testEntityInfo} must fail</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {"EmptyProject.ivml", "EmptyProject", "No entity defined in configuration", null, null, -1, null, -1, null},
            {"EntityEmptyDefinitionDevOptProject.ivml", "EntityEmptyDefinitionDevOptProject",
                "No identifier defined for entity", null, null, -1, null, -1, null},
            {"EntityEmptyIdentificationDescriptionDevOptProject.ivml",
                "EntityEmptyIdentificationDescriptionDevOptProject", "No identifier defined for entity", null, null, -1,
                null, -1, null},
            {"EntityEmptyIdentificationDevOptProject.ivml", "EntityEmptyIdentificationDevOptProject",
                "No identifier defined for entity", null, null, -1, null, -1, null},
            {"EntityIdentificationDevOptProject.ivml", "EntityIdentificationDevOptProject",
                "No monitoring scope defined for entity", null, null, -1, null, -1, null},
            {"IdentificationDevOptProject.ivml", "IdentificationDevOptProject",
                "No monitoring scope defined for entity", null, null, -1, null, -1, null},
            // Invalid identifier prevents establishing monitoring, but is valid regarding the meta model (currently)
            {"InvalidIdentifierDevOptProject.ivml", "MonitoringDevOptProject",
                null, "System Under Monitoring", "192.168.1.11", 1883, "tcp://broker.hivemq.com", 1883,
                "devopttestmonitoring"},
            {"MinimalDevOptProject.ivml", "MinimalDevOptProject", "No identifier defined for entity", null, null, -1,
                null, -1, null},
            {"MonitoringHttpDevOptProject.ivml", "MonitoringDevOptProject", null, "SystemUnderMonitoring",
                "192.168.1.11",  1883, "127.0.0.1", 80, "/http/context"},
            {"MonitoringMqttDevOptProject.ivml", "MonitoringHiveBrokerDevOptProject", null, "MonitoringViaHyve",
                "192.168.0.23", 1883, "tcp://broker.hivemq.com", 1883, "devopttestmonitoring"},
    };
    
    /**
     * The local reference to the global {@link EASyUtilities}.
     */
    private static EASyUtilities easyUtilities = EASyUtilities.INSTANCE;
    
    /**
     * The {@link EntityInfo} instance created based on the test IVML project loaded for the current test iteration.
     */
    private EntityInfo testEntityInfo;
    
    /**
     * The actual exception thrown during construction of the {@link #testEntityInfo}. Will be <code>null</code>, if
     * the construction was successful.
     */
    private ModelException actualException;
    
    /**
     * The text of the exception expected to be thrown during construction of the {@link #testEntityInfo}. Will be
     * <code>null</code>, if the construction must be successful.
     */
    private String expectedExceptionText;
    
    /**
     * The expected entity identifier provided by the {@link #testEntityInfo}. Will be <code>null</code>, if the
     * construction of the {@link #testEntityInfo} must fail.
     */
    private String expectedIdentifier;
    
    /**
     * The expected entity host (URL) provided by the {@link #testEntityInfo}. Will be <code>null</code>, if the
     * construction of the {@link #testEntityInfo} must fail.
     */
    private String expectedHost;
    
    /**
     * The expected entity port number provided by the {@link #testEntityInfo}. Will be <code>-1</code>, if the
     * construction of the {@link #testEntityInfo} must fail.
     */
    private int expectedPort;
    
    /**
     * The expected URL of the entity's monitoring scope provided by the {@link #testEntityInfo}. Will be
     * <code>null</code>, if the construction of the {@link #testEntityInfo} must fail.
     */
    private String expectedMonitoringUrl;
    
    /**
     * The expected port number of the entity's monitoring scope provided by the {@link #testEntityInfo}. Will be
     * <code>-1</code>, if the construction of the {@link #testEntityInfo} must fail.
     */
    private int expectedMonitoringPort;
    
    /**
     * The expected channel of the entity's monitoring scope provided by the {@link #testEntityInfo}. Will be
     * <code>null</code>, if the construction of the {@link #testEntityInfo} must fail.
     */
    private String expectedMonitoringChannel;

    /**
     * Constructs a new {@link EntityInfoTests} instance for loading a test IVML model based on the given
     * parameters. If loading the model is successful or the defined model was already loaded before, the
     * {@link #testEntityInfo} will be set with the defined project configuration in that model.
     * 
     * @param testIvmlModelFileName the name and extension of the IVML model file containing the IVML project definition
     *        to load
     * @param testIvmlProjectName the name of the IVML project to load from the IVML model file
     * @param expectedExceptionText the text of the exception expected to be thrown during construction of the
     *        {@link #testEntityInfo} or <code>null</code>, if construction must be successful
     * @param expectedIdentifier the expected entity identifier
     * @param expectedHost the expected entity host (URL)
     * @param expectedPort the expected entity port number
     * @param expectedMonitoringUrl the expected URL of the entity's monitoring scope
     * @param expectedMonitoringPort the expected port number of the entity's monitoring scope
     * @param expectedMonitoringChannel the expected channel of the entity's monitoring scope
     */
    //checkstyle: stop parameter number check
    public EntityInfoTests(String testIvmlModelFileName, String testIvmlProjectName,
            String expectedExceptionText, String expectedIdentifier, String expectedHost, int expectedPort,
            String expectedMonitoringUrl, int expectedMonitoringPort, String expectedMonitoringChannel) {
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        try {            
            testEntityInfo = new EntityInfo(easyUtilities.loadConfiguration(testIvmlModelFile));
            actualException = null;
        } catch (ModelException e) {
            testEntityInfo = null;
            actualException = e;
        } catch (EASyUtilitiesException e) {
            fail("Unexpected error while loading configuration from \"" + testIvmlModelFile + "\"", e);
        }
        this.expectedExceptionText = expectedExceptionText;
        this.expectedIdentifier = expectedIdentifier;
        this.expectedHost = expectedHost;
        this.expectedPort = expectedPort;
        this.expectedMonitoringUrl = expectedMonitoringUrl;
        this.expectedMonitoringPort = expectedMonitoringPort;
        this.expectedMonitoringChannel = expectedMonitoringChannel;
    }
    //checkstyle: resume parameter number check
    
    /**
     * Starts the necessary EASy-Producer components via {@link AbstractEASyBasedTests#setup()} before adding the
     * {@link AllTests#TEST_IVML_FILES_DIRECTORY} as model location to ensure the availability of all test IVML model
     * files of that directory.
     */
    @BeforeClass
    public static void setup() {
        // Start EASy components
        AbstractEASyBasedTests.setup();
        // Add IVML test files directory as common model location
        try {
            easyUtilities.addModelLocation(AllTests.TEST_IVML_FILES_DIRECTORY);
        } catch (EASyUtilitiesException e) {
            fail("Unexpected error while adding model location", e);
        }
    }
    
    /**
     * Removes the {@link AllTests#TEST_IVML_FILES_DIRECTORY} as model location from the EASy-Producer components before
     * stopping these components via {@link AbstractEASyBasedTests#teardown()}.
     */
    @AfterClass
    public static void teardown() {
        // Remove IVML test files directory as common model location
        try {
            easyUtilities.removeModelLocation(AllTests.TEST_IVML_FILES_DIRECTORY);
        } catch (EASyUtilitiesException e) {
            fail("Unexpected error while removing model location", e);
        }
        // Stop EASy components
        AbstractEASyBasedTests.teardown();
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
     * Tests whether the construction of {@link #testEntityInfo} throws the expected exception (text).
     */
    @Test
    public void testEqualInstanceConstructionException() {
        if (expectedExceptionText == null) {
            assertEquals(expectedExceptionText, actualException, "Wrong exception during instance construction");
        } else {
            assertEquals(expectedExceptionText, actualException.getMessage(), "Wrong exception text");
        }
    }
    
    /**
     * Tests whether the {@link #testEntityInfo} provides the {@link #expectedIdentifier}.
     */
    @Test
    public void testEqualObservableIdentifier() {
        if (expectedIdentifier == null) {
            assertEquals(expectedIdentifier, testEntityInfo, "Wrong entity info");
        } else {
            assertEquals(expectedIdentifier, testEntityInfo.getIdentifier(), "Wrong entity identifier");
        }
    }
    
    /**
     * Tests whether the {@link #testEntityInfo} provides the {@link #expectedHost}.
     */
    @Test
    public void testEqualObservableHost() {
        if (expectedHost == null) {
            assertEquals(expectedHost, testEntityInfo, "Wrong entity info");
        } else {
            assertEquals(expectedHost, testEntityInfo.getHost(), "Wrong entity host");
        }
    }
    
    /**
     * Tests whether the {@link #testEntityInfo} provides the {@link #expectedPort}.
     */
    @Test
    public void testEqualObservablePort() {
        if (expectedPort == -1) {
            assertEquals(null, testEntityInfo, "Wrong entity info");
        } else {
            assertEquals(expectedPort, testEntityInfo.getPort(), "Wrong entity port");
        }
    }
    
    /**
     * Tests whether the {@link #testEntityInfo} provides the {@link #expectedMonitoringUrl}.
     */
    @Test
    public void testEqualMonitoringUrl() {
        if (expectedMonitoringUrl == null) {
            assertEquals(expectedMonitoringUrl, testEntityInfo, "Wrong entity info");
        } else {
            assertEquals(expectedMonitoringUrl, testEntityInfo.getMonitoringUrl(), "Wrong monitoring URL");
        }
    }
    
    /**
     * Tests whether the {@link #testEntityInfo} provides the {@link #expectedMonitoringPort}.
     */
    @Test
    public void testEqualMonitoringPort() {
        if (expectedMonitoringPort == -1) {
            assertEquals(null, testEntityInfo, "Wrong entity info");
        } else {
            assertEquals(expectedMonitoringPort, testEntityInfo.getMonitoringPort(), "Wrong monitoring port");
        }
    }
    
    /**
     * Tests whether the {@link #testEntityInfo} provides the {@link #expectedMonitoringChannel}.
     */
    @Test
    public void testEqualMonitoringChannel() {
        if (expectedMonitoringChannel == null) {
            assertEquals(expectedMonitoringChannel, testEntityInfo, "Wrong entity info");
        } else {
            assertEquals(expectedMonitoringChannel, testEntityInfo.getMonitoringChannel(), "Wrong monitoring channel");
        }
    }
}
