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

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class realizes a thread-safe multi-stream logging mechanism. It is thread-safe in the sense that calls for
 * logging messages are synchronized and, hence, allow calls from different threads. Further, the actual output streams,
 * which are used to write log messages, can be dynamically added and removed at runtime. The logger writes each log
 * message to all known output stream depending on their individual log level.
 * 
 * @author kroeher
 *
 */
public class Logger {
    
    // TODO Logging or at least writing to streams in a separate thread!
    // TODO adding or removing an output stream can only be done, if those streams are not used for writing currently

    /**
     * The singleton instance of this class.
     */
    public static final Logger INSTANCE = new Logger();
    
    /**
     * The constant and system-dependent line separator for including line breaks in multi-line log entries.
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    
    /**
     * The constant string to write, if any of the public log methods receives no lines to log (either
     * <code>null</code>, or <i>empty</i> line(s)).
     */
    private static final String NO_MESSAGE_RECEIVED = "{NMR}";
    
    /**
     * The constant bytes to write to any output stream to signal that there will not be more bytes to receive.
     */
    private static final byte[] END_OF_LOG_ENTRY = {(byte) '\r', (byte) '\n'};
    
    /**
     * The constant format for writing timestamps as part of log entries.
     */
    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");
    
    /**
     * This enumeration defines different log levels for separating standard log entries (basic information, warnings,
     * and errors) from debug information. These log levels enable the differentiation between log entries written to
     * the available output streams. The available log levels are:
     * <ul>
     * <li><code>STANDARD</code>: All basic information, warning, and error messages will be written
     * <li><code>DEBUG</code>: Like <code>STANDARD</code>, but additional debug information will be written as well
     * </ul>
     * 
     * @author kroeher
     * 
     */
    public static enum LogLevel { STANDARD, DEBUG };
    
    /**
     * This enumeration defines the different types of messages, which this logger supports.
     *  
     * @author kroeher
     *
     */
    private enum MessageType {
        INFO("[I]"),
        WARNING("[W]"),
        ERROR("[E]"),
        DEBUG("[D]");
        
        /**
         * The internal label of the respective enumeration.
         */
        private String enumLabel;
        
        /**
         * Constructs a new instance of this enumeration.
         * 
         * @param label the internal label of the respective enumeration
         */
        private MessageType(String label) {
            enumLabel = label;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return enumLabel;
        }
        
    };
    
    /**
     * The list of all output streams this instance is aware of. The logger writes each log entry representing basic
     * information, a warning, or an error to each of these output streams.  
     */
    private List<OutputStream> allOutputStreamsList;
    
    /**
     * The list of debug output streams this instance is aware of. The logger writes each log entry representing basic
     * information, a warning, an error, or a debug information to each of these output streams.  
     */
    private List<OutputStream> debugOutputStreamsList;
    
    /**
     * The string builder used to build the individual log entries.
     */
    private StringBuilder logEntryBuilder;
    
    /**
     * The current number of characters to indent the next log entry line in a multi-line log entry.
     */
    private int logEntryIndentation;
    
    /**
     * Constructs a new {@link Logger} instance.
     */
    private Logger() {
        allOutputStreamsList = new ArrayList<OutputStream>();
        debugOutputStreamsList = new ArrayList<OutputStream>();
        logEntryBuilder = new StringBuilder();
    }
    
    /**
     * Logs the given string as basic information. This string is interpreted as a single line of text and written as
     * such to all output streams this instance is aware of.<br>
     * If the given string to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED} instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLine the string (single line of text) to log
     */
    public synchronized void logInfo(String id, String logLine) {
        log(MessageType.INFO, createTimestamp(), id, logLine);
    }
    
    /**
     * Logs the given set of strings as basic information. Each string is interpreted as a single line of text and
     * written as such to all output streams this instance is aware of. Hence, each string is written in a new line
     * forming a multi-line log entry.<br>
     * If the given set of strings to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED}
     * instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLines the set of strings (multiple lines of text) to log
     */
    public synchronized void logInfo(String id, String... logLines) {
        log(MessageType.INFO, createTimestamp(), id, logLines);
    }
    
    /**
     * Logs the given string as a warning. This string is interpreted as a single line of text and written as such to
     * all output streams this instance is aware of.<br>
     * If the given string to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED} instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLine the string (single line of text) to log
     */
    public synchronized void logWarning(String id, String logLine) {
        log(MessageType.WARNING, createTimestamp(), id, logLine);
    }
    
