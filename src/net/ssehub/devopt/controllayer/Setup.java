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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;

/*
 * TODO configuration properties to realize:
 * 
 * Logger: additional output streams and their log level
 * 
 * Update:
 *  Network connection (server/subscriber) for incoming update notifications
 *  Network connection (server/subscriber) for incoming update data
 *  Network connection (client/publisher) for outgoing update notifications
 *  Network connection (client/publisher) for outgoing update data
 *  [decided at runtime via respective element model?] Network connection (client/publisher) for outgoing adaptation
 * 
 * Data:
 *  Network connection for data caching with external data storage
 * 
 */


/**
 * This class realizes the internal setup of this tool in terms of configuration properties and their current values. It
 * determines the configuration file defined by the supplied command-line arguments, sets the user-defined property
 * values based on the configuration file content, validates these values, and provides them for other components of the
 * tool. If no configuration file is given or user-defined property values are not supported, the setup automatically
 * uses its default values for the respective properties.
 * 
 * @author kroeher
 *
 */
public class Setup {
    
    // [Start]---------------------------------------------------------------------------------------------------[Start]
    // [Start]                                 Constant Property Keys Definition                                 [Start]
    // [Start]---------------------------------------------------------------------------------------------------[Start]
    // checkstyle: stop declaration order check (public before private; following order eases filtering properties)
    
    /**
     * The prefix of all keys identifying a configuration property related to logging capabilities. 
     */
    private static final String KEY_LOGGING_PREFIX = "logging.";
    
    /**
     * The key identifying the configuration property for defining the type of stream to use for logging standard
     * information. The associated value to this key in this setup is either a valid user-defined stream or the
     * {@link #LOGGING_DEFAULT_VALUE}.
     */
    public static final String KEY_LOGGING_STANDARD = KEY_LOGGING_PREFIX + "standard";
    
    /**
     * The key identifying the configuration property for defining the type of stream to use for logging debug
     * information. The associated value to this key in this setup is either a valid user-defined stream or the
     * {@link #LOGGING_DEFAULT_VALUE}.
     */
    public static final String KEY_LOGGING_DEBUG = KEY_LOGGING_PREFIX + "debug";
  
    /**
     * The prefix of all keys identifying a configuration property related to registration capabilities. 
     */
    private static final String KEY_REGISTRATION_PREFIX = "registration.";
    
    /**
     * The key identifying the configuration property for defining the type of protocol to use for listening for
     * registrations of local elements to supervise by the controller. The associated value to this key in this setup is
     * either a valid user-defined protocol or the {@link #REGISTRATION_DEFAULT_PROTOCOL}.
     */
    public static final String KEY_REGISTRATION_PROTOCOL = KEY_REGISTRATION_PREFIX + "protocol";
    
    /**
     * The key identifying the configuration property for defining the URL to use for establishing the network
     * connection for registration of local elements. The associated value to this key in this setup is either a valid
     * user-defined URL or the {@link #REGISTRATION_DEFAULT_URL}.
     */
    public static final String KEY_REGISTRATION_URL = KEY_REGISTRATION_PREFIX + "url";
    
    /**
     * The key identifying the configuration property for defining the port number to use for establishing the network
     * connection for registration of local elements. The associated value to this key in this setup is either a valid
     * user-defined port or the {@link #REGISTRATION_DEFAULT_PORT}.
     */
    public static final String KEY_REGISTRATION_PORT = KEY_REGISTRATION_PREFIX + "port";
    
    /**
     * The key identifying the configuration property for defining the channel name to use for establishing the network
     * connection for registration of local elements. The associated value to this key in this setup is either a valid
     * user-defined channel or the {@link #REGISTRATION_DEFAULT_CHANNEL}.
     */
    public static final String KEY_REGISTRATION_CHANNEL = KEY_REGISTRATION_PREFIX + "channel";
    
