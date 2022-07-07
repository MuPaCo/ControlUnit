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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.AbstractEASyBasedTests;
import net.ssehub.devopt.controllayer.AllTests;

/**
 * This class contains unit tests for the {@link EASyUtilities#addModelLocation(File)} method.
 *  
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class EASyUtilitiesModelLocationAdditionTests extends AbstractEASyBasedTests {

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The file denoting the test IVML model location</li>
     * <li>The expected return value, if {@link EASyUtilities#addModelLocation(File)} is called with the test IVML model
     *     location</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, false},
            {new File("thisdoesnotexist"), false},
            {AllTests.TEST_IVML_FILES_DIRECTORY, true}, // IVML test file directory should not be added yet
            {new File("./model"), false} // DevOpt meta model directory already added during EASy startup
    };
    
    /**
     * The file denoting the IVML model location to use for addition.
     */
    private File testModelLocationFile;
    
    /**
     * The expected return value of {@link EASyUtilities#addModelLocation(File)}, if this method is called with
     * {@link #testModelLocationFile}.
     */
    private boolean expectedModelLocationAdded;

    /**
     * Constructs a new {@link EASyUtilitiesModelLocationAdditionTests} instance for testing one particular subset of
     * the {@link #TEST_DATA}.
     * 
     * @param testModelLocationFile the file denoting the IVML model location to use for addition
     * @param expectedModelLocationAdded the expected return value of {@link EASyUtilities#addModelLocation(File)}, if
     *        this method is called with {@link #testModelLocationFile}
     */
    public EASyUtilitiesModelLocationAdditionTests(File testModelLocationFile, boolean expectedModelLocationAdded) {
        this.testModelLocationFile = testModelLocationFile;
        this.expectedModelLocationAdded = expectedModelLocationAdded;
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
     * Tests whether {@link EASyUtilities#addModelLocation(File)} returns the {@link #expectedModelLocationAdded} value.
     */
    @Test
    public void testAddModelLocation() {
        try {
            assertEquals(expectedModelLocationAdded, easyUtilities.addModelLocation(testModelLocationFile),
                    "Incorrect addition of model location \"" + testModelLocationFile + "\"");
        } catch (EASyUtilitiesException e) {
            fail("Unexpected error while testing addition of model location \"" + testModelLocationFile + "\"", e);
        }
    }
    
}
