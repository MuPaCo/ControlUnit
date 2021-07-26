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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.devopt.controllayer.utilities.Logger.LogLevel;

/**
 * This class contains unit tests for the {@link Logger}.
 * 
 * @author kroeher
 *
 */
public class LoggerTests {
    
    /**
     * The identifier of this class used to create log entries.
     */
    private static final String ID = "LoggerTest";
    
    /**
     * The constant string representing the log entry used in the tests of this class.
     */
    private static final String TEST_LOG_ENTRY = "Test-Log-Entry";
    
    /**
     * The constant and system-dependent line separator for including line breaks.
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    
    /**
     * The constant string included by the {@link Logger}, if basic information is logged.
     */
    private static final String MESSAGE_TYPE_INFO = "[I]";
    
    /**
     * The constant string included by the {@link Logger}, if a warning is logged.
     */
    private static final String MESSAGE_TYPE_WARNING = "[W]";
    
    /**
     * The constant string included by the {@link Logger}, if an error is logged.
     */
    private static final String MESSAGE_TYPE_ERROR = "[E]";
    
    /**
     * The constant string included by the {@link Logger}, if debug information is logged.
     */
    private static final String MESSAGE_TYPE_DEBUG = "[D]";
    
    /**
     * The constant string included by the {@link Logger}, if any of the public log methods receives no lines to log
     * (either <code>null</code>, or <i>empty</i> line(s)).
     */
    private static final String NO_MESSAGE_RECEIVED = "{NMR}";
    
    /**
     * This class realizes a custom output stream, which is added to the {@link Logger} to intercept the final log
     * entries it creates.
     * 
     * @author kroeher
     *
     */
    private static class TestOutputStream extends OutputStream {
        
        /**
         * The constant bytes, which the {@link Logger} writes to any output stream to signal that there will not be
         * more bytes to receive.
         */
        private final byte[] endOfLogEntry = {(byte) '\r', (byte) '\n'};
        
        /**
         * The global string builder for building the log entry, which the {@link Logger} wants to write. 
         */
        private StringBuilder outputStringBuilder;
        
        /**
         * Constructs a new instance of this class.
         */
        private TestOutputStream() {
            outputStringBuilder = new StringBuilder();
        }
        
        /**
         * Returns the output this output stream should have written since to last time this method was called. Each
         * {@link #write(byte[])} call extends this output, while each {@link #flush()} call creates a line break within
         * that output. Calling this method resets that output to be empty after returning it. Hence, any subsequent
         * call to this method without additional writing results in returning an empty string.
         * 
         * @return the output of this output stream; never <code>null</code>, but may be <i>empty</i>, if there was
         *         nothing to write, or this method was already called after last writing
         */
        private String getOutputString() {
            String outputString = outputStringBuilder.toString().trim();
            outputStringBuilder.setLength(0);
            return outputString;
        }

        @Override
        public void write(byte[] bytesToWrite) throws IOException {
            if (!isEndOfLogEntry(bytesToWrite)) {                
                outputStringBuilder.append(new String(bytesToWrite));
            }
        }
        
        /**
         * Checks whether the given byte array contains exactly the same elements in the same order as
         * {@link #endOfLogEntry}.
         *  
         * @param bytes the byte array to check for equality
         * @return <code>true</code>, if the given byte array is equal to {@link #endOfLogEntry}; <code>false</code>
         *         otherwise
         */
        public boolean isEndOfLogEntry(byte[] bytes) {
            boolean isEndOfLogEntry = true;
            if (bytes.length == endOfLogEntry.length) {
                int bytesCounter = 0;
                while (isEndOfLogEntry && bytesCounter < bytes.length) {
                    if (bytes[bytesCounter] != endOfLogEntry[bytesCounter]) {                        
                        isEndOfLogEntry = false;
                    }
                    bytesCounter++;
                }
            } else {
                isEndOfLogEntry = false;
            }
            return isEndOfLogEntry;
        }

        /**
         * This method has to be implemented due to extending {@link OutputStream}. However, as this method is not used
         * by the {@link Logger}, there is no realization in this class.
         */
        @Deprecated
        @Override
        public void write(int iByte) throws IOException {}
        
        /**
         * Appends a line break to the current content of the {@link #outputStringBuilder}.
         */
        @Override
        public void flush() throws IOException {
            outputStringBuilder.append(LINE_SEPARATOR);
        }
        
        /**
         * Closes the extended {@link OutputStream}.
         */
        @Override
        public void close() throws IOException {
            super.close();
        }
        
    }
    
    /**
     * The output stream, which receives all standard log entries from the {@link Logger} for writing.
     */
    private static TestOutputStream standardOutputStream;
    
    /**
     * The output stream, which receives all standard and all additional debug log entries from the {@link Logger} for
     * writing.
     */
    private static TestOutputStream debugOutputStream;
    
