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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

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
     * The constant prefix of each IVML model file created during the tests in this class. This prefix is used to delete
     * these files again during {@link #teardown()}.
     */
    private static final String NEW_IVML_FILES_PREFIX = "temp_";
    
    /**
     * The canonical path to the {@link AllTests#TEST_IVML_FILES_DIRECTORY}.
     */
    private static String testIvmlFilesDirectoryPath;
    
    /**
     * The local reference to the global {@link EASyUtilities}.
     */
    private static EASyUtilities easyUtilities = EASyUtilities.INSTANCE;
    
    /**
     * All files in the directory denoted by the {@link #testIvmlFilesDirectoryPath}, which have a name ending with the
     * <code>.ivml</code>-extension.
     */
//    private static File[] testIvmlFiles;
    
    /**
     * Sets the {@link #testIvmlFilesDirectoryPath} and the {@link #testIvmlFiles}. Further, this method starts the
     * EASy-Producer components of the {@link #easyUtilities}.
     */
    @BeforeClass
    public static void setup() {
        try {
            testIvmlFilesDirectoryPath = AllTests.TEST_IVML_FILES_DIRECTORY.getCanonicalPath();
        } catch (IOException e) {
            fail(e);
        }
        try {
            easyUtilities.startEASyComponents(testIvmlFilesDirectoryPath);
        } catch (EASyUtilitiesException e) {
            fail(e);
        }
        
//        testIvmlFiles = AllTests.TEST_IVML_FILES_DIRECTORY.listFiles(new FilenameFilter() {
//            
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.endsWith(".ivml");
//            }
//        });
    }
    
    /**
     * Stops the EASy-Producer components of the {@link #easyUtilities} and deletes all IVML model files created during
     * the tests in this class.
     */
    @AfterClass
    public static void teardown() {
        try {
            easyUtilities.stopEASyComponents();
        } catch (EASyUtilitiesException e) {
            fail(e);
        }
        
        File[] createdIvmlFiles = AllTests.TEST_IVML_FILES_DIRECTORY.listFiles(new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(NEW_IVML_FILES_PREFIX);
            }
        });
        for (int i = 0; i < createdIvmlFiles.length; i++) {
            try {
                FileUtilities.INSTANCE.delete(createdIvmlFiles[i]);
            } catch (FileUtilitiesException e) {
                fail(e);
            }
        }
    }
    
    /**
     * Tests whether the addition of a new model is successful.
     */
    @Test
    public void testAddModel() {
        String newModelString = "project NewModel {\r\n"
                + "\r\n"
                + "    import DevOpt_System;"
                + "}";
        String newModelFileName = NEW_IVML_FILES_PREFIX + System.currentTimeMillis();
        
        try {
            String addedProjectName = easyUtilities.addModel(newModelString, newModelFileName);
            assertEquals("NewModel", addedProjectName, "No or wrong IVML model added");
        } catch (EASyUtilitiesException e) {
            e.printStackTrace();
            assertNull(e, "Adding a new model should not throw an excpetion");
        }
    }
    
}