    /**
     * The prefix of all keys identifying a configuration property related to model capabilities. 
     */
    private static final String KEY_MODEL_PREFIX = "model.";
    
    /**
     * The key identifying the configuration property for defining the fully-qualified path to the directory for saving
     * models (IVML files) of registered local elements. The associated value to this key in this setup is either a
     * valid user-defined path or the {@link #MODEL_DEFAULT_DIRECTORY}. The validation of this property by this setup
     * guarantees that the directory always exists. 
     */
    public static final String KEY_MODEL_DIRECTORY = KEY_MODEL_PREFIX + "directory";
    
    /**
     * The prefix of all keys identifying a configuration property related to aggregation capabilities. 
     */
    private static final String KEY_AGGREGATION_PREFIX = "aggregation.";
    
    /**
     * The key identifying the configuration property for defining the type of protocol to use for sending aggregated
     * runtime data from the local elements to a desired receiver (database, dashboard, higher-level element, etc.). The
     * associated value to this key in this setup is either a valid user-defined protocol or <code>null</code>, if
     * aggregated data must not be send.
     */
    public static final String KEY_AGGREGATION_PROTOCOL = KEY_AGGREGATION_PREFIX + "protocol";
    
    /**
     * The key identifying the configuration property for defining the URL to use for establishing the network
     * connection for sending aggregated runtime data. The associated value to this key in this setup is either a valid
     * user-defined protocol or <code>null</code>, if aggregated data must not be send.
     */
    public static final String KEY_AGGREGATION_URL = KEY_AGGREGATION_PREFIX + "url";
    
    /**
     * The key identifying the configuration property for defining the port number to use for establishing the network
     * connection for sending aggregated runtime data. The associated value to this key in this setup is either a valid
     * user-defined port number or <code>null</code>, if aggregated data must not be send.
     */
    public static final String KEY_AGGREGATION_PORT = KEY_AGGREGATION_PREFIX + "port";
    
    /**
     * The key identifying the configuration property for defining the channel name to use for establishing the network
     * connection for sending aggregated runtime data. The associated value to this key in this setup is either a valid
     * user-defined protocol or <code>null</code>, if aggregated data must not be send.
     */
    public static final String KEY_AGGREGATION_CHANNEL = KEY_AGGREGATION_PREFIX + "channel";
    
    // checkstyle: resume declaration order check (public before private; following order eases filtering properties)
    // [End]-------------------------------------------------------------------------------------------------------[End]
    // [End]                                   Constant Property Keys Definition                                   [End]
    // [End]-------------------------------------------------------------------------------------------------------[End]
    
    // [Start]---------------------------------------------------------------------------------------------------[Start]
    // [Start]                                 Constant Default Values Definition                                [Start]
    // [Start]---------------------------------------------------------------------------------------------------[Start]
    
    /**
     * The default value for the configuration property identified by {@link #KEY_LOGGING_STANDARD}.
     */
    private static final String LOGGING_STANDARD_DEFAULT_VALUE = "s";
    
    /**
     * The default value for the configuration property identified by {@link #KEY_LOGGING_DEBUG}.
     */
    private static final String LOGGING_DEBUG_DEFAULT_VALUE = "n";
    
    /**
     * The default value for the configuration property identified by {@link #KEY_REGISTRATION_PROTOCOL}.
     */
    private static final String REGISTRATION_DEFAULT_PROTOCOL = "HTTP";
    
    /**
     * The default value for the configuration property identified by {@link #KEY_REGISTRATION_URL}.
     */
    private static final String REGISTRATION_DEFAULT_URL = "127.0.0.1";
    
    /**
     * The default value for the configuration property identified by {@link #KEY_REGISTRATION_PORT}.
     */
    private static final String REGISTRATION_DEFAULT_PORT = "80";
    
    /**
     * The default value for the configuration property identified by {@link #KEY_REGISTRATION_CHANNEL}, if the value
     * of the configuration property identified by {@link #KEY_REGISTRATION_PROTOCOL} is <code>MQTT</code>.
     */
    private static final String REGISTRATION_DEFAULT_MQTT_CHANNEL = "registration";
    
