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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.devopt.controllayer.utilities.EASyUtilities;
import net.ssehub.devopt.controllayer.utilities.EASyUtilitiesException;
import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;

/**
 * This class contains unit tests for the {@link ModelReceiver} using its MQTT-based reception channel.<br>
 * <br>
 * Note that the tests in this class directly call the public method messageArrived() to avoid using network
 * connections and waiting for delivery of messages during unit tests. While this bypasses the internal network
 * components of the {@link ModelReceiver} class, it is sufficient to test its main purpose of receiving messages that
 * should include models for registration of local components.<br>
 * <br>
 * As a consequence, the {@link ModelReceiver} instance used in the tests of this class will never be started or
 * stopped, which typically establishes or terminates its network connection.
 * 
 * @author kroeher
 *
 */
public class ModelReceiverMqttTests {
    
    /**
     * The constant URL of the MQTT broker used to create the {@link #modelReceiver}.
     */
    private static final String TEST_BROKER_URL = "tcp://broker.hivemq.com";
    
    /**
     * The constant number defining the port of the MQTT broker used to create the {@link #modelReceiver}.
     */
    private static final int TEST_BROKER_PORT = 1883;
    
    /**
     * The constant MQTT topic used in these tests.
     */
    private static final String TEST_TOPIC = ModelReceiverMqttTests.class.getSimpleName();
    
    /**
     * The local reference to the global {@link EASyUtilities}.
     */
    private static EASyUtilities easyUtilities = EASyUtilities.INSTANCE;
    
    /**
     * The file denoting an initially empty directory as temporal model directory to start the EASy-Producer components
     * without immediately loading models at start-up.
     */
    private static File tempModelDirectory;
    
    /**
     * The {@link ModelReceiver} instance for the tests in this class.
     */
    private static ModelReceiver modelReceiver;
    
    /**
     * The callback to be informed by the {@link #modelReceiver}, if it receives a new IVML model.
     */
    private static TestModelReceptionCallback callback;
    
    /**
     * Creates the {@link #tempModelDirectory}, starts the EASy-Producer components of the {@link #easyUtilities} using
     * that directory as model directory, creates the {@link #callback} and the {@link #modelReceiver} instances for
     * testing.
     */
    @BeforeClass
    public static void setup() {
        // Create temporal, empty model directory to start the EASy components without immediately loading models 
        tempModelDirectory = new File(AllTests.TEST_IVML_FILES_DIRECTORY, "test_" + System.currentTimeMillis());
        try {
            tempModelDirectory = FileUtilities.INSTANCE.createDirectory(tempModelDirectory.getAbsolutePath());
        } catch (FileUtilitiesException e) {
            fail("Creating temporal model directory for EASy components during setup failed", e);
        }
        // Start the EASy components using the canonical path to the model directory created above
        try {
            easyUtilities.startEASyComponents(tempModelDirectory.getCanonicalPath());
        } catch (IOException | EASyUtilitiesException e) {
            fail("Starting EASy components during setup failed", e);
        }
        // Create test callback, which the ModelReceiver will inform about received models 
        callback = new TestModelReceptionCallback();
        try {
            /*
             * Create ModelReceiver instance for the tests in this class.
             * Check notes in test class Javadoc for more information about this instance and its usage.
             */
            modelReceiver = new ModelReceiver("MQTT", TEST_BROKER_URL, TEST_BROKER_PORT, TEST_TOPIC, null, null,
                    callback);
        } catch (ModelException e) {
            fail("Creating ModelReceiver instance during setup failed", e);
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
            fail("Stopping EASy components during teardown failed", e);
        }
        // Delete the temporal model directory and all its content used for the tests in this class only
        try {
            FileUtilities.INSTANCE.delete(tempModelDirectory);
        } catch (FileUtilitiesException e) {
            fail("Deleting temporal model directory for EASy components during teardown failed", e);
        }
    }
    
    /**
     * Resets the {@link #callback} after each test in this class. This resets the callback to non-called state with
     * no received IVML file and project names (values are <code>null</code> again).
     */
    @After
    public void resetModelReceiverCallback() {
        callback.reset();
    }
    
