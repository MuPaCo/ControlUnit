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
 * This class contains unit tests for the {@link EASyUtilities#loadConfiguration(File)} method. As a prerequisite, these
 * tests rely on proper execution of {@link EASyUtilities#addModelLocation(File)} during {@link #setup()} and
 * {@link EASyUtilities#removeModelLocation(File)} during {@link #teardown()}.
 *  
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class EASyUtilitiesConfigurationLoadTests extends AbstractEASyBasedTests {
    
    /**
     * The constant string denoting the absolute path to the parent project of this class.
     */
    private static final String PROJECT_DIRECTORY_PATH = AllTests.PROJECT_DIRECTORY.getAbsolutePath();
    
    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The file denoting the test IVML model file</li>
     * <li>The expected name of the loaded configuration, if {@link EASyUtilities#loadConfiguration(File)} is called
     *     with the test IVML model file; must be <code>null</code>, if loading is expected to fail</li>
     * <li>The expected message of the exception thrown during {@link EASyUtilities#loadConfiguration(File)}, if
     *     this method is called with the test IVML model file; must be <code>null</code>, if loading is expected to be
     *     successful</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, null, "Source IVML file is \"null\""},
            {new File("thisdoesnotexist"), null,
                "Source IVML file \"" + PROJECT_DIRECTORY_PATH + "\\thisdoesnotexist\" is not a file"},
            {AllTests.PROJECT_DIRECTORY, null, "Source IVML file \"" + PROJECT_DIRECTORY_PATH + "\" is not a file"},
            {new File(AllTests.TEST_CONFIGURATION_FILES_DIRECTORY, "empty.cfg"), null,
                "Loading configuration from file \"" + PROJECT_DIRECTORY_PATH
                    + "\\testdata\\setup\\empty.cfg\" failed"},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EmptyProject.ivml"), "EmptyProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityEmptyDefinitionDevOptProject.ivml"),
                "EntityEmptyDefinitionDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityEmptyIdentificationDescriptionDevOptProject.ivml"),
                "EntityEmptyIdentificationDescriptionDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityEmptyIdentificationDevOptProject.ivml"),
                "EntityEmptyIdentificationDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "EntityIdentificationDevOptProject.ivml"),
                "EntityIdentificationDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "IdentificationDevOptProject.ivml"),
                "IdentificationDevOptProject", null},
            // Invalid configurations should be loaded successfully; testing their validity is a different issue
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "InvalidDevOptProject.ivml"), "DevOpt_E3_1_Modeling_Approach",
                null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "InvalidIdentifierDevOptProject.ivml"),
                "MonitoringDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "MinimalDevOptProject.ivml"), "MinimalDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "MonitoringHttpDevOptProject.ivml"),
                "MonitoringDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "MonitoringMqttDevOptProject.ivml"),
                "MonitoringHiveBrokerDevOptProject", null},
            {new File(AllTests.TEST_IVML_FILES_DIRECTORY, "ProjectMissingClosingBracket.ivml"), null,
                "Loading configuration from file \"" + PROJECT_DIRECTORY_PATH
                    + "\\testdata\\ivml\\ProjectMissingClosingBracket.ivml\" failed"}
    };
    
    /**
     * The file denoting the IVML model file to use for loading.
     */
    private File testIvmlFile;
    
    /**
     * The expected name of the loaded configuration, if {@link EASyUtilities#loadConfiguration(File)} is called with
     * the {@link #testIvmlFile}; will be <code>null</code>, if loading is expected to fail.
     */
    private String expectedLoadedConfigurationName;
    
    /**
     * The expected message of the exception thrown during {@link EASyUtilities#loadConfiguration(File)}, if this
     * method is called with {@link #testIvmlFile}; will be <code>null</code>, if loading is expected to be successful.
     */
    private String expectedLoadingExceptionMessage;
    
    /**
     * Constructs a new {@link EASyUtilitiesConfigurationLoadTests} instance for testing one particular subset of the
     * {@link #TEST_DATA}.
     * 
     * @param testIvmlFile the file denoting the IVML model file to use for loading
     * @param expectedLoadedConfigurationName the expected name of the loaded configuration, if
     *        {@link EASyUtilities#loadConfiguration(File)} is called with the {@link #testIvmlFile}; will be
     *        <code>null</code>, if loading is expected to fail
     * @param expectedLoadingExceptionMessage the expected message of the exception thrown during
     *        {@link EASyUtilities#loadConfiguration(File)}, if this method is called with {@link #testIvmlFile}; will
     *        be <code>null</code>, if loading is expected to be successful
     */
    public EASyUtilitiesConfigurationLoadTests(File testIvmlFile, String expectedLoadedConfigurationName,
            String expectedLoadingExceptionMessage) {
        this.testIvmlFile = testIvmlFile;
        this.expectedLoadedConfigurationName = expectedLoadedConfigurationName;
        this.expectedLoadingExceptionMessage = expectedLoadingExceptionMessage;
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
     * Tests whether {@link EASyUtilities#loadConfiguration(File)} returns the {@link #expectedLoadedConfigurationName}
     * and the {@link #expectedLoadingExceptionMessage}.
     */
    @Test
    public void testLoadConfiguration() {
        Configuration loadedConfiguration = null;
        EASyUtilitiesException loadingExcpetion = null;
        try {
            loadedConfiguration = easyUtilities.loadConfiguration(testIvmlFile);
        } catch (EASyUtilitiesException e) {
            loadingExcpetion = e;
        }
        
        if (expectedLoadedConfigurationName == null) {
            // Loading should fail: no configuration, but exception message available
            assertEquals(expectedLoadedConfigurationName, loadedConfiguration,
                    "Incorrect loading of model file \"" + testIvmlFile + "\"");
            assertEquals(expectedLoadingExceptionMessage, loadingExcpetion.getMessage(),
                    "Incorrect excpetion while loading of model file \"" + testIvmlFile + "\"");
        } else {
            // Loading should be successful: configuration available, but no exception (message)
            assertEquals(expectedLoadedConfigurationName, loadedConfiguration.getName(),
                    "Incorrect loading of model file \"" + testIvmlFile + "\"");
            assertEquals(expectedLoadingExceptionMessage, loadingExcpetion,
                    "Incorrect excpetion while loading of model file \"" + testIvmlFile + "\"");
        }
    }

}
