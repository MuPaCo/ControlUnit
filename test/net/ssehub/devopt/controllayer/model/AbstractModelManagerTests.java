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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import net.ssehub.devopt.controllayer.AbstractEASyBasedTests;
import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.SetupException;
import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;

/**
 * This abstract class realizes preparation, utility, and post-processing methods, which are necessary for tests
 * involving the {@link ModelManager} instance. Hence, each specific test class requiring that instance should extend
 * this abstract class.
 * 
 * @author kroeher
 *
 */
public class AbstractModelManagerTests extends AbstractEASyBasedTests {
    
    /**
     * The local reference to the global {@link ModelManager} instance. This instance is created exactly once, if the
     * {@link AbstractModelManagerTests} constructor is called by one of the extending classes for the first time. It is
     * based on the {@link #testSetup}.
     */
    protected static ModelManager testModelManagerInstance;

    /**
     * The constant path to the directory containing test configuration files. This path ends with a
     * {@link File#separator}. 
     */
    private static final String TEST_CONFIGURATION_FILES_DIRECTORY_PATH =
            AllTests.TEST_CONFIGURATION_FILES_DIRECTORY.getAbsolutePath() + File.separator;
    
    /**
     * The constant path to the test configuration file used to create the {@link #testSetup}.
     */
    private static final String MODEL_MANAGER_TEST_CONFIGURATION_PATH = TEST_CONFIGURATION_FILES_DIRECTORY_PATH
            + "modelProperties_valid.cfg";
    
    /**
     * The {@link Setup} instance used to create the global {@link ModelManager} instance.
     */
    private static Setup testSetup;
    
    /**
     * Constructs a new {@link AbstractModelManagerTests} instance. In particular, if this constructor is called the
     * first time, it creates the {@link #testModelManagerInstance} using the {@link #testSetup} constructed with the
     * {@link #MODEL_MANAGER_TEST_CONFIGURATION_PATH}. Calling this constructor multiple times has no further effect.
     */
    protected AbstractModelManagerTests() {
        if (testModelManagerInstance == null) {            
            try {
                testSetup = new Setup(MODEL_MANAGER_TEST_CONFIGURATION_PATH);
                ModelManager.setUp(testSetup);
                testModelManagerInstance = ModelManager.getInstance();
            } catch (SetupException | ModelException e) {
                fail("Unexpected error while creating test model manager instance", e);
            }
        }
    }
    
    /**
     * Deletes the model directory and all its content defined in the {@link #testSetup}. This method is called exactly
     * once after all test classes of the test suite are executed.
     * 
     * @see AllTests#tearDown()
     */
    public static void deleteSetupArtifacts() {
        if (testSetup != null) {            
            String modelDirectoryPath = testSetup.getModelConfiguration(Setup.KEY_MODEL_DIRECTORY);
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

}