    /**
     * The internal reference of this class to the {@link Logger#INSTANCE}.
     */
    private static Logger logger;
    
    /**
     * The list of all output streams the {@link Logger} is aware of. The logger writes each log entry representing
     * basic information, a warning, or an error to each of these output streams.  
     */
    private static List<OutputStream> allOutputStreamsList;
    
    /**
     * The list of debug output streams the {@link Logger} is aware of. The logger writes each log entry representing
     * basic information, a warning, an error, or a debug information to each of these output streams.  
     */
    private static List<OutputStream> debugOutputStreamsList;
    
    /**
     * Creates the {@link #standardOutputStream} and the {@link #debugOutputStream} as well as a reference to the
     * {@link Logger#INSTANCE} as the internal {@link #logger} of this class. Further, it adds the created output
     * streams to the logger and provides access to its internal lists of output streams via the
     * {@link #allOutputStreamsList} and the {@link #debugOutputStreamsList}.
     */
    @BeforeClass
    public static void setup() {
        logger = Logger.INSTANCE;
        assertTrue(accessOutputStreamLists(), "Accessing logger output stream lists failed");
        standardOutputStream = new TestOutputStream();
        debugOutputStream = new TestOutputStream();
        assertTrue(logger.addOutputStream(standardOutputStream, LogLevel.STANDARD),
                "Adding standard output stream to logger during setup failed");
        assertTrue(logger.addOutputStream(debugOutputStream, LogLevel.DEBUG),
                "Adding debug output stream to logger during setup failed");
    }
    
    /**
     * Creates references to the internal lists of output streams of the {@link Logger} via the
     * {@link #allOutputStreamsList} and the {@link #debugOutputStreamsList}.
     * 
     * @return <code>true</code>, if creating both references was successful; <code>false</code> otherwise, which
     *         implies that the {@link #allOutputStreamsList} and the {@link #debugOutputStreamsList} are both
     *         <code>null</code>
     */
    private static boolean accessOutputStreamLists() {
        allOutputStreamsList = getOutputStreamList("allOutputStreamsList");
        debugOutputStreamsList = getOutputStreamList("debugOutputStreamsList");
        return (allOutputStreamsList != null && debugOutputStreamsList != null);
    }
    
    /**
     * Retrieves the list of output streams with the given field name from the {@link Logger} vie reflection.
     * 
     * @param fieldName the name of the field representing one of the lists of output streams of the logger
     * @return the list of output streams of the logger with the given name or <code>null</code>, if no such field
     *         exists, the field is not accessible, or not of the expected list type
     */
    @SuppressWarnings("unchecked")
    private static List<OutputStream> getOutputStreamList(String fieldName) {
        List<OutputStream> outputStreamList = null;
        if (fieldName != null && !fieldName.isBlank()) {
            try {
                Field loggerField = logger.getClass().getDeclaredField(fieldName);
                loggerField.setAccessible(true);
                outputStreamList = (List<OutputStream>) loggerField.get(logger);
            } catch (NoSuchFieldException e) {
                System.err.println(ID + ": Retrieving logger field " + fieldName + " failed");
                e.printStackTrace();
            } catch (SecurityException e) {
                System.err.println(ID + ": Retrieving logger field " + fieldName + " failed");
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                System.err.println(ID + ": Accessing logger field " + fieldName + " failed");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println(ID + ": Accessing logger field " + fieldName + " failed");
                e.printStackTrace();
            } catch (ClassCastException e) {
                System.err.println(ID + ": Casting logger field " + fieldName + " to list of streams failed");
                e.printStackTrace();
            }
        }
        return outputStreamList;
    }
    
    /**
     * Removes the {@link #standardOutputStream} and the {@link #debugOutputStream} from the logger. Further, it closes
     * both output streams.
     */
    @AfterClass
    public static void teardown() {
        assertTrue(logger.removeOutputStream(standardOutputStream),
                "Removing standard output stream from logger during teardown failed");
        assertTrue(logger.removeOutputStream(debugOutputStream),
                "Removing debug output stream from logger during teardown failed");
        try {
            standardOutputStream.close();
        } catch (IOException e) {
            assertNull(e);
        }
        try {
            debugOutputStream.close();
        } catch (IOException e) {
            assertNull(e);
        }
    }
    
    /**
     * Tests whether the addition of an output stream fails, if both arguments are <code>null</code>.
     */
    @Test
    public void testAddOutputStreamAllNull() {
        int expectedNumberOfAllOutputStreams = allOutputStreamsList.size();
        int expectedNumberOfDebugOutputStreams = debugOutputStreamsList.size();
        
        assertFalse(logger.addOutputStream(null, null), "Providing null-arguments must fail");
        
        assertEquals(expectedNumberOfAllOutputStreams, allOutputStreamsList.size(),
                "Number of all output streams must be equal");
        assertEquals(expectedNumberOfDebugOutputStreams, debugOutputStreamsList.size(),
                "Number of debug output streams must be equal");
    }
    
