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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class contains unit tests for the creation of new {@link HttpServer} instances.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class HttpServerCreationTests {

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the expected attribute
     * values of a server instance created for the respective test iteration as well as the input for creating that
     * server instance. For each subset, the values represent:
     * <ul>
     * <li>The identifier of the server instance</li>
     * <li>The address (e.g., IP) to bind the server instance to</li>
     * <li>The port to bind the server instance socket to</li>
     * <li>The maximum number of incoming connections stored before the server instance refuses incoming
     *     connections</li>
     * <li>The expected creation result in terms of:</li>
     *     <ul>
     *     <li>The server instance string as provided by {@link HttpServer#toString()}, if creating the instance with
     *         the associated parameters above must be successful</li>
     *     <li>The detail message string of the top-level exception thrown during server instance creation, if creating
     *         the server instance must fail</li>
     *     </ul>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, null, -1, -1, "Invalid network element ID: \"null\""},
            
            {"", null, -1, -1, "Invalid network element ID: minimum 1 byte, but is 0"},
            
            {"#TestServer#", null, -1, -1,
                "Invalid network element ID: must contain only [0..9], [a..z], [A..Z], but is \"#TestServer#\""},
            
            {"TestServer", null, -1, -1, "Invalid HTTP server address: address is blank"},
            
            {"TestServer", "thisisnotanuri", -1, -1, "Invalid HTTP server port: -1"},
            
            {"TestServer", "thisisnotanuri", 0, -1, "Creating HTTP server \"TestServer\" failed"},
            
            {"TestServer", "localhost", -1, -1, "Invalid HTTP server port: -1"},
            
            {"TestServer", "localhost", 80, -1,
                "HttpServer{id=TestServer;address=/127.0.0.1:80;contexts=;state=initialized;}"},
            
            {"TestServer", "localhost", 80, 0,
                "HttpServer{id=TestServer;address=/127.0.0.1:80;contexts=;state=initialized;}"},
            
            {"TestServer", "localhost", 80, 10,
                "HttpServer{id=TestServer;address=/127.0.0.1:80;contexts=;state=initialized;}"},
            
            {"TestServer", "127.0.0.1", 80, 10,
                "HttpServer{id=TestServer;address=/127.0.0.1:80;contexts=;state=initialized;}"}
    };
    
    /**
     * The expected creation result. This string contains:
     * <ul>
     * <li>The server instance string as provided by {@link HttpServer#toString()}, if creating the instance must be
     *     successful</li>
     * <li>The detail message string of the top-level exception thrown during server instance creation, if creating
     *     the server instance must fail</li>
     * </ul>
     */
    private String expectedCreationResult;
    
    /**
     * The actual creation result. This string contains:
     * <ul>
     * <li>The server instance string provided by {@link HttpServer#toString()} after successful creation of the server
     *     instance</li>
     * <li>The detail message string of the top-level exception thrown during server instance creation, if creating
     *     the server instance failed</li>
     * </ul>
     */
    private String actualCreationResult;
    
    /**
     * Constructs a new {@link HttpServerCreationTests} instance for creating a server instance based on the given
     * parameters. If creating the instance is successful, the information provided by {@link HttpServer#toString()} for
     * this instance is set as the {@link #actualCreationResult}. If creating the instance fails, the detail message
     * string of the thrown {@link NetworkException} is set as the {@link #actualCreationResult}.
     * 
     * @param id the identifier of the server instance
     * @param address the address (e.g., IP) to bind the server instance to
     * @param port the port to bind the server instance socket to
     * @param backlog the maximum number of incoming connections stored before the server instance refuses incoming
     *        connections
     * @param expectedCreationResult either the server information as provided by {@link HttpServer#toString()}, if
     *        creating the instance must be successful, or the detail exception message, if creating the instance must
     *        fail
     */
    public HttpServerCreationTests(String id, String address, int port, int backlog, String expectedCreationResult) {
        this.expectedCreationResult = expectedCreationResult;
        try {
            HttpServer server = new HttpServer(id, address, port, backlog);
            actualCreationResult = server.toString();
            server.start();
            server.stop(5);
        } catch (NetworkException e) {
            actualCreationResult = e.getMessage();
        }
    }
    
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
     * is equal to the {@link #actualCreationResult} of creating the server instance of that iteration.  
     */
    @Test
    public void testEqualCreationResults() {
        assertEquals(expectedCreationResult, actualCreationResult, "Wrong creation result");
    }
    
}
