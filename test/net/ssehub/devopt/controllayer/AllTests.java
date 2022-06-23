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

import java.io.File;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.devopt.controllayer.model.ModelReceiverMqttTests;
import net.ssehub.devopt.controllayer.network.HttpClientCreationTests;
import net.ssehub.devopt.controllayer.network.HttpClientUsageTests;
import net.ssehub.devopt.controllayer.network.HttpServerCreationTests;
import net.ssehub.devopt.controllayer.network.HttpServerUsageTests;
import net.ssehub.devopt.controllayer.network.MqttV3ClientCreationTests;
import net.ssehub.devopt.controllayer.network.MqttV3ClientUsageTests;
import net.ssehub.devopt.controllayer.utilities.EASyUtilitiesTests;
import net.ssehub.devopt.controllayer.utilities.LoggerTests;

/**
 * Definition of this test suite.
 */
@RunWith(Suite.class)
@SuiteClasses({
    // Core tests
    SetupCreationTests.class,
    SetupUsageTests.class,
    // Utilities tests
    LoggerTests.class,
    EASyUtilitiesTests.class,
    // Network tests
    HttpClientCreationTests.class,
    HttpClientUsageTests.class,
    HttpServerCreationTests.class,
    HttpServerUsageTests.class,
    MqttV3ClientCreationTests.class,
    MqttV3ClientUsageTests.class, /* These tests are unstable; see class description */
    // Model tests
    ModelReceiverMqttTests.class
    })

/**
 * This class summarizes individual test classes into a single test suite and provides common attributes those tests.
 * 
 * @author kroeher
 *
 */
public class AllTests {

    /**
     * The constant file denoting the base directory, which contains all test data used by some for the tests. 
     */
    public static final File TEST_DATA_DIRECTORY = new File("./testdata");
    
    /**
     * The constant file denoting the directory, which contains IVML files for testing. 
     */
    public static final File TEST_IVML_FILES_DIRECTORY = new File(TEST_DATA_DIRECTORY, "ivml");
    
    /**
     * The constant file denoting the directory, which contains configuration files for testing. 
     */
    public static final File TEST_CONFIGURATION_FILES_DIRECTORY = new File(TEST_DATA_DIRECTORY, "setup");
    
}
