/*
 * Copyright 2021 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.devopt.controllayer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;

/**
 * This class contains unit tests for the creation of new {@link Setup} instances. In particular, these tests focus on
 * the correct exceptions to be thrown, if input parameter or the resources they point to are not as required by that
 * class. These tests do not include checks regarding the provision of correct configuration properties based on a
 * correct input. Such tests are subject to the {@link SetupUsageTests}.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class SetupCreationTests {
    
    private static final String TEST_CONFIGURATION_FILES_DIRECTORY_PATH =
            AllTests.TEST_CONFIGURATION_FILES_DIRECTORY.getAbsolutePath() + File.separator;
    
    private static final String DEFAULT_SETUP_LOGGING_STRING = "logging.debug = n;logging.standard = s;";
    
    private static final String DEFAULT_SETUP_REGISTRATION_STRING = "registration.protocol = HTTP;"
            + "registration.port = 80;registration.url = 127.0.0.1;registration.channel = /registration;";
    
    private static final String DEFAULT_SETUP_MODEL_STRING = "model.directory = ./models;";
    
    private static final String DEFAULT_SETUP_NO_MODEL_STRING = DEFAULT_SETUP_LOGGING_STRING
            + DEFAULT_SETUP_REGISTRATION_STRING;
    
    private static final String DEFAULT_SETUP_STRING = DEFAULT_SETUP_LOGGING_STRING
            + DEFAULT_SETUP_REGISTRATION_STRING
            + DEFAULT_SETUP_MODEL_STRING;

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). For each subset, the values represent:
     * <ul>
     * <li>The the path to the configuration file for creating a new setup instance</li>
     * <li>The expected creation result in terms of:</li>
     *     <ul>
     *     <li>The setup instance string as provided by {@link Setup#toString()}, if creating the instance with the
     *         associated parameter above must be successful</li>
     *     <li>The detail message string of the top-level exception thrown during setup instance creation, if creating
     *         the setup instance must fail</li>
     *     </ul>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, DEFAULT_SETUP_STRING},
            {"", DEFAULT_SETUP_STRING},
            {"  ", DEFAULT_SETUP_STRING},
            {"notafilepath", "Invalid configuration file path: \"notafilepath\""},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "empty.cfg", "Loaded properties are empty"},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "modelProperties_noDirectory.cfg", DEFAULT_SETUP_STRING},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "modelProperties_invalidDirectory.cfg", DEFAULT_SETUP_STRING},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "modelProperties_valid.cfg", DEFAULT_SETUP_NO_MODEL_STRING 
                + "model.directory = ./testdata/model_db;"}
            /*
             * TODO: incomplete configuration properties, wrong combinations, values the fail validation, more than a
             *       single args element
             *       
             * {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "minimal.cfg", "Loaded properties are empty"}
             */
    };
    
    /**
     * The expected creation result. This string contains:
     * <ul>
     * <li>The setup instance string as provided by {@link Setup#toString()}, if creating the instance must be
     *     successful</li>
     * <li>The detail message string of the top-level exception thrown during setup instance creation, if creating
     *     the setup instance must fail</li>
     * </ul>
     */
    private String expectedCreationResult;
    
    /**
     * The actual creation result. This string contains:
     * <ul>
     * <li>The setup instance string provided by {@link Setup#toString()} after successful creation of the setup
     *     instance</li>
     * <li>The detail message string of the top-level exception thrown during setup instance creation, if creating
     *     the setup instance failed</li>
     * </ul>
     */
    private String actualCreationResult;
    
    /**
     * The {@link Setup} instance used in the current test iteration.
     */
    private Setup setup;
    
    /**
     * Constructs a new {@link SetupCreationTests} instance for creating a setup instance based on the given parameter.
     * If creating the instance is successful, the information provided by {@link Setup#toString()} for this instance is
     * set as the {@link #actualCreationResult}. If creating the instance fails, the detail message string of the thrown
     * {@link SetupException} is set as the {@link #actualCreationResult}.
     * 
     * @param testConfigurationFilePath the path to the configuration file for creating a new setup instance 
     * @param expectedCreationResult either the setup information as provided by {@link Setup#toString()}, if creating
     *        the instance must be successful, or the detail exception message, if creating the instance must fail
     */
    public SetupCreationTests(String testConfigurationFilePath, String expectedCreationResult) {
        this.expectedCreationResult = expectedCreationResult;
        try {
            setup = new Setup(testConfigurationFilePath);
            actualCreationResult = setup.toString();
        } catch (SetupException e) {
            actualCreationResult = e.getMessage();
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
     * Deletes all artifacts (files, directories, etc.) created by the {@link Setup} instance used to execute the test
     * cases in this class. This deletion is performed after each test method to ensure proper creation during the next
     * test iteration.
     */
    @After
    public void deleteSetupArtifacts() {
        if (setup != null) {            
            String modelDirectoryPath = setup.getModelConfiguration(Setup.KEY_MODEL_DIRECTORY);
            if (modelDirectoryPath != null) {
                File modelDirectory = new File(modelDirectoryPath);
                try {
                    System.out.println("Deleting test model directory \"" + modelDirectory.getAbsolutePath() + "\"");
                    FileUtilities.INSTANCE.delete(modelDirectory);
                } catch (FileUtilitiesException e) {
                    System.out.println("Deleting test model directory failed; see trace below");
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Tests whether the {@link #expectedCreationResult} defined in the {@link #TEST_DATA} for a specific test iteration
     * is equal to the {@link #actualCreationResult} of creating the setup instance of that iteration.  
     */
    @Test
    public void testEqualCreationResults() {
        assertEquals(expectedCreationResult, actualCreationResult, "Wrong creation result");
    }
    
}