    /**
     * Tests whether the addition of an output stream fails, if only the output stream to add is <code>null</code>.
     */
    @Test
    public void testAddOutputStreamStreamNull() {
        int expectedNumberOfAllOutputStreams = allOutputStreamsList.size();
        int expectedNumberOfDebugOutputStreams = debugOutputStreamsList.size();
        
        assertFalse(logger.addOutputStream(null, LogLevel.STANDARD), "Providing null as output stream must fail");
        
        assertEquals(expectedNumberOfAllOutputStreams, allOutputStreamsList.size(),
                "Number of all output streams must be equal");
        assertEquals(expectedNumberOfDebugOutputStreams, debugOutputStreamsList.size(),
                "Number of debug output streams must be equal");
    }
    
    /**
     * Tests whether the addition of an output stream fails, if only the log level for the output stream to add is
     * <code>null</code>.
     */
    @Test
    public void testAddOutputStreamLogLevelNull() {
        int expectedNumberOfAllOutputStreams = allOutputStreamsList.size();
        int expectedNumberOfDebugOutputStreams = debugOutputStreamsList.size();
        
        assertFalse(logger.addOutputStream(new TestOutputStream(), null), "Providing null as log level must fail");
        
        assertEquals(expectedNumberOfAllOutputStreams, allOutputStreamsList.size(),
                "Number of all output streams must be equal");
        assertEquals(expectedNumberOfDebugOutputStreams, debugOutputStreamsList.size(),
                "Number of debug output streams must be equal");
    }
    