    /**
     * Logs the given set of strings as a warning. Each string is interpreted as a single line of text and written as
     * such to all output streams this instance is aware of. Hence, each string is written in a new line forming a
     * multi-line log entry.<br>
     * If the given set of strings to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED}
     * instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLines the set of strings (multiple lines of text) to log
     */
    public synchronized void logWarning(String id, String... logLines) {
        log(MessageType.WARNING, createTimestamp(), id, logLines);
    }
    
    /**
     * Logs the given string as an error. This string is interpreted as a single line of text and written as such to all
     * output streams this instance is aware of.<br>
     * If the given string to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED} instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLine the string (single line of text) to log
     */
    public synchronized void logError(String id, String logLine) {
        log(MessageType.ERROR, createTimestamp(), id, logLine);
    }
    
    /**
     * Logs the given set of strings as an error. Each string is interpreted as a single line of text and written as
     * such to all output streams this instance is aware of. Hence, each string is written in a new line forming a
     * multi-line log entry.<br>
     * If the given set of strings to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED}
     * instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLines the set of strings (multiple lines of text) to log
     */
    public synchronized void logError(String id, String... logLines) {
        log(MessageType.ERROR, createTimestamp(), id, logLines);
    }
    
    /**
     * Logs the given string as debug information. This string is interpreted as a single line of text and written as
     * such to all output streams, which support {@link LogLevel#DEBUG} only.<br>
     * If the given string to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED} instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLine the string (single line of text) to log
     */
    public synchronized void logDebug(String id, String logLine) {
        log(MessageType.DEBUG, createTimestamp(), id, logLine);
    }
    
    /**
     * Logs the given set of strings as debug information. Each string is interpreted as a single line of text and
     * written as such to all output streams, which support {@link LogLevel#DEBUG} only. Hence, each string is written
     * in a new line forming a multi-line log entry.<br>
     * If the given set of strings to log is <code>null</code> or blank, the logger writes {@link #NO_MESSAGE_RECEIVED}
     * instead.
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param logLines the set of strings (multiple lines of text) to log
     */
    public synchronized void logDebug(String id, String... logLines) {
        log(MessageType.DEBUG, createTimestamp(), id, logLines);
    }
    
    /**
     * Logs the given exception. Logging exceptions produces two types of log entries:
     * <ul>
     * <li>A simple (single-line) entry describing the main cause of the given exception; this entry is written to all
     *     output streams as an error</li>
     * <li>A detailed (multi-line) entry describing the full stack trace of the given exception; this entry is written
     *     to all output streams, which support {@link LogLevel#DEBUG} only, as debug information</li>
     * </ul>
     * If the given exception to log is <code>null</code>, the logger writes a simple error message with
     * {@link #NO_MESSAGE_RECEIVED} instead.
     * 
     * 
     * @param id the identifier of the caller of this method (e.g. the name of the class) to identify the source of the
     *        written log entry
     * @param exception the exception to log
     */
    public synchronized void logException(String id, Exception exception) {
        // Timestamp must be the same for both types of log entries
        String timestamp = createTimestamp();
        // Simply log entry for all output streams
        String simpleLogEntry = createSimpleLogEntry(exception);
        log(MessageType.ERROR, timestamp, id, simpleLogEntry);
        // Additional detailed log entry for debug output streams
        log(MessageType.DEBUG, timestamp, id, createDetailedLogEntry(simpleLogEntry, exception));
    }
    
    /**
     * Creates a simple (single-line) log entry describing the main cause of the given throwable. This single line
     * consists of the string-representation of the given throwable and the first element of its stack trace, which
     * identifies the last method invocation.
     * 
     * @param throwable the throwable for which a simple log entry should be created
     * @return a simple (single-line) log entry describing the given throwable; may be <code>null</code>, if the given
     *         throwable is <code>null</code>
     */
    private String createSimpleLogEntry(Throwable throwable) {
        String throwableLogEntry = null;
        if (throwable != null) {            
            StringBuilder throwableLogEntryBuilder = new StringBuilder();
            throwableLogEntryBuilder.append(throwable.toString());
            StackTraceElement[] throwableStackTraceElements = throwable.getStackTrace();
            if (throwableStackTraceElements != null && throwableStackTraceElements.length > 0) {
                throwableLogEntryBuilder.append(" at ");
                throwableLogEntryBuilder.append(throwableStackTraceElements[0].toString());
            }
            throwableLogEntry = throwableLogEntryBuilder.toString(); 
        }
        return throwableLogEntry;
    }
    
