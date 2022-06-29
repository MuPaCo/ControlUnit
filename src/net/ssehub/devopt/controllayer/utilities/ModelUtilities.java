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

import java.util.Iterator;

import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.AbstractVariable;
import net.ssehub.easy.varModel.model.Project;
import net.ssehub.easy.varModel.model.datatypes.IDatatype;

/**
 * This class provides thread-safe utility methods for {@link Project} operations. In particular, the methods of this
 * class provide individual (types of) information defined in the respective IVML model. 
 * 
 * @author kroeher
 *
 */
public class ModelUtilities {

    /**
     * The singleton instance of this class.
     */
    public static final ModelUtilities INSTANCE = new ModelUtilities();
    
    /**
     * The name of the DevOpt meta model type defining an entity.
     */
    private static final String TYPE_ENTITY = "Entity";
    
    /**
     * The name of the DevOpt meta model type defining an entity description.
     */
    private static final String TYPE_ENTITY_DESCRIPTION = "EntityDescription";
    
    /**
     * The name of the DevOpt meta model type defining an entity identification description.
     */
    private static final String TYPE_IDENTIFICATION_DESCRIPTION = "IdentificationDescription";
    
    /**
     * The name of the DevOpt meta model type defining an entity identification.
     */
    private static final String TYPE_IDENTIFICATION = "Identification";
    
    /**
     * The name of the DevOpt meta model type defining an entity runtime description.
     */
    private static final String TYPE_RUNTIME_DESCRIPTION = "RuntimeDescription";
    
    /**
     * The name of the DevOpt meta model type defining an entity runtime date.
     */
    private static final String TYPE_RUNTIME_DATE = "RuntimeDate";
    
    /**
     * Constructs a new {@link ModelUtilities} instance.
     */
    private ModelUtilities() { }
    
    /**
     * Returns the identifier defined for the given entity.
     * 
     * @param entity the entity for which its identifier should be returned
     * @return the identifier of the given entity or <code>null</code>, if the given entity is <code>null</code>, the
     *         given entity is not of type {@link #TYPE_ENTITY}, or no such definition is available
     * @see #getEntity(Configuration)
     */
    public synchronized String getEntityIdentificationIdentifier(IDecisionVariable entity) {
        String identifier = null;
        if (entity != null && hasType(entity, TYPE_ENTITY)) {
            IDecisionVariable entityIdentification = getEntityIdentification(entity);
            if (entityIdentification != null) {
                Object value = getValue(getNestedByName(entityIdentification, "identifier"));
                if (value != null) {
                    identifier = String.valueOf(value);
                }
            }
        }
        return identifier;
    }
    
    /**
     * Returns the host defined for the given entity.
     * 
     * @param entity the entity for which its host should be returned
     * @return the host of the given entity or <code>null</code>, if the given entity is <code>null</code>, the given
     *         entity is not of type {@link #TYPE_ENTITY}, or no such definition is available
     * @see #getEntity(Configuration)
     */
    public synchronized String getEntityIdentificationHost(IDecisionVariable entity) {
        String host = null;
        if (entity != null && hasType(entity, TYPE_ENTITY)) {
            IDecisionVariable entityIdentification = getEntityIdentification(entity);
            if (entityIdentification != null) {
                Object value = getValue(getNestedByName(entityIdentification, "host"));
                if (value != null) {
                    host = String.valueOf(value);
                }
            }
        }
        return host;
    }
    
    /**
     * Returns the port number defined for the given entity.
     * 
     * @param entity the entity for which its port number should be returned
     * @return the port number of the given entity or <code>-1</code>, if the given entity is <code>null</code>, the
     *         given entity is not of type {@link #TYPE_ENTITY}, or no such definition is available
     * @see #getEntity(Configuration)
     */
    public synchronized int getEntityIdentificationPort(IDecisionVariable entity) {
        int port = -1;
        if (entity != null && hasType(entity, TYPE_ENTITY)) {
            IDecisionVariable entityIdentification = getEntityIdentification(entity);
            if (entityIdentification != null) {
                Object value = getValue(getNestedByName(entityIdentification, "port"));
                if (value != null) {
                    port = (int) value;
                }
            }
        }
        return port;
    }
    