    /**
     * The default value for the configuration property identified by {@link #KEY_REGISTRATION_CHANNEL} if the value
     * of the configuration property identified by {@link #KEY_REGISTRATION_PROTOCOL} is <code>HTTP</code>.
     */
    private static final String REGISTRATION_DEFAULT_HTTP_CHANNEL = "/" + REGISTRATION_DEFAULT_MQTT_CHANNEL;
    
    /**
     * The default value for the configuration property identified by {@link #KEY_MODEL_DIRECTORY}.
     */
    private static final String MODEL_DEFAULT_DIRECTORY = "./models";
    
    // [End]-------------------------------------------------------------------------------------------------------[End]
    // [End]                                   Constant Default Values Definition                                  [End]
    // [End]-------------------------------------------------------------------------------------------------------[End]
    
    private Properties loggingProperties;
    private Properties registrationProperties;
    private Properties modelProperties;
    private Properties aggregationProperties;
    
    private List<String> postponedWarnings;
    
    /**
     * Constructs a new {@link Setup} instance providing the configuration properties and their current values for other
     * components of this tool.
     * 
     * @param configurationFilePath the path to the configuration file containing the configuration properties for this
     *        setup instance; may be <code>null</code> to force using the default property values
     * @throws SetupException if determining the configuration file, setting the configuration properties, or validating
     *         their values fails
     */
    public Setup(String configurationFilePath) throws SetupException {
        loggingProperties = new Properties();
        registrationProperties = new Properties();
        modelProperties = new Properties();
        aggregationProperties = new Properties();
        postponedWarnings = new ArrayList<String>();
        File configurationFile = getConfigurationFile(configurationFilePath);
        if (configurationFile != null) {
            setProperties(configurationFile);
        }
        /*
         * Validation sets default property values (for mandatory properties) either, if property values of the given
         * configuration file are not supported, or, if no property value is set. Hence, validation automatically
         * ensures default property values also, if no configuration file is available and setting the properties was
         * not called above.
         */
        validateProperties();
    }
    
    /**
     * Returns the configuration file denoted by the given configuration file path.
     * 
     * @param configurationFilePath the path to the configuration file containing the configuration properties
     * @return the configuration file to use for building a setup instance or <code>null</code>, if the given
     *         configuration file path is <code>null</code> or <i>blank</i>
     * @throws SetupException if the given configuration file path neither <code>null</code> nor blank, but the path
     *         causes the configuration file creation to fail
     */
    private File getConfigurationFile(String configurationFilePath) throws SetupException {
        File configurationFile = null;
        if (configurationFilePath != null && !configurationFilePath.isBlank()) {
            try {
                configurationFile = FileUtilities.INSTANCE.getCheckedFileObject(configurationFilePath, false);
            } catch (FileUtilitiesException e) {
                throw new SetupException("Invalid configuration file path: \"" + configurationFilePath + "\"", e);
            }
        }
        return configurationFile;
    }
    
    /**
     * Reads the configuration properties defined in the given configuration file and calls
     * {@link #setProperties(Properties)} passing the read properties set.
     * 
     * @param configurationFile the configuration file containing the configuration properties for this setup instance;
     *        must not be <code>null</code> 
     * @throws SetupException if reading the configuration properties from the given file fails or no properties are
     *         available
     */
    private void setProperties(File configurationFile) throws SetupException {
        InputStream propertiesInputStream = null;
        try {
            propertiesInputStream = new FileInputStream(configurationFile);
            Properties loadedProperties = new Properties();
            loadedProperties.load(propertiesInputStream);
            setProperties(loadedProperties);
        } catch (IOException e) {
            throw new SetupException("Setting configuration properties failed", e);
        } finally {
            if (propertiesInputStream != null) {
                try {
                    propertiesInputStream.close();
                } catch (IOException e) {
                    throw new SetupException("Closing file input stream for \"" + configurationFile.getAbsolutePath()
                            + "\" failed", e);
                }
            }
        }
    }
    