    /**
     * Tests the correct reaction to the reception of <code>null</code> as incoming message.<br>
     * <br>
     * Note that tests using an incoming message with <code>null</code> as message content is not possible as creating a
     * {@linkplain MqttMessage} instance with <code>null</code> as payload results in a {@link NullPointerException}.
     */
    @Test
    public void testReceiveNullMessage() {
        MqttMessage message = null;
        boolean expectedWasCalled = false;
        String expectedReceivedIvmlProjectName = null;
        try {
            modelReceiver.messageArrived(TEST_TOPIC, message);
            assertEquals(expectedWasCalled, callback.wasCalled(),
                    "Callback must not be called, if message is \"null\"");
            /*
             * Model receiver creates IVML files with file names based on current time in milliseconds. Hence, we cannot
             * check for the correct file name, but only for it being available or not.
             */
            assertNull("Callback must not hold a file name, if message is \"null\"",
                    callback.getReceivedIvmlFileName());
            assertEquals(expectedReceivedIvmlProjectName, callback.getReceivedIvmlProjectName(),
                    "Callback must not hold a project name, if message is \"null\"");
        // checkstyle: stop exception type check
        } catch (Exception e) {
        // checkstyle: resume exception type check
            fail("No processing and, hence, no exception expected, if message is \"null\"", e);
        }
    }
    
    /**
     * Tests the correct reaction to the reception of an incoming message with no content (empty string).
     */
    @Test
    public void testReceiveMessageWithEmptyContent() {
        String messageContent = "";
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        boolean expectedWasCalled = false;
        String expectedReceivedIvmlProjectName = null;
        try {
            modelReceiver.messageArrived(TEST_TOPIC, message);
            assertEquals(expectedWasCalled, callback.wasCalled(),
                    "Callback must not be called, if message content is empty");
            /*
             * Model receiver creates IVML files with file names based on current time in milliseconds. Hence, we cannot
             * check for the correct file name, but only for it being available or not.
             */
            assertNull("Callback must not hold a file name, if message content is empty",
                    callback.getReceivedIvmlFileName());
            assertEquals(expectedReceivedIvmlProjectName, callback.getReceivedIvmlProjectName(),
                    "Callback must not hold a project name, if message content is empty");
        // checkstyle: stop exception type check
        } catch (Exception e) {
        // checkstyle: resume exception type check
            fail("No processing and, hence, no exception expected, if message content is empty", e);
        }
    }
    
    /**
     * Tests the correct reaction to the reception of an incoming message with content that is not an IVML model
     * (arbitrary string).
     */
    @Test
    public void testReceiveMessageWithNonModelContent() {
        String messageContent = "This is not a valid IVML model";
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        boolean expectedWasCalled = false;
        String expectedReceivedIvmlProjectName = null;
        try {
            modelReceiver.messageArrived(TEST_TOPIC, message);
            assertEquals(expectedWasCalled, callback.wasCalled(),
                    "Callback must not be called, if message content is not an IVML model");
            /*
             * Model receiver creates IVML files with file names based on current time in milliseconds. Hence, we cannot
             * check for the correct file name, but only for it being available or not.
             */
            assertNull("Callback must not hold a file name, if message content is not an IVML model",
                    callback.getReceivedIvmlFileName());
            assertEquals(expectedReceivedIvmlProjectName, callback.getReceivedIvmlProjectName(),
                    "Callback must not hold a project name, if message content is not an IVML model");
        // checkstyle: stop exception type check
        } catch (Exception e) {
        // checkstyle: resume exception type check
            fail("Processing must not propagate exceptions, if message content is not an IVML model", e);
        }
    }
    
