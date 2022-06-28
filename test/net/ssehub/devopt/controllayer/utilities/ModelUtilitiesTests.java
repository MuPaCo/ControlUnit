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
package net.ssehub.devopt.controllayer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;

/**
 * This class contains unit tests for the {@link ModelUtilities}.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class ModelUtilitiesTests {

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the expected model
     * information provided by the model utilities for the respective test iteration as well as the input for loading
     * the respective test model. For each subset, the values represent:
     * <ul>
     * <li>The name and extension of the IVML model file containing the IVML project definition to load</li>
     * <li>The name of the IVML project to load from the IVML model file</li>
     * <li>The identifier of the entity defined by the IVML project</li>
     * <li>The host (IP) of the entity defined by the IVML project</li>
     * <li>The port number of the entity defined by the IVML project</li>
     * <li>The monitoring scope of the entity defined by the IVML project</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {"EmptyProject.ivml", "EmptyProject", false, null, null, -1, null},
            {"MinimalDevOptProject.ivml", "MinimalDevOptProject", true, null, null, -1, null},
            {"EntityEmptyDefinitionDevOptProject.ivml", "EntityEmptyDefinitionDevOptProject", true, null, null, -1,
                null},
            {"EntityEmptyIdentificationDescriptionDevOptProject.ivml",
                "EntityEmptyIdentificationDescriptionDevOptProject", true, null, null, -1, null},
            {"EntityEmptyIdentificationDevOptProject.ivml", "EntityEmptyIdentificationDevOptProject", true, null, null,
                -1, null},
            {"EntityIdentificationDevOptProject.ivml", "EntityIdentificationDevOptProject", true,
                "System under Monitoring", "192.168.1.11", 1883, null},
            {"MonitoringDevOptProject.ivml", "MonitoringDevOptProject", true, "System under Monitoring", "192.168.1.11",
                1883, "entity/monitoring/mqtt/topic@127.0.0.1:8883"},
    };
    
    /**
     * The local reference to the global {@link EASyUtilities}.
     */
    private static EASyUtilities easyUtilities = EASyUtilities.INSTANCE;
    
    /**
     * The file denoting an initially empty directory as temporal model directory to start the EASy-Producer components
     * without immediately loading models at start-up. The individual tests in this class may add models to that
     * directory via the {@link #easyUtilities}-
     */
    private static File tempModelDirectory;
    
    /**
     * The name of the test IVML model file loaded during the previous test iteration. The initial value is
     * <code>null</code>.
     */
    private static String previousTestIvmlModelFileName;
    
    /**
     * The name of the test IVML project loaded during the previous test iteration. The initial value is
     * <code>null</code>.
     */
    private static String previousTestIvmlProjectName;
    
    /**
     * The local reference to the global {@link ModelUtilities}.
     */
    private ModelUtilities modelUtilities = ModelUtilities.INSTANCE;
    
    /**
     * The {@link Configuration} of the test IVML project loaded for the current test iteration.
     */
    private Configuration testIvmlProjectConfiguration;
    
    /**
     * The definition of whether the {@link #modelUtilities} must retrieve an entity from the 
     * {@link #testIvmlProjectConfiguration} (<code>true</code>) or not (<code>false</code>).
     */
    private boolean expectedEntityAvailable;
    
    /**
     * The expected entity identifier.
     */
    private String expectedEntityIdentifier;
    
    /**
     * The expected entity host.
     */
    private String expectedEntityHost;
    
    /**
     * The expected entity port number.
     */
    private int expectedEntityPort;
    
    /**
     * The expected entity monitoring scope.
     */
    private String expectedEntityMonitoringScope;
    
    /**
     * Constructs a new {@link ModelUtilitiesTests} instance for loading a test IVML model based on the given
     * parameters. If loading the model is successful or the defined model was already loaded before, the
     * {@link #testIvmlProjectConfiguration} will be set with the defined project configuration in that model.
     * 
     * @param testIvmlModelFileName the name and extension of the IVML model file containing the IVML project definition
     *        to load
     * @param testIvmlProjectName the name of the IVML project to load from the IVML model file
     * @param expectedEntityAvailable the definition of whether the {@link #modelUtilities} must retrieve an entity 
     *        from the {@link #testIvmlProjectConfiguration} (<code>true</code>) or not (<code>false</code>)
     * @param expectedEntityIdentifier the expected entity identifier
     * @param expectedEntityHost the expected entity host
     * @param expectedEntityPort the expected entity port number
     * @param expectedEntityMonitoringScope the expected entity monitoring scope
     */
    //checkstyle: stop parameter number check
    public ModelUtilitiesTests(String testIvmlModelFileName, String testIvmlProjectName,
            boolean expectedEntityAvailable, String expectedEntityIdentifier, String expectedEntityHost,
            int expectedEntityPort, String expectedEntityMonitoringScope) {
        // Add model only, if it was not added before (multiple additions due to parameterization causes exceptions)
        if (testIvmlModelFileName != null && testIvmlProjectName != null
                && !testIvmlModelFileName.equals(previousTestIvmlModelFileName)
                && !testIvmlProjectName.equals(previousTestIvmlProjectName)) {
            File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
            String testIvmlModelString = getIvmlModelString(testIvmlModelFile);
            try {
                testIvmlProjectName = easyUtilities.addModel(testIvmlModelString, testIvmlProjectName);
                previousTestIvmlModelFileName = testIvmlModelFileName;
                previousTestIvmlProjectName = testIvmlProjectName;
            } catch (EASyUtilitiesException e) {
                fail("Retrieving configuration for IVML model file \"" + testIvmlModelFileName + "\"failed", e);
            }
        }
        testIvmlProjectConfiguration = easyUtilities.getConfiguration(previousTestIvmlProjectName);
        this.expectedEntityAvailable = expectedEntityAvailable;
        this.expectedEntityIdentifier = expectedEntityIdentifier;
        this.expectedEntityHost = expectedEntityHost;
        this.expectedEntityPort = expectedEntityPort;
        this.expectedEntityMonitoringScope = expectedEntityMonitoringScope;
    }
    //checkstyle: resume parameter number check
    
    /**
     * Reads the content of the given file and returns it as a single string using {@link System#lineSeparator()} to
     * concatenate individual file lines.
     * 
     * @param ivmlModelFile the file to read to content from
     * @return the content of the given file or <code>null</code>, if reading the content fails
     */
    private String getIvmlModelString(File ivmlModelFile) {
        String ivmlModelString = null;
        List<String> testIvmlFileLines;
        try {
            testIvmlFileLines = FileUtilities.INSTANCE.readFile(ivmlModelFile);
            StringBuilder ivmlModelStringBuilder = new StringBuilder();
            for (String testIvmlFileLine : testIvmlFileLines) {
                ivmlModelStringBuilder.append(testIvmlFileLine);
                ivmlModelStringBuilder.append(System.lineSeparator());
            }
            ivmlModelString = ivmlModelStringBuilder.toString();
        } catch (FileUtilitiesException e) {
            e.printStackTrace();
        }
        return ivmlModelString;
    }
    
    /**
     * Creates the {@link #tempModelDirectory} and starts the EASy-Producer components of the {@link #easyUtilities}
     * using that directory as model directory.
     */
    @BeforeClass
    public static void setup() {
        // Create temporal, empty model directory to start the EASy components without immediately loading models 
        tempModelDirectory = new File(AllTests.TEST_IVML_FILES_DIRECTORY, "test_" + System.currentTimeMillis());
        try {
            tempModelDirectory = FileUtilities.INSTANCE.createDirectory(tempModelDirectory.getAbsolutePath());
        } catch (FileUtilitiesException e) {
            fail(e);
        }
        // Start the EASy components using the canonical path to the model directory created above
        try {
            easyUtilities.startEASyComponents(tempModelDirectory.getCanonicalPath());
        } catch (IOException | EASyUtilitiesException e) {
            fail(e);
        }
    }
    
    /**
     * Stops the EASy-Producer components of the {@link #easyUtilities} and deletes the {@link #tempModelDirectory} 
     * including its content created during the tests in this class.
     */
    @AfterClass
    public static void teardown() {
        // Stop the EASy components
        try {
            easyUtilities.stopEASyComponents();
        } catch (EASyUtilitiesException e) {
            fail(e);
        }
        // Delete the temporal model directory and all its content used for the tests in this class only
        try {
            FileUtilities.INSTANCE.delete(tempModelDirectory);
        } catch (FileUtilitiesException e) {
            fail(e);
        }
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
     * Tests whether the {@link #expectedEntityAvailable} is equal to the availability of an entity as provided by the
     * {@link #modelUtilities}. This test only checks for the existence of an object, but not for the correct identity
     * of the entity. These kinds of checks are subject to the other tests in this class.
     */
    @Test
    public void testEntityAvailable() {
        boolean actualEntityAvailable = false;
        IDecisionVariable entity = modelUtilities.getEntity(testIvmlProjectConfiguration);
        if (entity != null) {
            actualEntityAvailable = true;
        }
        assertEquals(expectedEntityAvailable, actualEntityAvailable, "Wrong entity availability");
    }
    
    /**
     * Tests whether the {@link #expectedEntityIdentifier} is equal to the one provided by the {@link #modelUtilities}.
     */
    @Test
    public void testEqualEntityIdentifier() {
        IDecisionVariable entity = modelUtilities.getEntity(testIvmlProjectConfiguration);
        assertEquals(expectedEntityIdentifier, modelUtilities.getEntityIdentificationIdentifier(entity),
                "Wrong entity identifier");
    }
    
    /**
     * Tests whether the {@link #expectedEntityHost} is equal to the one provided by the {@link #modelUtilities}.
     */
    @Test
    public void testEqualEntityHost() {
        IDecisionVariable entity = modelUtilities.getEntity(testIvmlProjectConfiguration);
        assertEquals(expectedEntityHost, modelUtilities.getEntityIdentificationHost(entity), "Wrong entity host");
    }
    
    /**
     * Tests whether the {@link #expectedEntityPort} is equal to the one provided by the {@link #modelUtilities}.
     */
    @Test
    public void testEqualEntityPort() {
        IDecisionVariable entity = modelUtilities.getEntity(testIvmlProjectConfiguration);
        assertEquals(expectedEntityPort, modelUtilities.getEntityIdentificationPort(entity), "Wrong entity port");
    }
    
    /**
     * Tests whether the {@link #expectedEntityMonitoringScope} is equal to the one provided by the 
     * {@link #modelUtilities}.
     */
    @Test
    public void testEqualEntityMonitoringScope() {
        IDecisionVariable entity = modelUtilities.getEntity(testIvmlProjectConfiguration);
        assertEquals(expectedEntityMonitoringScope, modelUtilities.getEntityRuntimeDateMonitoringScope(entity),
                "Wrong entity monitoring scope");
    }
    
}