    /**
     * Tests whether the addition of an output stream is successful, if a valid output stream and
     * {@link LogLevel#STANDARD} is provided.
     */
    @Test
    public void testAddOutputStreamValidStandardOutputStream() {
        TestOutputStream testOutputStream = new TestOutputStream();
        
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.STANDARD),
                "Providing valid output stream and log level must not fail");
        
        assertTrue(allOutputStreamsList.contains(testOutputStream),
                "List of all output streams must contain test output stream");
        
        assertFalse(debugOutputStreamsList.contains(testOutputStream),
                "List of debug output streams must not contain test output stream");
        
    }
    
    /**
     * Tests whether the addition of an output stream is successful, if a valid output stream and
     * {@link LogLevel#DEBUG} is provided.
     */
    @Test
    public void testAddOutputStreamValidDebugOutputStream() {
        TestOutputStream testOutputStream = new TestOutputStream();
        
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.DEBUG),
                "Providing valid output stream and log level must not fail");
        
        assertTrue(allOutputStreamsList.contains(testOutputStream),
                "List of all output streams must contain test output stream");
        
        assertTrue(debugOutputStreamsList.contains(testOutputStream),
                "List of debug output streams must contain test output stream");
    }
    
    /**
     * Tests whether adding the same valid output stream with {@link LogLevel#STANDARD} twice is denied.
     */
    @Test
    public void testAddOutputStreamValidStandardOutputStreamTwice() {
        TestOutputStream testOutputStream = new TestOutputStream();
        
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.STANDARD),
                "Adding valid standard output stream once must not fail");
        
        assertFalse(logger.addOutputStream(testOutputStream, LogLevel.STANDARD),
                "Adding valid standard output stream twice must not fail");
        
        assertEquals(1, countOccurrences(testOutputStream, allOutputStreamsList),
                "Entries in list of all output streams must be unqiue");
        
        assertFalse(debugOutputStreamsList.contains(testOutputStream),
                "List of debug output streams must not contain test output stream");
    }
    
    /**
     * Tests whether adding the same valid output stream with {@link LogLevel#DEBUG} twice is denied.
     */
    @Test
    public void testAddOutputStreamValidDebugOutputStreamTwice() {
        TestOutputStream testOutputStream = new TestOutputStream();
        
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.DEBUG),
                "Adding valid standard output stream once must not fail");
        
        assertFalse(logger.addOutputStream(testOutputStream, LogLevel.DEBUG),
                "Adding valid standard output stream twice must not fail");
        
        assertEquals(1, countOccurrences(testOutputStream, allOutputStreamsList),
                "Entries in list of all output streams must be unqiue");
        
        assertEquals(1, countOccurrences(testOutputStream, debugOutputStreamsList),
                "Entries in list of debug output streams must be unqiue");
    }
    
    /**
     * Tests whether adding the same valid output stream first with {@link LogLevel#STANDARD} and second with
     * {@link LogLevel#DEBUG} is successful.
     */
    @Test
    public void testAddOutputStreamValidStandardAndDebug() {
        TestOutputStream testOutputStream = new TestOutputStream();
        
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.STANDARD),
                "Adding valid standard output stream first for standard log level must success");
        
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.DEBUG),
                "Adding valid standard output stream second for debug log level must success");
        
        assertEquals(1, countOccurrences(testOutputStream, allOutputStreamsList),
                "Entries in list of all output streams must be unqiue");
        
        assertEquals(1, countOccurrences(testOutputStream, debugOutputStreamsList),
                "Entries in list of debug output streams must be unqiue");
    }
    
    /**
     * Counts the occurrences of the given output stream in the given list of output streams.
     * 
     * @param outputStream the output stream for which its occurrences in the given list should be counted
     * @param outputStreamList the list of output streams in which the occurrences of the given output stream should be
     *        counted
     * @return the occurrences of the output stream in the list; always greater or equal to <code>0</code> 
     */
    private int countOccurrences(OutputStream outputStream, List<OutputStream> outputStreamList) {
        int outputStreamOccurrences = 0;
        for (OutputStream listEntry : outputStreamList) {
            if (listEntry.equals(outputStream)) {
                outputStreamOccurrences++;
            }
        }
        return outputStreamOccurrences;
    }
    
    /**
     * Tests whether the removal of an output stream fails, if the output stream to remove is <code>null</code>.
     */
    @Test
    public void testRemoveOutputStreamStreamNull() {
        int expectedNumberOfAllOutputStreams = allOutputStreamsList.size();
        int expectedNumberOfDebugOutputStreams = debugOutputStreamsList.size();
        
        assertFalse(logger.removeOutputStream(null), "Providing null-argument must fail");
        
        assertEquals(expectedNumberOfAllOutputStreams, allOutputStreamsList.size(),
                "Number of all output streams must be equal");
        assertEquals(expectedNumberOfDebugOutputStreams, debugOutputStreamsList.size(),
                "Number of debug output streams must be equal");
    }
    
    /**
     * Tests whether the removal of an output stream fails, if the output stream to remove was not added before.
     */
    @Test
    public void testRemoveOutputStreamNewStream() {
        int expectedNumberOfAllOutputStreams = allOutputStreamsList.size();
        int expectedNumberOfDebugOutputStreams = debugOutputStreamsList.size();
        
        assertFalse(logger.removeOutputStream(new TestOutputStream()), "Removing unknown output stream must fail");
        
        assertEquals(expectedNumberOfAllOutputStreams, allOutputStreamsList.size(),
                "Number of all output streams must be equal");
        assertEquals(expectedNumberOfDebugOutputStreams, debugOutputStreamsList.size(),
                "Number of debug output streams must be equal");
    }
    
    /**
     * Tests whether the addition and subsequent removal of an output stream is successful, if a valid output stream and
     * {@link LogLevel#STANDARD} is provided.
     */
    @Test
    public void testAddAndRemoveOutputStreamValidStandardOutputStream() {
        TestOutputStream testOutputStream = new TestOutputStream();
        
        // Addition of output stream
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.STANDARD),
                "Providing valid output stream and log level must not fail");
        assertTrue(allOutputStreamsList.contains(testOutputStream),
                "List of all output streams must contain test output stream");
        assertFalse(debugOutputStreamsList.contains(testOutputStream),
                "List of debug output streams must not contain test output stream");
        
        // Removal of output stream
        assertTrue(logger.removeOutputStream(testOutputStream),
                "Removing known output stream must not fail");
        assertFalse(allOutputStreamsList.contains(testOutputStream),
                "List of all output streams must not contain test output stream");
        assertFalse(debugOutputStreamsList.contains(testOutputStream),
                "List of debug output streams must not contain test output stream");
    }
    
    /**
     * Tests whether the addition and subsequent removal of an output stream is successful, if a valid output stream and
     * {@link LogLevel#DEBUG} is provided.
     */
    @Test
    public void testAddAndRemoveOutputStreamValidDebugOutputStream() {
        TestOutputStream testOutputStream = new TestOutputStream();
        
        // Addition of output stream
        assertTrue(logger.addOutputStream(testOutputStream, LogLevel.DEBUG),
                "Providing valid output stream and log level must not fail");
        assertTrue(allOutputStreamsList.contains(testOutputStream),
                "List of all output streams must contain test output stream");
        assertTrue(debugOutputStreamsList.contains(testOutputStream),
                "List of debug output streams must contain test output stream");
        
        // Removal of output stream
        assertTrue(logger.removeOutputStream(testOutputStream),
                "Removing known output stream must not fail");
        assertFalse(allOutputStreamsList.contains(testOutputStream),
                "List of all output streams must not contain test output stream");
        assertFalse(debugOutputStreamsList.contains(testOutputStream),
                "List of debug output streams must not contain test output stream");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected basic information
     * log entries, if the given ID and the given log entry are <code>null</code>.
     */
    @Test
    public void testLogInfoAllNull() {
        String expectedMessageType = MESSAGE_TYPE_INFO;
        String expectedId = "[null]";
        String expectedLogLine = NO_MESSAGE_RECEIVED;
        
        String nullLogEntry = null;
        logger.logInfo(null, nullLogEntry);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected basic information
     * log entries, if the given ID is {@link #ID} and the given log entry is {@link #TEST_LOG_ENTRY}.
     */
    @Test
    public void testLogInfoAllValid() {
        String expectedMessageType = MESSAGE_TYPE_INFO;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY;
        
        logger.logInfo(ID, expectedLogLine);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected basic information
     * log entries, if the given ID is {@link #ID} and the given log entry consists of two lines. Each line contains the
     * {@link #TEST_LOG_ENTRY}.
     */
    @Test
    public void testLogInfoAllValidMultiLine() {
        String expectedMessageType = MESSAGE_TYPE_INFO;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY + System.lineSeparator() 
                + "                                       " + TEST_LOG_ENTRY;
        
        logger.logInfo(ID, TEST_LOG_ENTRY, TEST_LOG_ENTRY);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected warning log
     * entries, if the given ID and the given log entry are <code>null</code>.
     */
    @Test
    public void testLogWarningAllNull() {
        String expectedMessageType = MESSAGE_TYPE_WARNING;
        String expectedId = "[null]";
        String expectedLogLine = NO_MESSAGE_RECEIVED;
        
        String nullLogEntry = null;
        logger.logWarning(null, nullLogEntry);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected warning log
     * entries, if the given ID is {@link #ID} and the given log entry is {@link #TEST_LOG_ENTRY}.
     */
    @Test
    public void testLogWarningAllValid() {
        String expectedMessageType = MESSAGE_TYPE_WARNING;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY;
        
        logger.logWarning(ID, expectedLogLine);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected warning log
     * entries, if the given ID is {@link #ID} and the given log entry consists of two lines. Each line contains the
     * {@link #TEST_LOG_ENTRY}.
     */
    @Test
    public void testLogWarningAllValidMultiLine() {
        String expectedMessageType = MESSAGE_TYPE_WARNING;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY + System.lineSeparator() 
                + "                                       " + TEST_LOG_ENTRY;
        
        logger.logWarning(ID, TEST_LOG_ENTRY, TEST_LOG_ENTRY);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected error log entries,
     * if the given ID and the given log entry are <code>null</code>.
     */
    @Test
    public void testLogErrorAllNull() {
        String expectedMessageType = MESSAGE_TYPE_ERROR;
        String expectedId = "[null]";
        String expectedLogLine = NO_MESSAGE_RECEIVED;
        
        String nullLogEntry = null;
        logger.logError(null, nullLogEntry);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected error log entries,
     * if the given ID is {@link #ID} and the given log entry is {@link #TEST_LOG_ENTRY}.
     */
    @Test
    public void testLogErrorAllValid() {
        String expectedMessageType = MESSAGE_TYPE_ERROR;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY;
        
        logger.logError(ID, expectedLogLine);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected error log entries,
     * if the given ID is {@link #ID} and the given log entry consists of two lines. Each line contains the
     * {@link #TEST_LOG_ENTRY}.
     */
    @Test
    public void testLogErrorAllValidMultiLine() {
        String expectedMessageType = MESSAGE_TYPE_ERROR;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY + System.lineSeparator() 
                + "                                       " + TEST_LOG_ENTRY;
        
        logger.logError(ID, TEST_LOG_ENTRY, TEST_LOG_ENTRY);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #debugOutputStream} creates the expected debug log entry, if the given ID and the given log
     * entry are <code>null</code>. Logging debug information should not create log entries on
     * {@link #standardOutputStream}, which is checked as well in this method.
     */
    @Test
    public void testLogDebugAllNull() {
        String expectedMessageType = MESSAGE_TYPE_DEBUG;
        String expectedId = "[null]";
        String expectedLogLine = NO_MESSAGE_RECEIVED;
        
        String nullLogEntry = null;
        logger.logDebug(null, nullLogEntry);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String streamName = "Standard output";
        assertEquals("", actualStandardOutput, streamName + " must not contain debug information");
        
        String actualDebugOutput = debugOutputStream.getOutputString();
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #debugOutputStream} creates the expected debug log entry, if the given ID is {@link #ID} and
     * the given log entry is {@link #TEST_LOG_ENTRY}. Logging debug information should not create log entries on
     * {@link #standardOutputStream}, which is checked as well in this method.
     */
    @Test
    public void testLogDebugAllValid() {
        String expectedMessageType = MESSAGE_TYPE_DEBUG;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY;
        
        logger.logDebug(ID, expectedLogLine);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String streamName = "Standard output";
        assertEquals("", actualStandardOutput, streamName + " must not contain debug information");
        
        String actualDebugOutput = debugOutputStream.getOutputString();
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #debugOutputStream} creates the expected debug log entry, if the given ID is {@link #ID} and
     * the given log entry consists of two lines. Each line contains the {@link #TEST_LOG_ENTRY}. Logging debug
     * information should not create log entries on {@link #standardOutputStream}, which is checked as well in this
     * method.
     */
    @Test
    public void testLogDebugAllValidMultiLine() {
        String expectedMessageType = MESSAGE_TYPE_DEBUG;
        String expectedId = "[" + ID + "]";
        String expectedLogLine = TEST_LOG_ENTRY + System.lineSeparator() 
                + "                                       " + TEST_LOG_ENTRY;
        
        logger.logDebug(ID, TEST_LOG_ENTRY, TEST_LOG_ENTRY);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String streamName = "Standard output";
        assertEquals("", actualStandardOutput, streamName + " must not contain debug information");
        
        String actualDebugOutput = debugOutputStream.getOutputString();
        streamName = "Debug output";
        assertTrue(hasTimestamp(actualDebugOutput), streamName + " must contain timestamp");
        assertEquals(expectedMessageType, getMessageType(actualDebugOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutput), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected error log entries,
     * if the given ID and the given exception are <code>null</code>. Further, this test checks whether
     * {@link #debugOutputStream} creates an additional debug log entry as expected.
     */
    @Test
    public void testLogExceptionAllNull() {
        String expectedId = "[null]";
        String expectedStandardMessageType = MESSAGE_TYPE_ERROR;
        String expectedDebugMessageType = MESSAGE_TYPE_DEBUG;
        String expectedLogLine = NO_MESSAGE_RECEIVED;
                
        logger.logException(null, null);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedStandardMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualStandardOutput), streamName + " contains wrong log line");
        
        streamName = "Debug output";
        String[] actualDebugOutputParts = actualDebugOutput.split(LINE_SEPARATOR);
        /*
         * First part (line) must be the same error message as in standard output
         * Second part (line) must be additional debug information
         */
        assertEquals(2, actualDebugOutputParts.length, streamName + " must contain two entries");
        // Check first part as for standard output
        assertTrue(hasTimestamp(actualDebugOutputParts[0]), streamName + " must contain timestamp");
        assertEquals(expectedStandardMessageType, getMessageType(actualDebugOutputParts[0]),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutputParts[0]), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutputParts[0]), streamName + " contains wrong log line");
        // Check second part regarding debug information
        assertTrue(hasTimestamp(actualDebugOutputParts[1]), streamName + " must contain timestamp");
        assertEquals(expectedDebugMessageType, getMessageType(actualDebugOutputParts[1]),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutputParts[1]), streamName + " contains wrong caller id");
        assertEquals(expectedLogLine, getLogLine(actualDebugOutputParts[1]), streamName + " contains wrong log line");
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected error log entries,
     * if the given ID is {@link #ID} and the given exception is a valid instance of {@link Exception}. Further, this
     * test checks whether {@link #debugOutputStream} creates an additional debug log entry as expected.
     * 
     * <b>Note</b>: Changes to this test class resulting in an addition or removal of lines also changes to exception
     *              messages generated at runtime of this test. Hence, the expected log line has to be adapted
     *              accordingly
     */
    @Test
    public void testLogExceptionAllValid() {
        String expectedId = "[" + ID + "]";
        String expectedStandardMessageType = MESSAGE_TYPE_ERROR;
        String expectedDebugMessageType = MESSAGE_TYPE_DEBUG;
        Exception expectedException = new Exception(TEST_LOG_ENTRY);
        String expectedStandardLogLine = "java.lang.Exception: Test-Log-Entry at "
                + "net.ssehub.devopt.controllayer.utilities.LoggerTests.testLogExceptionAllValid(LoggerTests.java:964)";
        
        logger.logException(ID, expectedException);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedStandardMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedStandardLogLine, getLogLine(actualStandardOutput),
                streamName + " contains wrong log line");
        
        streamName = "Debug output";
        String[] actualDebugOutputParts = actualDebugOutput.split(LINE_SEPARATOR);
        /*
         * First part (line) must be the same error message as in standard output
         * Second part (all following lines) must be additional debug information (the full stack trace here)
         */
        // Check first part as for standard output
        assertTrue(hasTimestamp(actualDebugOutputParts[0]), streamName + " must contain timestamp");
        assertEquals(expectedStandardMessageType, getMessageType(actualDebugOutputParts[0]),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutputParts[0]), streamName + " contains wrong caller id");
        assertEquals(expectedStandardLogLine, getLogLine(actualDebugOutputParts[0]),
                streamName + " contains wrong log line");
        // Check second part regarding debug information
        assertTrue(hasTimestamp(actualDebugOutputParts[1]), streamName + " must contain timestamp");
        assertEquals(expectedDebugMessageType, getMessageType(actualDebugOutputParts[1]),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutputParts[1]), streamName + " contains wrong caller id");
        assertEquals(expectedStandardLogLine, getLogLine(actualDebugOutputParts[1]),
                streamName + " contains wrong log line");
        // No further check of second part as the stack trace is system/runtime dependent
    }
    
    /**
     * Tests whether {@link #standardOutputStream} and {@link #debugOutputStream} create the expected error log entries,
     * if the given ID is {@link #ID} and the given exception is a valid instance of {@link Exception} with an
     * additional cause (nested, valid exception). Further, this test checks whether {@link #debugOutputStream} creates
     * an additional debug log entry as expected.
     * 
     * <b>Note</b>: Changes to this test class resulting in an addition or removal of lines also changes to exception
     *              messages generated at runtime of this test. Hence, the expected log line has to be adapted
     *              accordingly
     */
    @Test
    public void testLogExceptionAllValidMultiCause() {
        String expectedId = "[" + ID + "]";
        String expectedStandardMessageType = MESSAGE_TYPE_ERROR;
        String expectedDebugMessageType = MESSAGE_TYPE_DEBUG;
        Exception causeException = new Exception(TEST_LOG_ENTRY);
        Exception expectedException = new Exception(TEST_LOG_ENTRY, causeException);
        String expectedStandardLogLine = "java.lang.Exception: Test-Log-Entry at "
                + "net.ssehub.devopt.controllayer.utilities.LoggerTests"
                + ".testLogExceptionAllValidMultiCause(LoggerTests.java:1020)";
        
        logger.logException(ID, expectedException);
        
        String actualStandardOutput = standardOutputStream.getOutputString();
        String actualDebugOutput = debugOutputStream.getOutputString();
        
        String streamName = "Standard output";
        assertTrue(hasTimestamp(actualStandardOutput), streamName + " must contain timestamp");
        assertEquals(expectedStandardMessageType, getMessageType(actualStandardOutput),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualStandardOutput), streamName + " contains wrong caller id");
        assertEquals(expectedStandardLogLine, getLogLine(actualStandardOutput),
                streamName + " contains wrong log line");
        
        streamName = "Debug output";
        String[] actualDebugOutputParts = actualDebugOutput.split(LINE_SEPARATOR);
        /*
         * First part (line) must be the same error message as in standard output
         * Second part (all following lines) must be additional debug information (the full stack trace here)
         */
        // Check first part as for standard output
        assertTrue(hasTimestamp(actualDebugOutputParts[0]), streamName + " must contain timestamp");
        assertEquals(expectedStandardMessageType, getMessageType(actualDebugOutputParts[0]),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutputParts[0]), streamName + " contains wrong caller id");
        assertEquals(expectedStandardLogLine, getLogLine(actualDebugOutputParts[0]),
                streamName + " contains wrong log line");
        // Check second part regarding debug information
        assertTrue(hasTimestamp(actualDebugOutputParts[1]), streamName + " must contain timestamp");
        assertEquals(expectedDebugMessageType, getMessageType(actualDebugOutputParts[1]),
                streamName + " contains wrong message type");
        assertEquals(expectedId, getId(actualDebugOutputParts[1]), streamName + " contains wrong caller id");
        assertEquals(expectedStandardLogLine, getLogLine(actualDebugOutputParts[1]),
                streamName + " contains wrong log line");
        assertTrue(containsCause(actualDebugOutputParts, causeException.getClass().getName(),
                causeException.getMessage()), streamName + "must contain exception cause (nested throwable)");
        // No further check of second part as the stack trace is system/runtime dependent
    }
    
    /**
     * Checks whether the given set of strings contains an element, which equals a cause description constructed by the
     * given exception class name and message.
     * 
     * @param outputLines the set of strings to search in
     * @param exceptionClassName the name of the exception class representing the cause to search for
     * @param exceptionMessage the exception message describing the cause to search for
     * @return <code>true</code>, if the first element in the given set of strings is found, which (after trimming) is
     *         equal to "Caused by: [exceptionClassName]: [exceptionMessage]"; <code>false</code> otherwise
     */
    private boolean containsCause(String[] outputLines, String exceptionClassName, String exceptionMessage) {
        boolean containsCause = false;
        String causeDescription = "Caused by: " + exceptionClassName + ": " + exceptionMessage;
        int outputLinesCounter = 0;
        while (!containsCause && outputLinesCounter < outputLines.length) {
            containsCause = outputLines[outputLinesCounter].trim().equals(causeDescription);
            outputLinesCounter++;
        }
        return containsCause;
    }
     
    
    /**
     * Checks the availability, the correct position and format of an expected timestamp as part of a log entry.
     *  
     * @param actualOutput the logger output, which should be checked for a correct timestamp
     * @return <code>true</code>, if the given logger output contains a correct timestamp; <code>false</code> otherwise
     */
    private boolean hasTimestamp(String actualOutput) {
        boolean hasTimestamp = false;
        if (actualOutput != null) {
            String[] actualOutputParts = actualOutput.split(" ");
            if (actualOutputParts.length > 0) {
                // First part must be the timestamp of the form: [YYYY-mm-dd@hh:mm:ss]
                hasTimestamp = actualOutputParts[0].matches("\\[\\d{4}-\\d{2}-\\d{2}@\\d{2}:\\d{2}:\\d{2}\\]");
            } else {
                System.out.println("Cannot determine timestamp due to missing output parts: " + actualOutput);
            }
        } else {
            System.out.println("Cannot determine timestamp due to output being null");
        }
        return hasTimestamp;
    }
    
    /**
     * Determines the message type of a log entry. This type must be specified as second part of each log entry.
     * 
     * @param actualOutput the logger output in which the message type should be determined
     * @return the message type of the given logger output as a string or <code>null</code>, if no message type can be
     *         determined
     */
    private String getMessageType(String actualOutput) {
        String messageType = null;
        if (actualOutput != null) {
            String[] actualOutputParts = actualOutput.split(" ");
            if (actualOutputParts.length > 1) {
                // Second part must be the message type: [I], [W], [E], or [D]
                messageType = actualOutputParts[1];
            } else {
                System.out.println("Cannot determine message type due to missing output parts: " + actualOutput);
            }
        } else {
            System.out.println("Cannot determine message type due to output being null");
        }
        return messageType;
    }
    
    /**
     * Determines the ID of the caller of the logger within a log entry. This type must be specified as third part of
     * each log entry.
     * 
     * @param actualOutput the logger output in which the ID of the caller should be determined
     * @return the ID of the caller within the given logger output as a string or <code>null</code>, if no ID can be
     *         determined
     */
    private String getId(String actualOutput) {
        String id = null;
        if (actualOutput != null) {
            String[] actualOutputParts = actualOutput.split(" ");
            if (actualOutputParts.length > 2) {
                // Third part must be the ID of the caller
                id = actualOutputParts[2];
            } else {
                System.out.println("Cannot determine ID due to missing output parts: " + actualOutput);
            }
        } else {
            System.out.println("Cannot determine ID due to output being null");
        }
        return id;
    }
    
    /**
     * Determines the log line (actual log message) of a log entry. This type must be specified as fourth part of
     * each log entry and may span multiple lines. In case of a multi-line log entry, all characters (including line
     * breaks and whitespaces) are included in the single string return of this method.
     * 
     * @param actualOutput the logger output in which the log line should be determined
     * @return the log line of the given logger output as a string or <code>null</code>, if no log line can be
     *         determined
     */
    private String getLogLine(String actualOutput) {
        String logLine = null;
        if (actualOutput != null) {
            /*
             * Each log entry must have the following form:
             * "[TIMESTAMP] [MESSAGE_TYPE] [CALLER_ID] ACTUAL_MESSAGE"
             * 
             * [TIMESTAMP]           has 21 characters
             * Separating whitespace has 1 character
             * [MESSAGE_TYPE]        has 3 characters
             * Separating whitespace has 1 character
             * [CALLER_ID]           has 1 character plus an arbitrary number of characters for the actual ID
             * 
             * Hence, to calculate the start index of the log line (actual message), we have to search for the first
             * character after the whitespace found after index 26 (21 + 1 + 3 + 1 + 1 = 27 characters for timestamp,
             * whitespace, message type, whitespace, open bracket of caller ID).
             */
            int logLineStartIndex = actualOutput.indexOf(" ", 26);
            if (logLineStartIndex != -1) {
                logLineStartIndex = logLineStartIndex + 1;
                if (logLineStartIndex < actualOutput.length()) {                    
                    logLine = actualOutput.substring(logLineStartIndex);
                } else {
                    System.out.println("Cannot determine log entry due to log line start index [" + logLineStartIndex 
                            + "] being larger than the actual string:" + actualOutput);
                }
            } else {
                System.out.println("Cannot determine log entry due to missing whitespace after index 26:" 
                        + actualOutput);
            }
        } else {
            System.out.println("Cannot determine log entry due to output being null");
        }
        return logLine;
    }
    
    

    

}
