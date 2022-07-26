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

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.SetupException;
import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;

/**
 * This class contains units tests for the {@link Aggregator#setUp(Setup)} method.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class AggregatorSetupTests {
    
    /**
     * The constant string denoting the path to the directory containing test configuration files used to create
     * {@link Setup} instances.
     */
    private static final String TEST_CONFIGURATION_FILES_DIRECTORY_PATH =
            AllTests.TEST_CONFIGURATION_FILES_DIRECTORY.getAbsolutePath() + File.separator;

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The string denoting the path to the test configuration file to use for creating the {@link Setup} instance as
     *     an input to {@link Aggregator#setUp(Setup)}</li>
     * <li>The expected exception message, if the aggregator setup fails; <code>null</code>, if setup must be
     *     successful</li>
     * <li>The definition of whether failing of tearing down the current aggregator instance after the test is expected
     *     (<code>true</code>) or not (<code>false</code>); e.g., if the setup fails, tearing down must fail as well as
     *     no aggregator instance was created</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, "Setup is \"null\"", true},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "aggregationProperties_incomplete.cfg", null, false},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "aggregationProperties_valid.cfg", null, false},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "description.cfg", null, false},
            {TEST_CONFIGURATION_FILES_DIRECTORY_PATH + "minimal.cfg", null, false},
            // Remaining test configuration files do not provide different parameter combinations than the previous ones
    };
    
    /**
     * The expected exception message thrown during setup of the aggregator. May be <code>null</code>, if no exception
     * must be thrown.
     */
    private String expectedSetupExceptionMessage;
    
    /**
     * The actual exception message thrown during setup of the aggregator. May be <code>null</code>, if no exception
     * was be thrown.
     */
    private String actualSetupExceptionMessage;
    
    /**
     * The definition of whether failing of tearing down the current aggregator instance after the test is expected
     * (<code>true</code>) or not (<code>false</code>); e.g., if the setup fails, tearing down must fail as well as
     * no aggregator instance was created.
     */
    private boolean expectedTearDownFail;
    
    /**
     * Constructs a new {@link AggregatorSetupTests} instance for testing one particular subset of the
     * {@link #TEST_DATA}.
     * 
     * @param testConfigurationFilePath the string denoting the path to the test configuration file to use for creating
     *        the {@link Setup} instance as an input to {@link Aggregator#setUp(Setup)}
     * @param expectedSetupExceptionMessage the expected exception message thrown during setup of the aggregator; can be
     *        <code>null</code>, if no exception must be thrown
     * @param expectedTearDownFail the definition of whether failing of tearing down the current aggregator instance
     *        after the test is expected (<code>true</code>) or not (<code>false</code>);
     */
    public AggregatorSetupTests(String testConfigurationFilePath, String expectedSetupExceptionMessage,
            boolean expectedTearDownFail) {
        this.expectedTearDownFail = expectedTearDownFail;
        this.expectedSetupExceptionMessage = expectedSetupExceptionMessage;
        Setup testSetup = null;
        if (testConfigurationFilePath != null) {            
            try {
                testSetup = new Setup(testConfigurationFilePath);
            } catch (SetupException e) {
                fail("Test setup creation failed unexpectedly", e);
            }
        }
        try {
            Aggregator.setUp(testSetup);
            actualSetupExceptionMessage = null;
        } catch (MonitoringException e) {
            actualSetupExceptionMessage = e.getMessage();
        } finally {
            // Delete the model directory again, which is created automatically by a setup instance
            if (testSetup != null) {
                try {
                    FileUtilities.INSTANCE.delete(new File(testSetup.getModelConfiguration(Setup.KEY_MODEL_DIRECTORY)));
                } catch (FileUtilitiesException e) {
                    fail("Deleting model directory as defined in test setup failed unexpectedly", e);
                }
            }
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
     * Initiates the deletion of the singleton instance of the {@link Aggregator} class and the release of associated
     * resources.
     */
    @After
    public void tearDownAggregator() {
        try {
            Aggregator.tearDown();
        } catch (MonitoringException e) {
            if (!expectedTearDownFail) {                
                fail("Tearing down aggregator failed unexpectedly", e);
            }
        }
    }
    
    /**
     * Tests whether the {@link #actualSetupExceptionMessage} is equal to the {@link #expectedSetupExceptionMessage}.
     */
    @Test
    public void testAggregatorSetup() {
        assertEquals(expectedSetupExceptionMessage, actualSetupExceptionMessage, "Wrong setup exception message");
    }
    
}