    /**
     * Return the identification definition for the given entity.
     * 
     * @param entity the entity for which its identification definition should be returned; the caller of this method
     *        must ensure that this entity is of type {@link #TYPE_ENTITY}
     * @return the identification definition of the given entity or <code>null</code>, if the given entity is
     *         <code>null</code> or no such definition is available
     * @see #getEntity(Configuration)
     */
    private IDecisionVariable getEntityIdentification(IDecisionVariable entity) {
        IDecisionVariable entityIdentificationDescription = null;
        if (entity != null) {
            // Entity available; get the entity description (full set of all types of descriptions)
            entityIdentificationDescription = getNestedByType(entity, TYPE_ENTITY_DESCRIPTION);
            if (entityIdentificationDescription != null) {
                // Entity description available; get the identification description
                entityIdentificationDescription = getNestedByType(entityIdentificationDescription,
                        TYPE_IDENTIFICATION_DESCRIPTION);
                if (entityIdentificationDescription != null) {
                    // Entity identification description available; get the identification
                    entityIdentificationDescription = getNestedByType(entityIdentificationDescription,
                            TYPE_IDENTIFICATION);
                }
            }
        }
        return entityIdentificationDescription;
    }
    
    /**
     * Returns the monitoring scope defined for the given entity.
     * 
     * @param entity the entity for which its monitoring scope should be returned
     * @return the identifier of the given entity or <code>null</code>, if the given entity is <code>null</code>, the
     *         given entity is not of type {@link #TYPE_ENTITY}, or no such definition is available
     * @see #getEntity(Configuration)
     */
    public synchronized String getEntityRuntimeDateMonitoringScope(IDecisionVariable entity) {
        String scope = null;
        if (entity != null && hasType(entity, TYPE_ENTITY)) {
            IDecisionVariable entityRuntimeDate = getEntityRuntimeDate(entity);
            if (entityRuntimeDate != null) {
                Object value = getValue(getNestedByName(entityRuntimeDate, "monitoringScope"));
                if (value != null) {
                    scope = String.valueOf(value);
                }
            }
        }
        return scope;
    }
    
    /**
     * Return the runtime date definition for the given entity.
     * 
     * @param entity the entity for which its runtime date should be returned; the caller of this method must ensure
     *        that this entity is of type {@link #TYPE_ENTITY}
     * @return the runtime date definition of the given entity or <code>null</code>, if the given entity is
     *         <code>null</code> or no such definition is available
     * @see #getEntity(Configuration)
     */
    private IDecisionVariable getEntityRuntimeDate(IDecisionVariable entity) {
        IDecisionVariable entityRuntimeDescription = null;
        if (entity != null) {
            // Entity available; get the entity description (full set of all types of descriptions)
            entityRuntimeDescription = getNestedByType(entity, TYPE_ENTITY_DESCRIPTION);
            if (entityRuntimeDescription != null) {
                // Entity description available; get the runtime description
                entityRuntimeDescription = getNestedByType(entityRuntimeDescription, TYPE_RUNTIME_DESCRIPTION);
                if (entityRuntimeDescription != null) {
                    // Entity runtime description available; get the runtime date
                    entityRuntimeDescription = getNestedByType(entityRuntimeDescription, TYPE_RUNTIME_DATE);
                }
            }
        }
        return entityRuntimeDescription;
    }
    
    /**
     * Returns the entity (definition) of the given configuration.
     * 
     * @param configuration the configuration in which the entity (definition) should be found
     * @return the entity (definition) or <code>null</code>, if the given configuration is <code>null</code> or no such
     *         definition is available
     */
    public synchronized IDecisionVariable getEntity(Configuration configuration) {
        return getNestedByType(configuration, TYPE_ENTITY);
    }
    
    /**
     * Returns the first (top-level) decision variable found in the given configuration, which has the declaration
     * type denoted by the given type string.
     * 
     * @param configuration the configuration in which to search for a decision variable with the given type
     * @param type the string representation of the type (name) of the decision variable to search for
     * @return the first type-matching (top-level) decision variable or <code>null</code>, if the given configuration is
     *         <code>null</code>, the given type is <code>null</code> or <i>blank</i>, or no such definition is
     *         available.
     */
    private IDecisionVariable getNestedByType(Configuration configuration, String type) {
        IDecisionVariable nestedVariable = null;
        if (configuration != null && type != null && !type.isBlank()) {            
            Iterator<IDecisionVariable> variablesIterator = configuration.iterator();
            if (variablesIterator != null) {                
                while (nestedVariable == null && variablesIterator.hasNext()) {
                    nestedVariable = variablesIterator.next();
                    if (!hasType(nestedVariable, type)) {
                        nestedVariable = null;
                    }
                }
            }
        }
        return nestedVariable;
    }
    
