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

import net.ssehub.devopt.controllayer.utilities.StringUtilities;

/**
 * This class realizes a simplified HTTP response. Its information is represented by generic data types instead of the
 * HTTP-specific ones send by the {@link HttpServer} (more specific, the {@link HttpRequestHandler}) to a client. In
 * this way, this class supports decoupling the network-specific data (types) from the internal data of the
 * remaining implementation. An instance of this class provides the following response information:
 * <ul>
 * <li>The set of response headers as an array of {@link String}, which follows the pattern described by
 *     {@link java.net.http.HttpRequest.Builder#headers(String...)}</li>
 * <li>The HTTP response code to be send to the client</li>
 * <li>The body of the response as a single {@link String}</li>
 * </ul>
 * 
 * @author kroeher
 *
 */
public class HttpResponse {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = HttpResponse.class.getSimpleName();
    
    /**
     * The response headers of this response following the pattern described by
     * {@link java.net.http.HttpRequest.Builder#headers(String...)}.
     */
    private String[] headers;
    
    /**
     * The HTTP response code of this response.
     */
    private int code;
    
    /**
     * The response body of this response.
     */
    private String body;

    /**
     * Constructs a new {@link HttpResponse} instance.<br>
     * <br>
     * Using <code>null</code> for one or more of the constructor's parameters, an invalid response code, or invalid
     * headers may result in unintended behavior of the response receiver. While there are no internal checks preventing
     * from constructing an instance with such values, doing so is not recommended.
     * 
     * @param headers the response headers of this response following the pattern described by
     *        {@link java.net.http.HttpRequest.Builder#headers(String...)}
     * @param code the HTTP response code of this response
     * @param body the response body of this response
     */
    public HttpResponse(String[] headers, int code, String body) {
        this.headers = headers;
        this.code = code;
        this.body = body;
    }
    
    /**
     * Returns the response headers of this response following the pattern described by
     * {@link java.net.http.HttpRequest.Builder#headers(String...)}.
     * 
     * @return the response headers of this response or <code>null</code>, if no such information is available for this
     *         response
     */
    public String[] getHeaders() {
        return headers;
    }
    
    /**
     * Returns the HTTP response code of this response.
     * 
     * @return the HTTP response code of this response
     */
    public int getCode() {
        return code;
    }
    
    /**
     * Returns the response body of this response.
     * 
     * @return the response body of this response or <code>null</code>, if no such information is available for this
     *         response
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns a textual representation of this instance as provided by {@link StringUtilities#toString(String[][])}.
     * This information contains:
     * <ul>
     * <li>The identifier of this instance's parent class (simple class name)</li>
     * <li>The response headers of this instance following the pattern described by
     *     {@link java.net.http.HttpRequest.Builder#headers(String...)}</li>
     * <li>The HTTP response code of this instance</li>
     * <li>The response body of this instance</li>
     * </ul>
     * 
     * @return a textual representation of this instance as provided by {@link StringUtilities#toString(String[][])}
     */
    @Override
    public String toString() {
        String[] headersInfo = {"headers"};
        if (headers != null) {            
            headersInfo = new String[headers.length + 1];
            headersInfo[0] = "headers";
            for (int i = 0; i < headers.length; i++) {
                headersInfo[i + 1] = headers[i];
            }
        }
                
        String[][] responseInfo = {
            {ID},
            headersInfo,
            {"code", "" + code},
            {"body", "\"" + body + "\""}
        };
        return StringUtilities.INSTANCE.toString(responseInfo);
    }
    
}