    /**
     * Creates a detailed (multi-line) log entry describing the given throwable. This detailed entry starts with the
     * given parent message, which is typically the simple log entry of the given throwable created by 
     * {@link #createSimpleLogEntry(Throwable)}. The additional lines of the detailed entry contain the full stack trace
     * of the given throwable and its cause(s).
     * 
     * @param parentMessage the simple log entry of the given throwable
     * @param throwable the throwable for which a detailed log entry should be created
     * @return a detailed (multi-line) log entry describing the given throwable including its stack trace; may be
     *         <code>null</code>, if the given throwable is <code>null</code>
     */
    private String[] createDetailedLogEntry(String parentMessage, Throwable throwable) {
        String[] throwableLogEntry = null;
        if (throwable != null) {            
            List<String> throwableLogEntryList = new ArrayList<String>();
            if (parentMessage != null) {
                throwableLogEntryList.add(parentMessage);
            }
            throwableLogEntryList.addAll(createStackTraceLogEntry(throwable));
            throwableLogEntry = new String[throwableLogEntryList.size()];
            throwableLogEntry = throwableLogEntryList.toArray(throwableLogEntry);
        }
        return throwableLogEntry;
    }
    
    /**
     * Creates a multi-line log entry containing the full stack trace of the given throwable and its cause(s).
     * 
     * @param throwable the throwable for which the full stack trace should be converted to a list of strings
     * @return a list of strings representing a multi-line log entry of the stack trace of the given throwable; never
     *         <code>null</code>, but may be <i>empty</i>, if the given throwable is <code>null</code>
     */
    private List<String> createStackTraceLogEntry(Throwable throwable) {
        List<String> stackTraceLogEntryList = new ArrayList<String>();
        if (throwable != null) {            
            StackTraceElement[] stackTraceElements = throwable.getStackTrace();
            if (stackTraceElements != null && stackTraceElements.length > 0) {
                for (int i = 0; i < stackTraceElements.length; i++) {
                    stackTraceLogEntryList.add("  at " + stackTraceElements[i].toString());
                }
            }
            // Recursively create stack trace entries for the cause of the given throwable
            Throwable cause = throwable.getCause();
            if (cause != null) {
                stackTraceLogEntryList.add("Caused by: " + cause.toString());                
                stackTraceLogEntryList.addAll(createStackTraceLogEntry(cause));
            }
        }
        return stackTraceLogEntryList;
    }
    
    /**
     * Builds the final log entry based on the given parameters and writes it to the individual output streams depending
     * on their specific log level. Each string in the given <code>logLines</code> parameter will be written in a
     * separate line forming a multi-line log entry (an entry with a single timestamp, message type, and id, but
     * spanning multiple lines).
     * 
     * @param messageType the type of this log entry; must not be <code>null</code>
     * @param timestamp the point in time (created via {@link #createTimestamp()} at which the log entry was received;
     *        must not be <code>null</code>
     * @param id the identifier of the source of the log entry to write; must not be <code>null</code>
     * @param logLines the (set of) string(s) to log; must not be <code>null</code>
     */
    private void log(MessageType messageType, String timestamp, String id, String... logLines) {
        // Create the final log entry
        logEntryBuilder.append("[");
        logEntryBuilder.append(timestamp);
        logEntryBuilder.append("] ");
        logEntryBuilder.append(messageType);
        logEntryBuilder.append(" [");
        logEntryBuilder.append(id);
        logEntryBuilder.append("] ");
        logEntryIndentation = logEntryBuilder.length();
        if (isBlank(logLines)) {
            logEntryBuilder.append(NO_MESSAGE_RECEIVED);
        } else {
            logEntryBuilder.append(logLines[0]);
            for (int i = 1; i < logLines.length; i++) {
                logEntryBuilder.append(LINE_SEPARATOR);
                // For multi-line entries, add whitespaces to each additional line to keep all lines align 
                for (int j = 0; j < logEntryIndentation; j++) {
                    logEntryBuilder.append(" ");
                }
                logEntryBuilder.append(logLines[i]);
            }
        }
        // Write the final log entry to each output stream depending on its specific log level
        if (messageType == MessageType.DEBUG) {
            write(logEntryBuilder.toString(), debugOutputStreamsList);
        } else {
            write(logEntryBuilder.toString(), allOutputStreamsList);
        }
        // As there is only a single, global instance of the builder, reset its content after writing 
        logEntryBuilder.setLength(0);
    }
    
