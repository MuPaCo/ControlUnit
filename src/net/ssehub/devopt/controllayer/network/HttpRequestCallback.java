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
 * This interface needs to be implemented by any class that needs to be informed about and react to specific requests to
 * the {@link HttpServer}. For each {@link com.sun.net.httpserver.HttpContext} of an instance of this server, the
 * assigned {@link HttpRequestHandler} will inform the implementing class about arrived server requests using the
 * methods defined by this interface.
 * 
 * @author kroeher
 *
 */
public interface HttpRequestCallback {

    /**
     * Receives the client request to the specific {@link com.sun.net.httpserver.HttpContext} of the {@link HttpServer}
     * this instance is added to.
     * 
     * @param request the simplified HTTP request received from a client or <code>null</code>, if an error occurred
     *        during processing the request
     * @param exception the error occurred during processing the request by the {@link HttpRequestHandler} assigned to
     *        this instance's context; may be <code>null</code>
     * @return the response to send to the client or <code>null</code>, if processing the request by the callback fails
     * @see HttpServer#addContext(String, HttpRequestCallback) 
     */
    public abstract HttpResponse requestArrived(HttpRequest request, NetworkException exception);
    
    /**
     * The name of the callback used for logging information, e.g. in case of warnings or errors.
     * 
     * @return the name of the callback; should not be <code>null</code>
     */
    public abstract String getName();
    
}
