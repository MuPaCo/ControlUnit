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
package net.ssehub.devopt.controllayer.utilities;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.devopt.controllayer.AllTests;

/**
 * This class contains unit tests for the {@link EASyUtilities}.
 * 
 * @author kroeher
 *
 */
public class EASyUtilitiesTests {
    
    /**
     * The constant string defining IVML file extension.
     */
    private static final String IVML_MODEL_FILE_EXTENSION = ".ivml";
    
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
     * Tests whether the addition of an empty string (no content at all) results in the expected error.
     */
    @Test
    public void testAddEmptyString() {
        try {
            easyUtilities.addModel("", "NoFileUsed");
            fail("Addition of empty string");
        } catch (EASyUtilitiesException e) {
            assertNotNull("Addition of empty string as model must cause an excpetion", e);
        }
    }
    
    /**
     * Tests whether the addition of a valid, empty project is successful.
     */
    @Test
    public void testAddModelValidEmptyProject() {
        String testIvmlProjectName = "EmptyProject";
        String testIvmlModelFileName = testIvmlProjectName + IVML_MODEL_FILE_EXTENSION;
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        try {
            String ivmlModelString = getIvmlModelString(testIvmlModelFile);
            String addedProjectName = easyUtilities.addModel(ivmlModelString, testIvmlProjectName);
            assertEquals(testIvmlProjectName, addedProjectName, "Addition of new model returned wrong project name");
            assertEquals(testIvmlProjectName, getLoadedProjectName(testIvmlProjectName), "Added new model not loaded");
        } catch (EASyUtilitiesException e) {
            fail("Addition of valid project must not cause an excpetion", e);
        }
    }
    
    /**
     * Tests whether the addition of an erroneous project (missing closing bracket for project definition) fails.
     */
    @Test
    public void testAddModelProjectMissingClosingBracket() {
        String testIvmlProjectName = "ProjectMissingClosingBracket";
        String testIvmlModelFileName = testIvmlProjectName + IVML_MODEL_FILE_EXTENSION;
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        try {
            String ivmlModelString = getIvmlModelString(testIvmlModelFile);
            String addedProjectName = easyUtilities.addModel(ivmlModelString, testIvmlProjectName);
            assertEquals(null, addedProjectName, "Addition of erroneous model definition");
            assertEquals(null, getLoadedProjectName(testIvmlProjectName), "Erroneous model loaded");
        } catch (EASyUtilitiesException e) {
            fail("Addition of invalid project must not cause an excpetion", e);
        }
    }
    
    /**
     * Tests whether the addition of a valid DevOpt model is successful.
     */
    @Test
    public void testAddModelValidDevOptProject() {
        String testIvmlProjectName = "DevOpt_E3_1_Modeling_Approach";
        String testIvmlModelFileName = testIvmlProjectName + IVML_MODEL_FILE_EXTENSION;
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        try {
            String ivmlModelString = getIvmlModelString(testIvmlModelFile);
            String addedProjectName = easyUtilities.addModel(ivmlModelString, testIvmlProjectName);
            assertEquals(testIvmlProjectName, addedProjectName, "Addition of erroneous model definition");
            assertEquals(testIvmlProjectName, getLoadedProjectName(testIvmlProjectName), "Erroneous model loaded");
        } catch (EASyUtilitiesException e) {
            fail("Addition of valid project must not cause an excpetion", e);
        }
    }

    /*
     * TODO implement further tests, like:
     *     addition, validation and removal of correct DevOpt Model
     *     addition of DevOpt Model not following meta-model
     */
    
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
     * Searches in the list of all IVML projects currently loaded by the {@link #easyUtilities} for a project with the
     * given name.
     * 
     * @param searchProjectName the name of the project to search for
     * @return the name of the project equal to the given project name or <code>null</code>, if no such project is
     *         loaded
     */
    private String getLoadedProjectName(String searchProjectName) {
        String foundProjectName = null;
        List<String> loadedProjectNames = easyUtilities.getProjectNames();
        int loadedProjectNamesCounter = 0;
        String loadedProjectName;
        while (foundProjectName == null && loadedProjectNamesCounter < loadedProjectNames.size()) {
            loadedProjectName = loadedProjectNames.get(loadedProjectNamesCounter); 
            if (loadedProjectName.equals(searchProjectName)) {
                foundProjectName = loadedProjectName;
            }
            loadedProjectNamesCounter++;
        }
        return foundProjectName;
    }
    
}
