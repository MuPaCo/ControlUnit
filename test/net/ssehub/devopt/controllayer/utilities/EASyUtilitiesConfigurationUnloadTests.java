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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.AbstractEASyBasedTests;
import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.easy.varModel.confModel.Configuration;

/**
 * This class contains unit tests for the {@link EASyUtilities#unloadConfiguration(Configuration)} method. As a
 * prerequisite, these tests rely on proper execution of {@link EASyUtilities#addModelLocation(File)} during
 * {@link #setup()} and {@link EASyUtilities#removeModelLocation(File)} during {@link #teardown()}. Further, each test
 * calls {@link EASyUtilities#loadConfiguration(File)} before actually testing the unloading.
 *  
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class EASyUtilitiesConfigurationUnloadTests extends AbstractEASyBasedTests {
    
    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The file denoting the IVML model file, which is used to load the test configuration for unloading</li>
     * <li>The expected return value, if {@link EASyUtilities#unloadConfiguration(Configuration)} is called with the
     *     configuration loaded from the test IVML model file</li>
     * <li>The expected message of the exception thrown during {@link EASyUtilities#unloadConfiguration(Configuration)},
     *     if this method is called with the configuration loaded from the test IVML model file; must be
     *     <code>null</code>, if unloading is expected to be successful</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, false, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EmptyProject.ivml"), true, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityEmptyDefinitionDevOptProject.ivml"), true, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityEmptyIdentificationDescriptionDevOptProject.ivml"),
                true, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityEmptyIdentificationDevOptProject.ivml"), true, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityIdentificationDevOptProject.ivml"), true, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "IdentificationDevOptProject.ivml"), true, null},
            // Invalid configurations should be loaded successfully; testing their validity is a different issue
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "InvalidDevOptProject.ivml"), true, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "MinimalDevOptProject.ivml"), true, null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "MonitoringDevOptProject.ivml"), true, null}
    };
    
    /**
     * The file denoting the IVML model file, which is used to load the test configuration for unloading.
     */
    private File testIvmlFile;
    
    /**
     * The expected return value, if {@link EASyUtilities#unloadConfiguration(Configuration)} is called with the
     * configuration loaded from {@link #testIvmlFile}.
     */
    private boolean expectedUnloadingConfiguration;
    
    /**
     * The expected message of the exception thrown during {@link EASyUtilities#unloadConfiguration(Configuration)},
     * if this method is called with the configuration loaded from {@link #testIvmlFile}; will be <code>null</code>, if
     * unloading is expected to be successful.
     */
    private String expectedUnloadingExceptionMessage;
    
    /**
     * Constructs a new {@link EASyUtilitiesConfigurationUnloadTests} instance for testing one particular subset of the
     * {@link #TEST_DATA}.
     * 
     * @param testIvmlFile the file denoting the IVML model file, which is used to load the test configuration for
     *        unloading
     * @param expectedUnloadingConfiguration the expected return value, if
     *        {@link EASyUtilities#unloadConfiguration(Configuration)} is called with the configuration loaded from
     *        {@link #testIvmlFile}
     * @param expectedUnloadingExceptionMessage the expected message of the exception thrown during
     *        {@link EASyUtilities#unloadConfiguration(Configuration)}, if this method is called with the configuration
     *        loaded from {@link #testIvmlFile}; will be <code>null</code>, if unloading is expected to be successful
     */
    public EASyUtilitiesConfigurationUnloadTests(File testIvmlFile, boolean expectedUnloadingConfiguration,
            String expectedUnloadingExceptionMessage) {
        this.testIvmlFile = testIvmlFile;
        this.expectedUnloadingConfiguration = expectedUnloadingConfiguration;
        this.expectedUnloadingExceptionMessage = expectedUnloadingExceptionMessage;
    }
    
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
     * Tests whether {@link EASyUtilities#unloadConfiguration(Configuration)} returns the
     * {@link #expectedUnloadingConfiguration} value and the {@link #expectedUnloadingExceptionMessage}.
     */
    @Test
    public void testUnloadConfiguration() {
        Configuration loadedConfiguration = null;
        EASyUtilitiesException unloadingExcpetion = null;
        /*
         * Unloading tests require configurations (or null) as input for method under test. Hence, loading configuration
         * prior to these tests must be successful (no invalid file objects, etc. must be used here). Testing loading
         * with such invalid input parameters is subject to explicit loading tests.
         */
        if (testIvmlFile != null) {
            try {
                loadedConfiguration = easyUtilities.loadConfiguration(testIvmlFile);
            } catch (EASyUtilitiesException e) {
                fail("Unexpected error while loading configuration from \"" + testIvmlFile + "\"", e);
            }
        }
        // Actual test of expected unloading result
        if (expectedUnloadingExceptionMessage != null) {     
            try {
                easyUtilities.unloadConfiguration(loadedConfiguration);
                fail("Unloading should fail exceptionally (processing error without actual unloading result)");
            } catch (EASyUtilitiesException e) {
                unloadingExcpetion = e;
            }
            assertEquals(expectedUnloadingExceptionMessage, unloadingExcpetion.getMessage(),
                    "Incorrect exception while unloading configuration from model file \"" + testIvmlFile + "\"");
        } else {
            try {
                assertEquals(expectedUnloadingConfiguration, easyUtilities.unloadConfiguration(loadedConfiguration),
                        "Incorrect unloading of configuration from model file \"" + testIvmlFile + "\"");
            } catch (EASyUtilitiesException e) {
                unloadingExcpetion = e;
            }
            assertEquals(expectedUnloadingExceptionMessage, unloadingExcpetion,
                    "Incorrect exception while unloading configuration from model file \"" + testIvmlFile + "\"");
        }
    }
    
}
