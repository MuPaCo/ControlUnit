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

import java.net.URI;

/**
 * This class realizes a simplified HTTP request. Its information is represented by generic data types instead of the
 * HTTP-specific ones received by the {@link HttpServer} (more specific, the {@link HttpRequestHandler}) from a
 * client. In this way, this class supports decoupling the network-specific data (types) from the internal data of the
 * remaining implementation. An instance of this class provides the following request information:
 * <ul>
 * <li>The {@link HttpRequestMethod}, e.g. <code>GET</code> or <code>POST</code></li>
 * <li>The target {@link URI} of the request</li>
 * <li>The body of the request as a single {@link String}</li>
 * </ul>
 * 
 * @author kroeher
 *
 */
public class HttpRequest {

    /**
     * The request method used to send this request.
     */
    private HttpRequestMethod method;
    
    /**
     * The target URI of this request.
     */
    private URI uri;
    
    /**
     * The request body of this request.
     */
    private String body;
    
    /**
     * The state of this request in terms of whether all of its information (attributes) are <code>null</code>
     * (<code>true</code>). If at least one of the information is not <code>null</code>, its value is
     * <code>false</code>.
     */
    private boolean isEmpty;
    
    /**
     * Constructs a new {@link HttpRequest} instance.<br>
     * <br>
     * Using <code>null</code> for one or more of the constructor's parameters may result in unintended behavior of the
     * request receiver. While there are no internal checks preventing from constructing an instance with such values,
     * doing so is not recommended.
     * 
     * @param method the request method used to send this request
     * @param uri the target URI of this request
     * @param body the request body of this request
     */
    public HttpRequest(HttpRequestMethod method, URI uri, String body) {
        this.method = method;
        this.uri = uri;
        this.body = body;
        if (this.method == null && this.uri == null && this.body == null) {
            isEmpty = true;
        } else {
            isEmpty = false;
        }
    }
    
    /**
     * Returns the request method used to send this request.
     * 
     * @return the request method used to send this request or <code>null</code>, if no such information is available
     *         for this request
     * @see #isEmpty()
     */
    public HttpRequestMethod getMethod() {
        return method;
    }
    
    /**
     * Returns the target URI of this request.
     * 
     * @return the target URI of this request or <code>null</code>, if no such information is available for this request
     * @see #isEmpty()
     */
    public URI getUri() {
        return uri;
    }
    
    /**
     * Returns the request body of this request.
     * 
     * @return the request body of this request or <code>null</code>, if no such information is available for this
     *         request
     * @see #isEmpty()
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Returns the state of this request in terms of whether all of its information (attributes) are <code>null</code>
     * or not.
     * 
     * @return <code>true</code>, if all internal request attributes are <code>null</code>; <code>false</code>, if at
     *         least one of the attributes is not <code>null</code>
     */
    public boolean isEmpty() {
        return isEmpty;
    }
    
}
