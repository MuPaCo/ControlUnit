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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class contains unit tests for the creation of new {@link HttpClient} instances.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class HttpClientCreationTests {

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the expected attribute
     * values of a client instance created for the respective test iteration as well as the input for creating that
     * client instance. For each subset, the values represent:
     * <ul>
     * <li>The identifier of the client instance</li>
     * <li>The HTTP version to support by the client instance</li>
     * <li>The HTTP redirect policy the client instance must follow</li>
     * <li>The URL of the proxy the client instance must use</li>
     * <li>The port of the proxy the client instance must use</li>
     * <li>The user name for authentication of the client instance</li>
     * <li>The password for authentication of the client instance</li>
     * <li>The expected creation result in terms of:</li>
     *     <ul>
     *     <li>The client instance string as provided by {@link HttpClient#toString()}, if creating the instance with
     *         the associated parameters above must be successful</li>
     *     <li>The detail message string of the top-level exception thrown during client instance creation, if creating
     *         the client instance must fail</li>
     *     </ul>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, null, null, null, 0, null, null, "Invalid network element ID: \"null\""},
            
            {"#TestClient#", null, null, null, 0, null, null,
                "Invalid network element ID: must contain only [0..9], [a..z], [A..Z], but is \"#TestClient#\""},
            
            {"TestClient", null, null, null, 0, null, null,
                "HttpClient{id=TestClient;version=HTTP_2;redirect=NEVER;proxy=null;username=null;password=null;"
                    + "connected=false;closed=false;}"},
            
            {"TestClient", Version.HTTP_2, Redirect.ALWAYS, null, -1, null, null,
                "HttpClient{id=TestClient;version=HTTP_2;redirect=ALWAYS;proxy=null;username=null;password=null;"
                    + "connected=false;closed=false;}"},
            
            {"TestClient", Version.HTTP_2, Redirect.ALWAYS, "", -1, null, null,
                "Invalid HTTP client proxy URL: URL is blank"},
            
            {"TestClient", Version.HTTP_2, Redirect.ALWAYS, "thisisnouri", 50, null, null,
                "Creating proxy for HTTP client \"TestClient\" failed"},
            
            {"TestClient", Version.HTTP_2, Redirect.ALWAYS, "127.0.0.1", -13, null, null,
                "Invalid HTTP client proxy port: -13"},
            
            {"TestClient", Version.HTTP_2, null, null, -1, "", "",
                "Invalid client credentials: user name is blank"},
            
            {"TestClient", Version.HTTP_2, null, null, -1, "u", "",
            "Invalid client credentials: password is blank"},
            
            {"TestClient", Version.HTTP_2, null, null, -1, null, null,
                "HttpClient{id=TestClient;version=HTTP_2;redirect=NEVER;proxy=null;username=null;password=null;"
                    + "connected=false;closed=false;}"},
            
            {"TestClient", Version.HTTP_1_1, Redirect.NEVER, "127.0.0.1", 50, "u", "p",
                "HttpClient{id=TestClient;version=HTTP_1_1;redirect=NEVER;proxy=127.0.0.1:50;username=u;password=p;"
                    + "connected=false;closed=false;}"}
    };
    
    /**
     * The expected creation result. This string contains:
     * <ul>
     * <li>The client instance string as provided by {@link HttpClient#toString()}, if creating the instance must be
     *     successful</li>
     * <li>The detail message string of the top-level exception thrown during client instance creation, if creating
     *     the client instance must fail</li>
     * </ul>
     */
    private String expectedCreationResult;
    
    /**
     * The actual creation result. This string contains:
     * <ul>
     * <li>The client instance string provided by {@link HttpClient#toString()} after successful creation of the client
     *     instance</li>
     * <li>The detail message string of the top-level exception thrown during client instance creation, if creating
     *     the client instance failed</li>
     * </ul>
     */
    private String actualCreationResult;
    
    /**
     * Constructs a new {@link HttpClientCreationTests} instance for creating a client instance based on the given
     * parameters. If creating the instance is successful, the information provided by {@link HttpClient#toString()} for
     * this instance is set as the {@link #actualCreationResult}. If creating the instance fails, the detail message
     * string of the thrown {@link NetworkException} is set as the {@link #actualCreationResult}.
     * 
     * @param id the identifier for creating a new client instance 
     * @param httpVersion the preferred HTTP protocol version for creating a new client instance
     * @param httpRedirect the redirect policy for creating a new client instance
     * @param proxyUrl the URL or IP of the proxy for creating a new client instance
     * @param proxyPort the port of the proxy for creating a new client instance
     * @param username the user name for creating a new client instance
     * @param password the password for creating a new client instance
     * @param expectedCreationResult either the client information as provided by {@link HttpClient#toString()}, if
     *        creating instance must be successful, or the detail exception message, if creating the instance must fail
     */
    //checkstyle: stop parameter number check
    public HttpClientCreationTests(String id, Version httpVersion, Redirect httpRedirect, String proxyUrl,
            int proxyPort, String username, String password, String expectedCreationResult) {
        this.expectedCreationResult = expectedCreationResult;
        actualCreationResult = null;
        try {
            HttpClient client = new HttpClient(id, httpVersion, httpRedirect, proxyUrl, proxyPort, username, password);
            actualCreationResult = client.toString();
        } catch (NetworkException e) {
            actualCreationResult = e.getMessage();
        }
    }
    //checkstyle: resume parameter number check
    
    /**
     * Returns the {@link #TEST_DATA} for the following tests in this class.
     * 
     * @return the {@link #TEST_DATA}
     */
    @Parameters
    public static Object[][] getTestData() {
        return TEST_DATA;
    }
    
    /**
     * Tests whether the {@link #expectedCreationResult} defined in the {@link #TEST_DATA} for a specific test iteration
     * is equal to the {@link #actualCreationResult} of creating the client instance of that iteration.  
     */
    @Test
    public void testEqualCreationResults() {
        assertEquals(expectedCreationResult, actualCreationResult, "Wrong creation result");
    }
    
}