    /**
     * Returns the first nested decision variable found in the given decision variable, which has the declaration type
     * denoted by the given type string. This method only considers the first nesting level (no recursion).
     * 
     * @param variable the decision variable in which to search for a nested decision variable with the given type
     * @param type the string representation of the type (name) of the nested decision variable to search for
     * @return the first type-matching nested decision variable or <code>null</code>, if the given decision variable is
     *         <code>null</code>, the given type is <code>null</code> or <i>blank</i>, or no such definition is
     *         available.
     */
    private IDecisionVariable getNestedByType(IDecisionVariable variable, String type) {
        IDecisionVariable nestedVariable = null;
        if (variable != null && type != null && !type.isBlank()) {
            int nestedElementsCount = variable.getNestedElementsCount();
            int nestedElementsCounter = 0;
            while (nestedVariable == null && nestedElementsCounter < nestedElementsCount) {
                nestedVariable = variable.getNestedElement(nestedElementsCounter);
                if (!hasType(nestedVariable, type)) {
                    nestedVariable = null;
                }
                nestedElementsCounter++;
            }
        }
        return nestedVariable;
    }
    
    /**
     * Checks whether the declaration type of the given decision variable is equal to the type denoted by the given
     * string.
     * 
     * @param variable the decision variable to check for an equal declaration type
     * @param type the string representation of the type (name) the declaration type of the decision variable should
     *        match
     * @return <code>true</code>, if the declaration type (name) of the given decision variable is equal to the given
     *         string, or <code>false</code>, if one of the parameters is <code>null</code>, the given name is
     *         <i>blank</i>, or there is no equality
     */
    private boolean hasType(IDecisionVariable variable, String type) {
        boolean hasType = false;
        if (variable != null && type != null && !type.isBlank()) {
            AbstractVariable variableDeclaration = variable.getDeclaration();
            if (variableDeclaration != null) {
                hasType = isEqual(variableDeclaration.getType(), type);
            }
        }
        return hasType;
    }
    
    /**
     * Checks whether the name of the given data type is equal to the given string.
     * 
     * @param datatype the data type to check for an equal name
     * @param type name of the data type to match
     * @return <code>true</code>, if the data type name is equal to the given string, or <code>false</code>, if one of
     *         the parameters is <code>null</code>, the given string is <i>blank</i>, or there is no equality
     */
    private boolean isEqual(IDatatype datatype, String type) {
        boolean isEqual = false;
        if (datatype != null && type != null && !type.isBlank()) {
            isEqual = type.equals(datatype.getName());
        }
        return isEqual;
    }

    /**
     * Returns the first nested decision variable found in the given decision variable, which has the declaration name
     * defined by the given name string. This method only considers the first nesting level (no recursion).
     * 
     * @param variable the decision variable in which to search for a nested decision variable with the given name
     * @param name the name of the nested decision variable to search for
     * @return the first name-matching nested decision variable or <code>null</code>, if the given decision variable is
     *         <code>null</code>, the given type is <code>null</code> or <i>blank</i>, or no such definition is
     *         available.
     */
    private IDecisionVariable getNestedByName(IDecisionVariable variable, String name) {
        IDecisionVariable nestedVariable = null;
        if (variable != null && name != null && !name.isBlank()) {
            int nestedElementsCount = variable.getNestedElementsCount();
            int nestedElementsCounter = 0;
            while (nestedVariable == null && nestedElementsCounter < nestedElementsCount) {
                nestedVariable = variable.getNestedElement(nestedElementsCounter);
                if (!hasName(nestedVariable, name)) {
                    nestedVariable = null;
                }
                nestedElementsCounter++;
            }
        }
        return nestedVariable;
    }
    
    /**
     * Checks whether the declaration name of the given decision variable is equal to the given string.
     * 
     * @param variable the decision variable to check for an equal declaration name
     * @param name the name the declaration name of the decision variable should match
     * @return <code>true</code>, if the declaration name of the given decision variable is equal to the given string,
     *         or <code>false</code>, if one of the parameters is <code>null</code>, the given type is <i>blank</i>, or
     *         there is no equality
     */
    private boolean hasName(IDecisionVariable variable, String name) {
        boolean hasName = false;
        if (variable != null && name != null && !name.isBlank()) {
            AbstractVariable variableDeclaration = variable.getDeclaration();
            if (variableDeclaration != null) {
                hasName = variableDeclaration.getName().equals(name);
            }
        }
        return hasName;
    }
    
    /**
     * Returns the object value of the given decision variable.
     * 
     * @param variable the decision variable to get the value from
     * @return the object value of the given decision variable or <code>null</code>, if the given decision variable or
     * its value is <code>null</code>
     */
    private Object getValue(IDecisionVariable variable) {
        Object value = null;
        if (variable != null && variable.getValue() != null) {
            value = variable.getValue().getValue();
        }
        return value;
    }
        
}