    /**
     * Adds the given properties based on their prefix to one of the internal property sets of this setup instance. If
     * a property has an unknown prefix, a corresponding message about ignoring that property is added to the
     * {@link #postponedWarnings}.
     *  
     * @param loadedProperties the properties to set as configuration properties of this setup instance
     * @throws SetupException if the given properties are empty
     */
    private void setProperties(Properties loadedProperties) throws SetupException {
        if (loadedProperties.isEmpty()) {
            throw new SetupException("Loaded properties are empty");
        }
        Enumeration<Object> propertyKeys = loadedProperties.keys();
        String propertyKey;
        String propertyValue;
        while (propertyKeys.hasMoreElements()) {
            propertyKey = (String) propertyKeys.nextElement();
            propertyValue = loadedProperties.getProperty(propertyKey);
            if (propertyKey.startsWith(KEY_LOGGING_PREFIX)) {
                loggingProperties.put(propertyKey, propertyValue);
            } else if (propertyKey.startsWith(KEY_REGISTRATION_PREFIX)) {
                registrationProperties.put(propertyKey, propertyValue);
            } else if (propertyKey.startsWith(KEY_MODEL_PREFIX)) {
                modelProperties.put(propertyKey, propertyValue);
            } else if (propertyKey.startsWith(KEY_AGGREGATION_PREFIX)) {
                aggregationProperties.put(propertyKey, propertyValue);
            } else {
                // At this point, the property key has an unknown prefix; hence, it will be ignored
                postponedWarnings.add("Ignoring unknown configuration property \"" + propertyKey + "\"");
            }
        }
    }
    
    /**
     * Calls the individual validation methods for checking whether the property values are valid. 
     * @throws SetupException if a property value is invalid
     */
    private void validateProperties() throws SetupException {
        validateLoggingProperties();
        validateRegistrationProperties();
        validateModelProperties();
        validateAggregationProperties();
    }
    
    /**
     * Validates the {@link #loggingProperties} and their values.
     */
    private void validateLoggingProperties() {
        validateLoggingProperty(KEY_LOGGING_STANDARD, getLoggingConfiguration(KEY_LOGGING_STANDARD),
                LOGGING_STANDARD_DEFAULT_VALUE);
        validateLoggingProperty(KEY_LOGGING_DEBUG, getLoggingConfiguration(KEY_LOGGING_DEBUG),
                LOGGING_DEBUG_DEFAULT_VALUE);
    }
    
