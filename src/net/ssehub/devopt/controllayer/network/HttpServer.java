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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpContext;

/**
 * This class realizes a HTTP server for receiving HTTP requests.
 * 
 * @author kroeher
 *
 */
public class HttpServer extends AbstractNetworkElement {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = HttpServer.class.getSimpleName();
    
    /**
     * This enumeration defines the different states of a server. The possible states are:
     * <ul>
     * <li>{@link #INITIALIZED}</li>
     * <li>{@link #RUNNING}</li>
     * <li>{@link #STOPPED}</li>
     * </ul>
     * 
     * @author kroeher
     *
     */
    public static enum ServerState {
        /**
         * The server is set up completely, but not yet started.
         */
        INITIALIZED,
        
        /**
         * The server started and is handling incoming connections.
         */
        RUNNING,
        
        /**
         * The server has stopped and its internal socket is closed.
         */
        STOPPED
    };
    
    /**
     * The actual HTTP server instance.
     */
    private com.sun.net.httpserver.HttpServer server;
    
    /**
     * The list of {@link HttpContext}s added to this server. Never <code>null</code>, but may be empty.
     */
    private List<HttpContext> contexts;
    
    /**
     * The current state of this server.
     */
    private ServerState state;

    /**
     * Constructs a new {@link HttpServer} instance for receiving HTTP requests.
     * 
     * @param id the identifier of this client consisting of 1 to 23 UTF-8 encoded bytes representing the characters
     *        [0..9], [a..z], [A..Z] only
     * @param address the address (e.g., IP) to bind the server to; must not be <code>null</code>
     * @param port the port to bind the server socket to; must be between 0 and 65535 (including), while 0 indicates
     *        using an anonymous port allocated by the system automatically
     * @param backlog the maximum number of incoming connections stored before refusing incoming connections; this
     *        is not a guaranteed value as the actual number depends on the operating system, which is also the default
     *        value used in case of the given value is less than or equal to 0
     * @throws NetworkException if the given identifier is <code>null</code> or does not match the expected format, the
     *         given address is <code>null</code> or blank, the given port is invalid, or creating the server socket or
     *         the server itself fails
     */
    public HttpServer(String id, String address, int port, int backlog) throws NetworkException {
        super(id);
        createServer(address, port, backlog);
    }

    /**
     * Creates the actual HTTP {@link #server} for receiving HTTP requests.
     * 
     * @param address the address (e.g., IP) to bind the server to; must not be <code>null</code>
     * @param port the port to bind the server socket to; must be between 0 and 65535 (including), while 0 indicates
     *        using an anonymous port allocated by the system automatically
     * @param backlog the maximum number of incoming connections stored before refusing incoming connections; this
     *        is not a guaranteed value as the actual number depends on the operating system, which is also the default
     *        value used in case of the given value is less than or equal to 0
     * @throws NetworkException if the given address is <code>null</code> or blank, the given port is invalid, or
     *         creating the server socket or the server itself fails
     *         
     */
    private void createServer(String address, int port, int backlog) throws NetworkException {
        if (address == null || address.isBlank()) {
            throw new NetworkException("Invalid HTTP server address: address is blank");
        }
        if (port < 0 || port > 65535) {
            throw new NetworkException("Invalid HTTP server port: " + port);
        }
        InetSocketAddress serverAdress = null;
        try {
            serverAdress = new InetSocketAddress(address, port);
        } catch (IllegalArgumentException | SecurityException e) {
            throw new NetworkException("Creating socket address for HTTP server \"" + getId() + "\" failed", e);
        }
        try {
            server = com.sun.net.httpserver.HttpServer.create(serverAdress, backlog);
        } catch (IOException e) {
            throw new NetworkException("Creating HTTP server \"" + getId() + "\" failed", e);
        }
        contexts = new ArrayList<HttpContext>();
        state = ServerState.INITIALIZED;
        logger.logDebug(ID, "\"" + getId() + "\" created");
    }
    
