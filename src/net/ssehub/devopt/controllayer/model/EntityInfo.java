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

import net.ssehub.devopt.controllayer.utilities.ModelUtilities;
import net.ssehub.devopt.controllayer.utilities.StringUtilities;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;

/**
 * This class represents a complex data structure to consistently provide all information of an entity (under
 * observation and control) to other components of this controller.
 * 
 * @author kroeher
 *
 */
public class EntityInfo {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = EntityInfo.class.getSimpleName();
    
    /**
     * The local reference to the global {@link ModelUtilities}.
     */
    private ModelUtilities modelUtilities = ModelUtilities.INSTANCE;
    
    /**
     * The path to the source file of the {@link Configuration} used to create this instance.
     */
    private String sourceFilePath;

    /**
     * The identifier of the entity as defined by its IVML configuration.
     */
    private String observableIdentifier;
    
    /**
     * The host (URL) of the entity as defined by its IVML configuration.
     */
    private String observableHost;
    
    /**
     * The port number of the entity as defined by its IVML configuration.
     */
    private int observablePort;
    
    /**
     * The URL of the entity's monitoring scope as defined by its IVML configuration. This monitoring scope defines the
     * target (MQTT broker, HTTP server, etc.) the entity sends its monitoring data to.
     */
    private String monitoringUrl;
    
    /**
     * The port number of the entity's monitoring scope as defined by its IVML configuration. This monitoring scope
     * defines the target (MQTT broker, HTTP server, etc.) the entity sends its monitoring data to.
     */
    private int monitoringPort;
    
    /**
     * The channel (MQTT topic name or HTTP server context name) of the entity's monitoring scope as defined by its IVML
     * configuration. This monitoring scope defines the target (MQTT broker, HTTP server, etc.) the entity sends its
     * monitoring data to.
     */
    private String monitoringChannel;
    
    /**
     * Constructs a new {@link EntityInfo} instance containing the information of the entity defined by the given
     * configuration and the given string denoting the path to the source file of that configuration.
     * 
     * @param configuration the IVML model configuration defining the entity for which this instance should be created;
     *        the caller of this constructor must ensure that the configuration is not <code>null</code>
     * @param sourceFilePath the path to the source file of the given configuration
     * @throws ModelException if entity information is not defined by the given configuration or retrieving that
     *         information from the given configuration fails 
     */
    public EntityInfo(Configuration configuration, String sourceFilePath) throws ModelException {
        this.sourceFilePath = sourceFilePath;
        setAttributeValues(configuration);
    }
    
    /**
     * Constructs a new {@link EntityInfo} instance containing the information of the entity defined by the given
     * configuration.
     * 
     * @param configuration the IVML model configuration defining the entity for which this instance should be created;
     *        the caller of this constructor must ensure that the configuration is not <code>null</code>
     * @throws ModelException if entity information is not defined by the given configuration or retrieving that
     *         information from the given configuration fails 
     */
    public EntityInfo(Configuration configuration) throws ModelException {
        setAttributeValues(configuration);
    }
    
    /**
     * Sets the attribute values of this instance by retrieving the actual entity definition from the given
     * configuration and calling the specific set-methods of this class using that entity.
     * 
     * @param configuration the IVML model configuration from which the entity definition should be retrieved; the 
     *        caller of this method must ensure that the configuration is not <code>null</code>
     * @throws ModelException if entity information is not defined by the given configuration or retrieving that
     *         information from the given configuration fails 
     */
    private void setAttributeValues(Configuration configuration) throws ModelException {
        IDecisionVariable entity = modelUtilities.getEntity(configuration);
        if (entity != null) {
            setIdentificationValues(entity);
            setMonitoringValues(entity);
        } else {
            throw new ModelException("No entity defined in configuration");
        }
    }
    
    /**
     * Sets the attribute values defining the entity identification.
     * 
     * @param entity the IVML model element denoting the entity for which its identification values of this instance
     *        should be set; the caller of this method must ensure that the entity is not <code>null</code>
     * @throws ModelException if entity identification information is not defined for the given element or retrieving
     *         that information form the given element fails
     */
    private void setIdentificationValues(IDecisionVariable entity) throws ModelException {
        observableIdentifier = modelUtilities.getEntityIdentificationIdentifier(entity);
        if (observableIdentifier == null || observableIdentifier.isBlank()) {
            throw new ModelException("No identifier defined for entity");
        }
        observableHost = modelUtilities.getEntityIdentificationHost(entity);
        if (observableHost == null || observableHost.isBlank()) {
            throw new ModelException("No host defined for entity");
        }
        observablePort = modelUtilities.getEntityIdentificationPort(entity);
        if (observablePort < 0 || observablePort > 65535) {
            throw new ModelException("No (valid) port defined for entity: " + observablePort);
        }
    }
    
