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
package net.ssehub.devopt.controllayer;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import net.ssehub.devopt.controllayer.utilities.EASyUtilities;
import net.ssehub.devopt.controllayer.utilities.EASyUtilitiesException;
import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;

/**
 * This abstract class realizes preparation, utility, and post-processing methods, which are necessary for tests
 * involving the {@link EASyUtilities} instance. Hence, each specific test class requiring that instance should extend
 * this abstract class.
 * 
 * @author kroeher
 *
 */
public abstract class AbstractEASyBasedTests {
    
    /**
     * The local reference to the global {@link EASyUtilities}.
     */
    protected static EASyUtilities easyUtilities = EASyUtilities.INSTANCE;

    /**
     * Starts the EASy-Producer components of the {@link #easyUtilities}.
     */
    @BeforeClass
    public static void setup() {
        try {
            easyUtilities.startEASyComponents();
        } catch (EASyUtilitiesException e) {
            fail(e);
        }
    }
    
    /**
     * Stops the EASy-Producer components of the {@link #easyUtilities}.
     */
    @AfterClass
    public static void teardown() {
        try {
            easyUtilities.stopEASyComponents();
        } catch (EASyUtilitiesException e) {
            fail(e);
        }
    }
    
    /**
     * Reads the content of the given file and returns it as a single string using {@link System#lineSeparator()} to
     * concatenate individual file lines.
     * 
     * @param ivmlModelFile the file to read to content from
     * @return the content of the given file or <code>null</code>, if reading the content fails
     */
    protected String getIvmlModelString(File ivmlModelFile) {
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
    
}