    /**
     * Adds the given path and callback as a new {@link HttpContext} to this server. The given callback will be informed
     * about any incoming request to the context by the {@link HttpRequestHandler} assigned to that context. Adding a
     * new context is only successful, if the server is in state {@link ServerState#INITIALIZED} while calling this
     * method. Calling this method while the server is in any other state has no effect.
     * 
     * @param path the root URI path to associate the context with; must always start with "/" and must not be
     *        <code>null</code> or blank
     * @param callback the instance to call back, if a request for the new context arrives; must not be
     *        <code>null</code>
     * @return <code>true</code>, if adding the new context was successful; <code>false</code> otherwise 
     * @throws NetworkException if the given path is <code>null</code>, blank, or its first character is not '/', or the
     *         given callback is <code>null</code>, or creating the new context fails
     * @see #getState()
     */
    public boolean addContext(String path, HttpRequestCallback callback) throws NetworkException {
        boolean contextAddedSuccessful = false;
        if (state == ServerState.INITIALIZED) {            
            if (path == null || path.isBlank()) {
                throw new NetworkException("Adding context \"" + path + "\" to server \"" + getId() + "\" failed");
            }
            if (!path.startsWith("/")) {
                throw new NetworkException("Adding context \"" + path + "\" to server \"" + getId() 
                + "\" failed: path must start with \"/\"");
            }
            if (callback == null) {
                throw new NetworkException("Adding context \"" + path + "\" to server \"" + getId() 
                + "\" failed: callback is \"null\"");
            }
            try {            
                contextAddedSuccessful = contexts.add(server.createContext(path, new HttpRequestHandler(callback)));
            } catch (IllegalArgumentException e) {
                throw new NetworkException("Adding context \"" + path + "\" to server \"" + getId() + "\" failed", e);
            }
        }
        return contextAddedSuccessful;
    }
    
    /**
     * Starts this server in a separate thread. Hence, this method does not block its caller. Starting the server is
     * only successful, if the server is in state {@link ServerState#INITIALIZED} prior to calling this method. Calling
     * this method while the server is in any other state has no effect.<br>
     * <br>
     * <b>Note</b> that a stopped server cannot be restarted.
     * 
     * @return <code>true</code>, if starting the server was successful; <code>false</code> otherwise
     * @see #getState()
     */
    public boolean start() {
        boolean serverStarted = false;
        if (state == ServerState.INITIALIZED) {
            server.start();
            state = ServerState.RUNNING;
            serverStarted = true;
            logger.logDebug(ID, "Server started", this.toString());
        }
        return serverStarted;
    }
    
    /**
     * Stops this server by closing the listening socket and disallowing any new exchanges from being processed. This
     * method blocks its caller until all current exchange handlers have completed or else when approximately delay
     * seconds have elapsed (whichever happens sooner). Stopping the server is only successful, if the server is in
     * state {@link ServerState#RUNNING} prior to calling this method. Calling this method while the server is in any
     * other state has no effect.<br>
     * <br>
     * <b>Note</b> that a stopped server cannot be restarted.
     * 
     * @param delay the maximum time in seconds to wait until exchanges have finished
     * @return <code>true</code>, if stopping the server was successful; <code>false</code> otherwise
     * @see #getState()
     */
    public boolean stop(int delay) {
        boolean serverStopped = false;
        if (state == ServerState.RUNNING) {
            if (delay < 0) {
                delay = 0;
            }
            server.stop(delay);
            state = ServerState.STOPPED;
            serverStopped = true;
            logger.logDebug(ID, "Server stopped", this.toString());
        }
        return serverStopped;
    }
    
    /**
     * Returns the current state of this server instance. 
     * 
     * @return the current server state; never <code>null</code>
     */
    public ServerState getState() {
        return state;
    }
    
    /**
     * Returns the information about this instance to be included in the textual representation provided by
     * {@link #toString()}. This information contains:
     * <ul>
     * <li>The identifier of this instance's parent class (simple class name)</li>
     * <li>The identifier of this instance</li>
     * <li>The address of this server instance</li>
     * <li>All {@link HttpContext} paths added to this server instance prior to its execution</li>
     * </ul>
     */
    @Override
    protected String[][] getElementInfo() {
        String[] contextsInfo = new String[contexts.size() + 1];
        contextsInfo[0] = "contexts";
        for (int i = 0; i < contexts.size(); i++) {
            contextsInfo[i + 1] = contexts.get(i).getPath();
        }
        
        String[][] serverInfo = {
            {ID},
            {"id", getId()},
            {"address", server.getAddress().toString()},
            contextsInfo,
            {"state", state.name().toLowerCase()}
        };
        return serverInfo;
    }
    
}
