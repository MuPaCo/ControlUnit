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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
            {null, "Missing configuration file path"},
            {"", "Missing configuration file path"},
            {"  ", "Missing configuration file path"},
            {"notafilepath", "Invalid configuration file path: \"notafilepath\""},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "empty.cfg", "Loaded properties are empty"},
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
            Setup setup = new Setup(testConfigurationFilePath);
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
     * Tests whether the {@link #expectedCreationResult} defined in the {@link #TEST_DATA} for a specific test iteration
     * is equal to the {@link #actualCreationResult} of creating the setup instance of that iteration.  
     */
    @Test
    public void testEqualCreationResults() {
        assertEquals(expectedCreationResult, actualCreationResult, "Wrong creation result");
    }
    
}