    /**
     * Writes the given log entry to each output stream of the given list.
     * 
     * @param logEntry the log entry to write
     * @param targetOutputStreamsList the list of output stream to which the log entry will be written
     */
    private void write(String logEntry, List<OutputStream> targetOutputStreamsList) {
        for (OutputStream outputStream : targetOutputStreamsList) {
            try {
                outputStream.write(logEntry.getBytes());
                outputStream.write(END_OF_LOG_ENTRY);
                outputStream.flush();
            } catch (IOException e) {
                // Use System.err as fallback stream for now
                System.err.println("[" + createTimestamp() + "] [E] [Logger] Writing log entry to output stream " 
                        + outputStream + " failed");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks the given set of strings for being blank. This is the case, if the given set is <code>null</code>, does
     * not contain any elements, or all elements in that set are either <code>null</code> or <i>blank</i>.
     *  
     * @param logLines the set of strings to check
     * @return <code>true</code>, if the given set of strings is blank; <code>false</code> otherwise
     */
    private boolean isBlank(String[] logLines) {
        boolean isBlank = true;
        if (logLines != null && logLines.length > 0) {
            int logLinesCounter = 0;
            String logLine;
            while (isBlank && logLinesCounter < logLines.length) {
                logLine = logLines[logLinesCounter];
                if (logLine != null) {
                    isBlank = logLine.isBlank();
                }
                logLinesCounter++;
            }
        }
        return isBlank;
    }
    
    /**
     * Creates a current timestamp in the {@link #TIMESTAMP_FORMAT} and returns it as a string.
     * 
     * @return the current timestamp; never <code>null</code>
     */
    private String createTimestamp() {
        return TIMESTAMP_FORMAT.format(new Date());
    }
    
    /**
     * Adds the given output stream to the internal list of output streams this instance is aware of. The additional
     * log level defines which types of log entries will be written to the given output stream. 
     *  
     * @param outputStream the output stream to add
     * @param logLevel the log level defining the types of log entries, which will be written to the given output stream
     * @return <code>true</code>, if adding the given output stream is successful; <code>false</code> otherwise, which
     *         is the case, if the given output stream or the given log level is <code>null</code> or the internal list
     *         of output streams contains the given instance already
     */
    public boolean addOutputStream(OutputStream outputStream, LogLevel logLevel) {
        boolean additionSuccessful = false;
        if (outputStream != null && logLevel != null) {
            additionSuccessful = addOutputStream(outputStream, allOutputStreamsList);
            if (logLevel == LogLevel.DEBUG) {
                additionSuccessful = addOutputStream(outputStream, debugOutputStreamsList);
            }
        }
        return additionSuccessful;
    }
    
    /**
     * Adds the given output stream to the given list of output streams.
     * 
     * @param outputStream outputStream the output stream to add; must not be <code>null</code>
     * @param targetOutputStreamList the list of output streams to which the given output stream should be added; must
     *        not be <code>null</code>
     * @return <code>true</code>, if adding the given output stream is successful; <code>false</code> otherwise, which
     *         is the case, if the given list of output streams contains the given instance already
     */
    private boolean addOutputStream(OutputStream outputStream, List<OutputStream> targetOutputStreamList) {
        boolean additionSuccessful = false;
        if (!targetOutputStreamList.contains(outputStream)) {
            additionSuccessful = targetOutputStreamList.add(outputStream);
        }
        return additionSuccessful;
    }
    
    /**
     * Removes the given output stream from the internal list of output streams this instance is aware of. This action
     * does not perform any further actions, like closing the given stream. These actions are up to the caller of
     * this method.
     *  
     * @param outputStream the output stream to remove
     * @return <code>true</code>, if removing the given output stream is successful; <code>false</code> otherwise, which
     *         is the case, if the given output stream is <code>null</code> or the internal list of output streams does
     *         not contain the given instance (nothing to remove)
     */
    public boolean removeOutputStream(OutputStream outputStream) {
        boolean removalSuccessful = false;
        if (outputStream != null) {
            // All output stream must be in allOutputStreamsList (independent of debug streams or not)
            removalSuccessful = allOutputStreamsList.remove(outputStream);
            // If the previous removal fails, there is no such output stream (return false)
            if (removalSuccessful) {
                /*
                 * If the previous removal was successful, it could be a debug stream:
                 * Hence, either it is a debug stream, which results in returning true already after calling the remove
                 * method below, or it is not a debug stream, which then relies on the result on the previous removal
                 * above.
                 */
                removalSuccessful = debugOutputStreamsList.remove(outputStream) || removalSuccessful;
            }
        }
        return removalSuccessful;
    }
    
}