    /**
     * Tests the correct reaction to the reception of an incoming message with content that is a valid, but empty IVML
     * model (<code>project ValidEmptyProject { }</code>).
     */
    @Test
    public void testReceiveMessageWithValidEmptyModelContent() {
        String messageContent = "project ValidEmptyProject { }";
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        boolean expectedWasCalled = true;
        String expectedReceivedIvmlProjectName = "ValidEmptyProject";
        try {
            modelReceiver.messageArrived(TEST_TOPIC, message);
            assertEquals(expectedWasCalled, callback.wasCalled(),
                    "Callback must be called, if message content is a valid IVML model");
            /*
             * Model receiver creates IVML files with file names based on current time in milliseconds. Hence, we cannot
             * check for the correct file name, but only for it being available or not.
             */
            assertNotNull("Callback must hold a file name, if message content is a valid IVML model",
                    callback.getReceivedIvmlFileName());
            assertEquals(expectedReceivedIvmlProjectName, callback.getReceivedIvmlProjectName(),
                    "Callback must hold the project name of the valid IVML model in the message content");
        // checkstyle: stop exception type check
        } catch (Exception e) {
        // checkstyle: resume exception type check
            fail("Processing must not throw exceptions, if message content is a valid IVML model", e);
        }
    }
    
    /**
     * Tests the correct reaction to the reception of an incoming message with content that is an invalid IVML-based
     * DevOpt model (content of <i>InvalidDevOptProject.ivml</i> of the projects test data directory).
     */
    @Test
    public void testReceiveMessageWithInvalidDevOptModelContent() {
        String testIvmlProjectName = "InvalidDevOptProject";
        String testIvmlModelFileName = testIvmlProjectName + ".ivml";
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        String messageContent = getIvmlModelString(testIvmlModelFile);
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        boolean expectedWasCalled = false;
        String expectedReceivedIvmlProjectName = null;
        try {
            modelReceiver.messageArrived(TEST_TOPIC, message);
            assertEquals(expectedWasCalled, callback.wasCalled(),
                    "Callback must be called, if message content is a valid IVML-based DevOpt model");
            /*
             * Model receiver creates IVML files with file names based on current time in milliseconds. Hence, we cannot
             * check for the correct file name, but only for it being available or not.
             */
            assertNull("Callback must not hold a file name, if message content is an invalid IVML-based DevOpt model",
                    callback.getReceivedIvmlFileName());
            assertEquals(expectedReceivedIvmlProjectName, callback.getReceivedIvmlProjectName(),
                    "Callback must not hold a project name, if message content is an invalid IVML-based DevOpt model");
        // checkstyle: stop exception type check
        } catch (Exception e) {
        // checkstyle: resume exception type check
            fail("Processing must not throw exceptions, if message content is an invalid IVML-based DevOpt model", e);
        }
    }
    
    /**
     * Tests the correct reaction to the reception of an incoming message with content that is a valid minimal IVML-
     * based DevOpt model (content of <i>MinimalDevOptProject.ivml</i> of the projects test data directory).
     */
    @Test
    public void testReceiveMessageWithValidMinimalDevOptModelContent() {
        String testIvmlProjectName = "MinimalDevOptProject";
        String testIvmlModelFileName = testIvmlProjectName + ".ivml";
        File testIvmlModelFile = new File(AllTests.TEST_IVML_FILES_DIRECTORY, testIvmlModelFileName);
        String messageContent = getIvmlModelString(testIvmlModelFile);
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        boolean expectedWasCalled = true;
        String expectedReceivedIvmlProjectName = "MinimalDevOptProject";
        try {
            modelReceiver.messageArrived(TEST_TOPIC, message);
            assertEquals(expectedWasCalled, callback.wasCalled(),
                    "Callback must be called, if message content is a valid IVML-based DevOpt model");
            /*
             * Model receiver creates IVML files with file names based on current time in milliseconds. Hence, we cannot
             * check for the correct file name, but only for it being available or not.
             */
            assertNotNull("Callback must hold a file name, if message content is a valid IVML-based DevOpt model",
                    callback.getReceivedIvmlFileName());
            assertEquals(expectedReceivedIvmlProjectName, callback.getReceivedIvmlProjectName(),
                    "Callback must hold the project name of the valid IVML-based DevOpt model in the message content");
        // checkstyle: stop exception type check
        } catch (Exception e) {
        // checkstyle: resume exception type check
            fail("Processing must not throw exceptions, if message content is a valid IVML-based DevOpt model", e);
        }
    }
    
    /**
     * Reads the content of the given file and returns it as a single string using {@link System#lineSeparator()} to
     * concatenate individual file lines.
     * 
     * @param ivmlModelFile the file to read the content from
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

}
