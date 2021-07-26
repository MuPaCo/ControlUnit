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
 * This class implements the {@link HttpRequestCallback} interface in order receive HTTP request send to
 * {@link HttpServer} instance and return an appropriate respond. Instances of this class are used in the
 * {@link HttpServerUsageTests} to test the correct receiving and responding of the respective server type.
 *  
 * @author kroeher
 *
 */
public class HttpRequestCallbackHandler implements HttpRequestCallback {
    
    /**
     * The identifier of this instance. 
     */
    private String id;
    
    /**
     * Constructs a new {@link HttpRequestCallbackHandler} instance.
     * 
     * @param id the identifier of this instance; should not be <code>null</code>
     */
    public HttpRequestCallbackHandler(String id) {
        this.id = id;
    }

    @Override
    public HttpResponse requestArrived(HttpRequest request, NetworkException exception) {
        HttpResponse response = null;
        int responseCode = 500; // default is generic server-side error
        StringBuilder responseBodyBuilder = new StringBuilder();
        if (request != null) {
            responseBodyBuilder.append("method=");
            responseBodyBuilder.append(request.getMethod());
            responseBodyBuilder.append(";uri=");
            responseBodyBuilder.append(request.getUri());
            responseBodyBuilder.append(";body=");
            responseBodyBuilder.append(request.getBody());
            responseBodyBuilder.append(";");
            responseCode = 200;
        }
        if (exception != null) {
            responseBodyBuilder.append("error=");
            responseBodyBuilder.append(exception.getMessage());
            responseBodyBuilder.append(";");
        }
        response = new HttpResponse(null, responseCode, responseBodyBuilder.toString());
        return response;
    }

    @Override
    public String getName() {
        return id;
    }
    
}
