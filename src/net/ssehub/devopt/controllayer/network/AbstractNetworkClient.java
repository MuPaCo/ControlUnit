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

/**
 * This class represents an abstract network client for communication with remote hosts.
 * 
 * @author kroeher
 *
 */
public abstract class AbstractNetworkClient extends AbstractNetworkElement {
    
    /**
     * The <i>optional</i> user name required to communicate with the desired host.<br>
     * Always <code>null</code>, if no user name is required.
     */
    private String username;
    
    /**
     * The <i>optional</i> password required to communicate with the desired host.<br>
     * Always <code>null</code>, if no password is required.
     */
    private String password;
    
    /**
     * The parameter indicating whether this client is connected to a remote host (<code>true</code>) or not
     * (<code>false</code>). The latter is the default value.
     */
    private boolean isConnected;
    
    /**
     * The parameter indicating whether this client is closed (<code>true</code>) or not (<code>false</code>). The
     * latter is the default value.
     */
    private boolean isClosed;
    
    /**
     * Constructs a new {@link AbstractNetworkClient} instance for connection to a host.
     * 
     * @param id the identifier of this client consisting of 1 to 23 UTF-8 encoded bytes representing the characters
     *        [0..9], [a..z], [A..Z] only
     * @param username the <i>optional</i> user name required for this client to communicate with its host or
     *        <code>null</code>, if no user name is required
     * @param password the <i>optional</i> password required for this client to communicate with its host or
     *        <code>null</code>, if no user name is required
     * @throws NetworkException if the given identifier is <code>null</code> or does not match the expected format
     */
    protected AbstractNetworkClient(String id, String username, String password)
            throws NetworkException {
        super(id);
        setCredentials(username, password);
        setIsConnected(false);
        setIsClosed(false);
    }
    
    /**
     * Sets the given strings as credentials for this client's communication with its host.
     * 
     * @param username the string to set as user name of this client's credentials 
     * @param password the string to set as password of this client's credentials
     * @throws NetworkException if either the user name or the password is not <code>null</code>, but blank
     */
    private void setCredentials(String username, String password) throws NetworkException {
        if (username != null && username.isBlank()) {
            throw new NetworkException("Invalid client credentials: user name is blank");
        }
        if (password != null && password.isBlank()) {
            throw new NetworkException("Invalid client credentials: password is blank");
        }
        this.username = username;
        this.password = password;
    }
    
    /**
     * Returns the {@link #username} for this client's communication with its host. 
     * 
     * @return the user name for this client's communication or <code>null</code>, if this client does not require a
     *         user name for its communication
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the {@link #password} for this client's communication with its host. 
     * 
     * @return the password for this client's communication or <code>null</code>, if this client does not require a
     *         password for its communication
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Sets the state of this client in terms of whether it is connected to a remote host or not.
     * 
     * @param isConnected <code>true</code>, if this client is connected to a remote host; <code>false</code> otherwise
     */
    protected void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }
    
    /**
     * Returns the state of this client in terms of whether it is connected to a remote host or not.
     * 
     * @return <code>true</code>, if this client is connected to a remote host; <code>false</code> otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Sets the state of this client in terms of whether it is closed or not.
     * 
     * @param isClosed <code>true</code>, if this client is closed; <code>false</code> otherwise
     */
    protected void setIsClosed(boolean isClosed) {
        this.isClosed = isClosed;
    }
    
    /**
     * Returns the state of this client in terms of whether it is closed or not.
     * 
     * @return <code>true</code>, if this client is closed; <code>false</code> otherwise
     */
    public boolean isClosed() {
        return isClosed;
    }
    
    /**
     * Closes this client.
     * 
     * @throws NetworkException if closing this client fails
     */
    public abstract void close() throws NetworkException;
    
}
