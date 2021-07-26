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
package net.ssehub.devopt.controllayer.network;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import net.ssehub.devopt.controllayer.utilities.Logger;
import net.ssehub.devopt.controllayer.utilities.StringUtilities;

/**
 * This class represents the abstract super-class for all network elements of this project.
 * 
 * @author kroeher
 *
 */
public abstract class AbstractNetworkElement {
    
    /**
     * The local reference to the global {@link Logger}.
     */
    protected Logger logger = Logger.INSTANCE;
    
    /**
     * The identifier of this element. This identifier must satisfy the following requirements to be valid:
     * <ul>
     * <li>Non-<code>null</code></li>
     * <li>Between 1 and 23 UTF-8 encoded bytes in length</li>
     * <li>Contains only the characters [0..9], [a..z], [A..Z]</li>
     * </ul>
     * Never <code>null</code> and always checked for being a valid identifier during instantiation.
     */
    private String id;

    /**
     * Constructs a new {@link AbstractNetworkElement} instance.
     * 
     * @param id the identifier of this element consisting of 1 to 23 UTF-8 encoded bytes representing the characters
     *        [0..9], [a..z], [A..Z] only
     * @throws NetworkException if the given identifier is <code>null</code> or does not match the expected format
     */
    protected AbstractNetworkElement(String id) throws NetworkException {
        setId(id);
    }
    
    /**
     * Sets the given string as the identifier of this element, if it matches the expected format of the internal
     * {@link #id}.
     * 
     * @param id the string to set as identifier of this element
     * @throws NetworkException if the given identifier is <code>null</code> or does not match the expected format of
     *         the internal {@link #id}
     */
    private void setId(String id) throws NetworkException {
        // Check for general id definition (non-null)
        if (id == null) {
            throw new NetworkException("Invalid network element ID: \"null\"");
        }
        byte[] idBytes;
        try {
            // Check for correct encoding (UTF-8)
            idBytes = id.getBytes(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new NetworkException("Invalid network element ID: not UTF-8 encoded", e);
        }
        // Check for correct length (1-23 bytes)
        if (idBytes.length <= 0) {
            throw new NetworkException("Invalid network element ID: minimum 1 byte, but is " + idBytes.length);
        }
        if (idBytes.length > 23) {
            throw new NetworkException("Invalid network element ID: maximum 23 bytes, but is " + idBytes.length);
        }
        // Check for correct characters ([0..9], [a..z], [A..Z]):
        if (!Pattern.matches("(\\d|[a-zA-Z])+", id)) {
            throw new NetworkException("Invalid network element ID: must contain only [0..9], [a..z], [A..Z], but is \""
                    + id + "\"");
        }
        this.id = id;
    }
    
    /**
     * Returns the information about this instance to be included in the textual representation provided by
     * {@link #toString()}. Hence, the returned information must comply with the expected input to
     * {@link StringUtilities#toString(String[][])}.
     *  
     * @return the set of instance information to be used by {@link StringUtilities#toString(String[][])}, if
     *         {@link #toString()} is called
     */
    protected abstract String[][] getElementInfo();
    
    /**
     * Returns the {@link #id} of this element.
     * 
     * @return the identifier of this element; never <code>null</code> and always a valid with respect to the expected
     *         format of the internal {@link #id}
     */
    public String getId() {
        return id;
    }
    
    /**
     * Returns a textual representation of this instance as provided by {@link StringUtilities#toString(String[][])}.
     * The specific information included in this representation depends on the implementation of
     * {@link #getElementInfo()} in the specific sub-classes of this class.
     * 
     * @return a textual representation of this instance as provided by {@link StringUtilities#toString(String[][])}
     */
    @Override
    public String toString() {
        return StringUtilities.INSTANCE.toString(getElementInfo());
    }
    
}