    /**
     * Validates a single property of the {@link #loggingProperties} and its value.
     * 
     * @param key the key of the logging property to validate
     * @param value the value associated with the given key
     * @param defaultValue the default value for the property identified by the given key
     */
    private void validateLoggingProperty(String key, String value, String defaultValue) {
        if (!handleUndefinedProperty(key, value, loggingProperties, defaultValue)) {
            // Check if specified logging configuration is supported by setup/logger
            if (!value.equalsIgnoreCase("s") && !value.equalsIgnoreCase("n")) {                
                // More options will follow in future
                postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \"" + key
                        + "\": using default \"" + defaultValue + "\"");
                resetProperty(loggingProperties, key, defaultValue);
            }
        }
    }
    
    /**
     * Validates the {@link #registrationProperties} and their values.
     */
    private void validateRegistrationProperties() {
        // Validate registration protocol
        String key = KEY_REGISTRATION_PROTOCOL;
        String value = getRegistrationConfiguration(key);
        String defaultValue = REGISTRATION_DEFAULT_PROTOCOL;
        if (!handleUndefinedProperty(key, value, registrationProperties, defaultValue)) {
            // Check if specified registration protocol is supported
            if (!value.equalsIgnoreCase("MQTT") && !value.equalsIgnoreCase("HTTP")) {
                postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \"" + key
                        + "\": using default \"" + defaultValue + "\"");
                resetProperty(registrationProperties, key, defaultValue);
            }
        }
        // Validate registration URL
        key = KEY_REGISTRATION_URL;
        value = getRegistrationConfiguration(key);
        defaultValue = REGISTRATION_DEFAULT_URL;
        if (!handleUndefinedProperty(key, value, registrationProperties, defaultValue)) {
            // Check if specified registration URL is supported
            // TODO use regex?
        }
        // Validate registration port
        key = KEY_REGISTRATION_PORT;
        value = getRegistrationConfiguration(key);
        defaultValue = REGISTRATION_DEFAULT_PORT;
        if (!handleUndefinedProperty(key, value, registrationProperties, defaultValue)) {
            // Check if specified registration port is supported
            int intValue = -1;
            try {
                intValue = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                /*
                 * Nothing to do here as in case of this exception, the next check will fail anyway due to the default
                 * intValue of -1.
                 */
            }
            if (intValue < 0 || intValue > 65535) {
                postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \"" + key
                        + "\": using default \"" + defaultValue + "\"");
                resetProperty(registrationProperties, key, defaultValue);
            }
        }
        // Validate registration channel
        key = KEY_REGISTRATION_CHANNEL;
        value = getRegistrationConfiguration(key);
        // Default channel depends on selected protocol
        String registrationProtocol = getRegistrationConfiguration(KEY_REGISTRATION_PROTOCOL);
        if (registrationProtocol.equalsIgnoreCase("HTTP")) {
            defaultValue = REGISTRATION_DEFAULT_HTTP_CHANNEL;
        } else if (registrationProtocol.equalsIgnoreCase("MQTT")) {
            defaultValue = REGISTRATION_DEFAULT_MQTT_CHANNEL;
        }
        if (!handleUndefinedProperty(key, value, registrationProperties, defaultValue)) {
            // Check if specified registration channel is supported
            if (registrationProtocol.equalsIgnoreCase("HTTP") && value.charAt(0) != '/') {
                postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \"" + key
                        + "\": using default \"" + defaultValue + "\"");
                resetProperty(registrationProperties, key, defaultValue);
            }
            // TODO use regex or URI for more elaborated check
        }
    }
    
    /**
     * Validates the {@link #modelProperties} and their values.
     * @throws SetupException if the mandatory model directory could not be created
     */
    private void validateModelProperties() throws SetupException {
        // Validate model directory
        String key = KEY_MODEL_DIRECTORY;
        String value = getModelConfiguration(key);
        String defaultValue = MODEL_DEFAULT_DIRECTORY;
        File modelDirectory;
        if (handleUndefinedProperty(key, value, modelProperties, defaultValue)) {
            // Default value was set for the model directory; validate default directory
            value = getModelConfiguration(key); // reassign the value after setting the default value
            modelDirectory = new File(value);
            try {                
                validateDirectory(modelDirectory);
            } catch (SetupException defaultDirectoryException) {
                throw new SetupException("Default model directory unavailable", defaultDirectoryException);
            }
        } else {
            // User-defined path to the model directory available; validate user-defined directory
            modelDirectory = new File(value);
            try {                
                validateDirectory(modelDirectory);
            } catch (SetupException userDirectoryException) {
                // Using user-defined path failed; use default value and validate default directory
                postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \"" + key
                        + "\": using default \"" + defaultValue + "\"");
                resetProperty(modelProperties, key, defaultValue);
                value = getModelConfiguration(key); // reassign the value after setting the default value
                modelDirectory = new File(value);
                try {                    
                    validateDirectory(modelDirectory);
                } catch (SetupException defaultDirectoryException) {
                    throw new SetupException("Model directory unavailable", defaultDirectoryException);
                }
            }
        }
    }
    
    /**
     * Checks whether the given file denotes a valid directory. If the given file does not exist, this method creates
     * it for checking whether it is a valid directory. In case it is not, the created file will be deleted again.
     * 
     * @param file the file to check for denoting a directory
     * @throws SetupException if the given file does not denote a valid directory or either the creation of the
     *         file or its deletion fails
     */
    private void validateDirectory(File file) throws SetupException {
        if (!file.exists()) {
            try {
                file = FileUtilities.INSTANCE.createDirectory(file.getAbsolutePath());
            } catch (FileUtilitiesException e) {
                /*
                 * As the path used to create the file object above cannot be "null" nor empty and that file did not
                 * exist before that call, the only reason for throwing an exception is that creating the file
                 * failed. Hence, the default model directory is not available, which is a fundamental error.
                 */
                throw new SetupException("Creating directory \"" + file.getAbsolutePath() + "\" failed", e);
            }
            if (!file.isDirectory()) {
                try {
                    FileUtilities.INSTANCE.delete(file);
                } catch (FileUtilitiesException e) {
                    throw new SetupException("Deleting temporal file \"" + file.getAbsolutePath() + "\" failed", e);
                }
                throw new SetupException("Path \"" + file.getAbsolutePath() + "\" does not denote a directory");
            }
        } else {
            if (!file.isDirectory()) {
                throw new SetupException("Path \"" + file.getAbsolutePath() + "\" does not denote a directory");
            }
        }
    }
    
    /**
     * Validates the {@link #aggregationProperties} and their values.
     */
    private void validateAggregationProperties() {
        if (isBlank(aggregationProperties)) {
            // Although being blank, delete all properties completely to avoid inconsistencies on usage
            aggregationProperties.clear();
        } else {
            String[] aggregationPropertiesKeys = {KEY_AGGREGATION_PROTOCOL, KEY_AGGREGATION_URL, KEY_AGGREGATION_PORT,
                KEY_AGGREGATION_CHANNEL};
            boolean aggregationPropertiesValid = true;
            int aggregationPropertiesCounter = 0;
            String key;
            String value;
            while (aggregationPropertiesValid && aggregationPropertiesCounter < aggregationPropertiesKeys.length) {
                key = aggregationPropertiesKeys[aggregationPropertiesCounter];
                value = getAggregationConfiguration(key);
                if (value == null || value.isBlank()) {
                    /*
                     * Check 'isBlank(aggregationProperties)' above must have returned false, hence, there is at least
                     * one property with an actual value. Finding 'null' or 'blank' now means properties are incomplete.
                     */
                    postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \""
                            + key + "\": incomplete aggregation configuration yields no distribution");
                    aggregationPropertiesValid = false;
                } else {                    
                    switch(key) {
                    case KEY_AGGREGATION_PROTOCOL:
                        // Validate protocol for sending aggregated data
                        if (!value.equalsIgnoreCase("MQTT") && !value.equalsIgnoreCase("HTTP")) {
                            postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \""
                                    + key + "\": no distribution of aggregation data");
                            aggregationPropertiesValid = false;
                        }
                        break;
                    case KEY_AGGREGATION_URL:
                        // Validate URL for sending aggregated data
                        // TODO Check if specified aggregation URL is supported; use regex?
                        break;
                    case KEY_AGGREGATION_PORT:
                        // Validate port for sending aggregated data
                        int intValue = -1;
                        try {
                            intValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            // In case of this exception, next check will fail anyway due to default 'intValue' of '-1'
                        }
                        if (intValue < 0 || intValue > 65535) {
                            postponedWarnings.add("Value \"" + value + "\" not supported for configuration property \""
                                    + key + "\": no distribution of aggregation data");
                            aggregationPropertiesValid = false;
                        }
                        break;
                    case KEY_AGGREGATION_CHANNEL:
                        // Validate channel for sending aggregated data
                        // TODO use regex or URI for checks
                        break;
                    default:
                        postponedWarnings.add("Ignoring unknown aggregation configuration property \"" + key + "\"");
                        break;
                    }
                }
                aggregationPropertiesCounter++;
            }
            // In case of invalid properties, delete them completely to avoid inconsistencies on usage
            if (!aggregationPropertiesValid) {
                aggregationProperties.clear();
            }
        }
    }
    
    /**
     * Checks whether the given properties are blank. This is the case, if the given instance is <code>null</code>,
     * <i>empty</i>, or all keys map to <code>null</code> or a blank string. Hence, if the given instance contains at
     * least one key, which maps to a non-<code>null</code> and non-blank value, the properties are not blank.
     *  
     * @param properties the properties to check for being blank
     * @return <code>true</code>, if the given properties are blank; <code>false</code> otherwise
     */
    private boolean isBlank(Properties properties) {
        boolean isBlank = true;
        if (properties != null && !properties.isEmpty()) {
            Enumeration<Object> propertyKeys = properties.keys();
            String propertyValue;
            while (isBlank && propertyKeys.hasMoreElements()) {
                propertyValue = (String) properties.get(propertyKeys.nextElement());
                if (propertyValue != null && !propertyValue.isBlank()) {
                    isBlank = false;
                }
            }
        }
        return isBlank;
    }
    
    /**
     * Checks whether the given value is <code>null</code> or blank. If this is the case, this method adds the given
     * default value for the given key to the given property set.
     * 
     * @param key the key of the property to check
     * @param value the value of the property to check
     * @param properties the property set to add the given default value for the given key to, if the given value is
     *        <code>null</code> or blank
     * @param defaultValue the default value for the given key, which will be added for that key to the given property
     *        set, if the given value is <code>null</code> or blank
     * @return <code>true</code>, if this method added the given default value for the given key; <code>false</code>
     *         otherwise, which means that no actions were performed
     */
    private boolean handleUndefinedProperty(String key, String value, Properties properties, String defaultValue) {
        boolean defaultValueAdded = false;
        if (value == null || value.isBlank()) {
            postponedWarnings.add("Configuration property \"" + key + "\" not defined: using default value \""
                    + defaultValue + "\"");
            resetProperty(properties, key, defaultValue);
            defaultValueAdded = true;
        }
        return defaultValueAdded;
    }
    
    /**
     * Removes the given key and its current value from the given property set and adds the given key and the given
     * value to that set again.
     * 
     * @param properties the property set to reset with the given key and value
     * @param key the key identifying the property to reset in the given property set
     * @param value the new value to add to the given property set using the given key
     */
    private void resetProperty(Properties properties, String key, String value) {
        properties.remove(key);
        properties.put(key, value);
    }
    
    /**
     * Returns whether postponed setup warnings are available (<code>true</code>) or not (<code>false</code>). A
     * postponed setup warning may be created, if a property in a given configuration file has an unknown prefix and,
     * hence, will be ignored by this setup instance.
     * 
     * @return <code>true</code>, if at least one postponed setup warning is available; <code>false</code> otherwise
     */
    public boolean hasPostponedWarnings() {
        return postponedWarnings.size() != 0;
    }
    
    /**
     * Returns the set of postponed setup warnings created, if a property in a given configuration file has an unknown
     * prefix and, hence, will be ignored by this setup instance.
     * 
     * @return the set of postponed setup warnings; never <code>null</code>, but may be empty
     * @see #hasPostponedWarnings()
     */
    public String[] getPostponedWarnings() {
        return postponedWarnings.toArray(new String[0]);
    }
    
    /**
     * Returns the setup value for the logging configuration property identified by the given key.
     * 
     * @param key the key of the logging configuration property for which the loaded setup value should be returned
     * @return the respective setup value or <code>null</code>, if the given key is not present in the logging property
     *         set
     */
    public String getLoggingConfiguration(String key) {
        return getConfiguration(loggingProperties, key);
    }
    
    /**
     * Returns the setup value for the registration configuration property identified by the given key.
     * 
     * @param key the key of the registration configuration property for which the loaded setup value should be returned
     * @return the respective setup value or <code>null</code>, if the given key is not present in the registration
     *         property set
     */
    public String getRegistrationConfiguration(String key) {
        return getConfiguration(registrationProperties, key);
    }
    
    /**
     * Returns the setup value for the model configuration property identified by the given key.
     * 
     * @param key the key of the model configuration property for which the loaded setup value should be returned
     * @return the respective setup value or <code>null</code>, if the given key is not present in the model property
     *         set
     */
    public String getModelConfiguration(String key) {
        return getConfiguration(modelProperties, key);
    }
    
    /**
     * Checks whether aggregation configuration properties are available.
     * 
     * @return <code>true</code>, if all aggregation configuration properties are available; <code>false</code>
     *         otherwise
     */
    public boolean hasAggregationConfiguration() {
        return !aggregationProperties.isEmpty();
    }
    
    /**
     * Returns the setup value for the aggregation configuration property identified by the given key.
     * 
     * @param key the key of the aggregation configuration property for which the loaded setup value should be returned
     * @return the respective setup value or <code>null</code>, if the given key is not present in the aggregation
     *         property set
     * @see #hasAggregationConfiguration()
     */
    public String getAggregationConfiguration(String key) {
        return getConfiguration(aggregationProperties, key);
    }
    
    /**
     * Returns the setup value for the configuration property in the given property set identified by the given key.
     *
     * @param properties the property set to search in for the given key
     * @param key the key of the configuration property for which the loaded setup value should be returned
     * @return the setup value for the configuration property identified by the given key or <code>null</code>, if the
     *         given key is not present in the given property set
     */
    private String getConfiguration(Properties properties, String key) {
        String configuration = null;
        if (properties.containsKey(key)) {
            configuration = properties.getProperty(key);
        }
        return configuration;
    }
    
    /**
     * Returns a concatenation of the multi-line, textual representation provided by {@link #toLogLines()}.
     * 
     * @return a textual representation of this setup instance
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        String[] stringLines = toLogLines();
        for (int i = 0; i < stringLines.length; i++) {
            stringBuilder.append(stringLines[i]);
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }
    
    /**
     * Returns a multi-line, textual representation of this setup instance for logging. Each element of the returned set
     * represents one property key with its associated value.
     * 
     * @return a textual representation of this setup instance for logging
     */
    public String[] toLogLines() {
        int logLinesLength = loggingProperties.size() + registrationProperties.size() + modelProperties.size()
                + aggregationProperties.size();
        String[] logLines = new String[logLinesLength];
        
        int logLinesOffset = 0;
        logLinesOffset = addLogLines(logLines, logLinesOffset, loggingProperties);
        logLinesOffset = addLogLines(logLines, logLinesOffset, registrationProperties);
        logLinesOffset = addLogLines(logLines, logLinesOffset, modelProperties);
        addLogLines(logLines, logLinesOffset, aggregationProperties);

        return logLines;
    }
    
    /**
     * Adds each element in the given property set as key-value-pair to the given string set. The given offset marks the
     * start index of the target string set for adding the key-value-pairs.
     * 
     * @param logLines the string set to add the elements of the given property set to
     * @param offset the start index of the target string set for adding the property elements
     * @param properties the property set containing the elements to add as key-value-pairs to the given string set
     * @return the index of the target string set after the last added property 
     */
    private int addLogLines(String[] logLines, int offset, Properties properties) {
        Enumeration<Object> propertyKeys = properties.keys();
        String propertyKey;
        String propertyValue;
        int logLineCounter = offset;
        while (propertyKeys.hasMoreElements()) {
            propertyKey = (String) propertyKeys.nextElement();
            propertyValue = properties.getProperty(propertyKey);
            logLines[logLineCounter] = propertyKey + " = " + propertyValue;
            logLineCounter++;
        }
        return logLineCounter;
    }
    
}