    /**
     * Sets the attribute values defining the entity's monitoring scope. This monitoring scope defines the target (MQTT
     * broker, HTTP server, etc.) the entity sends its monitoring data to.
     * 
     * @param entity the IVML model element denoting the entity for which its monitoring values of this instance
     *        should be set; the caller of this method must ensure that the entity is not <code>null</code>
     * @throws ModelException if entity monitoring information is not defined for the given element or retrieving that
     *         information form the given element fails
     */
    private void setMonitoringValues(IDecisionVariable entity) throws ModelException {
        String monitoringScope = modelUtilities.getEntityRuntimeDateMonitoringScope(entity);
        if (monitoringScope != null) {
            /*
             * Split monitoring scope string into its parts, like:
             * 
             * Monitoring scope string = "entity/monitoring/mqtt/topic@127.0.0.1:8883"
             *                           or "devopttestmonitoring@tcp://broker.hivemq.com:1883"
             * Observable info URL     = "127.0.0.1"
             *                           or "tcp://broker.hivemq.com"
             * Observable info port    = 8883
             *                           or 1883
             * Observable info channel = "entity/monitoring/mqtt/topic"
             *                           or "devopttestmonitoring"
             */
            int indexOfAt = monitoringScope.indexOf('@');
            int indexOfLastColon = monitoringScope.lastIndexOf(':');
            try {
                monitoringUrl = monitoringScope.substring(indexOfAt + 1, indexOfLastColon);
                if (monitoringUrl.isBlank()) {
                    throw new ModelException("No URL defined for monitoring scope");
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ModelException("Invalid monitoring scope definition: \"" + monitoringScope + "\"", e);
            }
            try {                
                monitoringPort = Integer.valueOf(monitoringScope.substring(indexOfLastColon + 1));
                if (monitoringPort < 0 || monitoringPort > 65535) {
                    throw new ModelException("No (valid) port defined for monitoring scope: " + monitoringPort);
                }
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new ModelException("Invalid monitoring scope definition: \"" + monitoringScope + "\"", e);
            }
            try {                
                monitoringChannel = monitoringScope.substring(0, indexOfAt);
                if (monitoringChannel.isBlank()) {
                    throw new ModelException("No channel defined for monitoring scope");
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ModelException("Invalid monitoring scope definition: \"" + monitoringScope + "\"", e);
            }
        } else {
            throw new ModelException("No monitoring scope defined for entity");
        }
    }
    
    /**
     * Returns string denoting the path to the source file of the {@link Configuration} used to create this instance.
     * 
     * @return the path to the source file or <code>null</code>, if no source file is known for this instance
     */
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    
    /**
     * Returns the identifier of the entity represented by this instance. The identifier is defined in the IVML
     * configuration for that entity.
     * 
     * @return the identifier of the entity; never <code>null</code> nor <i>blank</i>
     */
    public String getIdentifier() {
        return observableIdentifier;
    }
    
    /**
     * Returns the host (URL) of the entity represented by this instance. The host is defined in the IVML configuration
     * for that entity.
     * 
     * @return the host of the entity; never <code>null</code> nor <i>blank</i>
     */
    public String getHost() {
        return observableHost;
    }
    
    /**
     * Returns the port number of the entity represented by this instance. The port number is defined in the IVML
     * configuration for that entity.
     * 
     * @return the port number of the entity; always between 0 and 65535 (including)
     */
    public int getPort() {
        return observablePort;
    }
    
    /**
     * Returns the URL of the monitoring scope of the entity represented by this instance. This monitoring scope defines
     * the target (MQTT broker, HTTP server, etc.) that entity sends its monitoring data to. It is defined in the IVML
     * configuration for that entity.
     * 
     * @return the URL of the monitoring scope of the entity; never <code>null</code> nor <i>blank</i>
     */
    public String getMonitoringUrl() {
        return monitoringUrl;
    }
    
    /**
     * Returns the port number of the monitoring scope of the entity represented by this instance. This monitoring
     * scope defines the target (MQTT broker, HTTP server, etc.) that entity sends its monitoring data to. It is defined
     * in the IVML configuration for that entity.
     * 
     * @return the port number of the monitoring scope of the entity; always between 0 and 65535 (including)
     */
    public int getMonitoringPort() {
        return monitoringPort;
    }

    /**
     * Returns the channel (MQTT topic name or HTTP server context name) of the monitoring scope of the entity
     * represented by this instance. This monitoring scope defines the target (MQTT broker, HTTP server, etc.) that
     * entity sends its monitoring data to. It is defined in the IVML configuration for that entity.
     * 
     * @return the channel of the monitoring scope of the entity; never <code>null</code> nor <i>blank</i>
     */
    public String getMonitoringChannel() {
        return monitoringChannel;
    }
    
    /**
     * Checks whether the given {@link EntityInfo} instance is equal to this one. Two instances are equal, if their
     * string representation provided by {@link #toString()} is equal.
     * 
     * @param other the other instance to compare with this one for equality
     * @return <code>true</code>, if the given instance is equal to this one; <code>false</code> otherwise
     */
    public boolean equals(EntityInfo other) {
        boolean isEqual = false;
        if (other != null) {
            isEqual = this.toString().equals(other.toString());
        }
        return isEqual;
    }
    
    /**
     * Returns a custom string representation of this instance. This representation contains:
     * <ul>
     * <li>The identifier of this class</li>
     * <li>The identifier of the entity represented by this instance</li>
     * <li>The host (URL) of the entity represented by this instance</li>
     * <li>The port number of the entity represented by this instance</li>
     * <li>The URL of the monitoring scope  of the entity represented by this instance</li>
     * <li>The port number of the monitoring scope of the entity represented by this instance</li>
     * <li>The channel (MQTT topic name or HTTP server context name) of the monitoring scope  of the entity represented
     *     by this instance</li>
     * </ul>
     * 
     * @return the custom string representation of this instance
     */
    @Override
    public String toString() {
        String[][] observableInfo = {
            {ID},
            {"id", getIdentifier()},
            {"host", getHost()},
            {"port", "" + getPort()},
            {"monitoringUrl", getMonitoringUrl()},
            {"monitoringPort", "" + getMonitoringPort()},
            {"monitoringChannel", getMonitoringChannel()}
        };
        return StringUtilities.INSTANCE.toString(observableInfo);
    }
    
}
