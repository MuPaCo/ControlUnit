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
package net.ssehub.devopt.controllayer.utilities;

import java.util.List;

/**
 * This class provides thread-safe utility methods for {@link String} operations.
 * 
 * @author kroeher
 *
 */
public class StringUtilities {

    /**
     * The singleton instance of this class.
     */
    public static final StringUtilities INSTANCE = new StringUtilities();
    
    /**
     * The constant, system-dependent line separator.
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    
    /**
     * Constructs a new {@link StringUtilities} instance.
     */
    private StringUtilities() {}
    
    /**
     * Returns a single string build from the given instance information. This method is typically used to create a
     * textual representation of an instance for the usual {@link #toString()} method of a class. Hence, this method
     * expected the given instance information as follows:
     * <ul>
     * <li>The first item in the first subset (<code>instanceInfo[0][0]</code>) must always be the main identify (see
     *     format description below); any subsequent items in the first subset, e.g. <code>instanceInfo[0][1]</code> and
     *     following, will be ignored</li>
     * <li>For each following subset, e.g. <code>instanceInfo[1]</code>, <code>instanceInfo[2]</code>, etc.:</li>
     *     <ul>
     *     <li>The first item is treated as a label</li>
     *     <li>All subsequent items are treated as values for the label</li>
     *     </ul>
     * </ul>
     * 
     * Based on the given instance information above, this method creates the single string in the following format:<br>
     * <i>main_identifier{label1=value1_1, value1_2,...;label2=value2_1,...;}</i><br>
     * <br>
     * If the given instance information is <code>null</code> or empty, a default string of the form <i>?{?}</i> will be
     * returned.
     *  
     * @param instanceInfos the instance information to be used to create the single string
     * @return the single string based on the given instance information
     */
    public synchronized String toString(String[][] instanceInfos) {
        String buildString = "?{?}";
        if (instanceInfos != null && instanceInfos.length != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(instanceInfos[0][0]);
            stringBuilder.append("{");
            String[] instanceInfo;
            for (int i = 1; i < instanceInfos.length; i++) {
                instanceInfo = instanceInfos[i];
                if (instanceInfo.length > 0) {                
                    stringBuilder.append(instanceInfo[0]);
                    stringBuilder.append("=");
                    if (instanceInfo.length > 1) {                    
                        stringBuilder.append(instanceInfo[1]);
                        for (int j = 2; j < instanceInfo.length; j++) {
                            stringBuilder.append(",");
                            stringBuilder.append(instanceInfo[j]);
                        }
                    }
                    stringBuilder.append(";");
                }
            }
            stringBuilder.append("}");
            buildString = stringBuilder.toString();
        }
        return buildString;
    }
    
    /**
     * Returns a new array of strings, which starts with the given new element followed by the given elements list in
     * exactly its order.
     * 
     * @param newElement the new string to prepend to the given list
     * @param elements the list of strings to prepend with the new element
     * @return the given list of strings prepended by the given new element or <code>null</code>, if one of the given
     *         parameters is <code>null</code>
     */
    public synchronized String[] prepend(String newElement, List<String> elements) {
        return prepend(newElement, elements.toArray(new String[elements.size()]));
    }
    
    /**
     * Returns a new array of strings, which starts with the given new element followed by the given elements array in
     * exactly their order.
     * 
     * @param newElement the new string to prepend to the given array
     * @param elements the array of strings to prepend with the new element
     * @return the given array of strings prepended by the given new element or <code>null</code>, if one of the given
     *         parameters is <code>null</code>
     */
    public synchronized String[] prepend(String newElement, String[] elements) {
        String[] prependedElements = null;
        if (newElement != null && elements != null) {
            prependedElements = new String[elements.length + 1];
            prependedElements[0] = newElement;
            for (int i = 0; i < elements.length; i++) {
                prependedElements[i + 1] = elements[i];
            }
        }
        return prependedElements;
    }
    
    /**
     * Returns a single string in which each element of the given string list is separated by a {@link #LINE_SEPARATOR}.
     * 
     * @param stringList the list of strings to concatenate to a single string; must not be <code>null</code>
     * @return the single string; may be <i>empty</i>, if the given list is empty.
     */
    public synchronized String toMultiLineString(List<String> stringList) {
        StringBuilder multiLineStringBuilder = new StringBuilder();
        if (stringList.size() > 0) {
            multiLineStringBuilder.append(stringList.get(0));
            for (int i = 1; i < stringList.size(); i++) {
                multiLineStringBuilder.append(LINE_SEPARATOR);
                multiLineStringBuilder.append(stringList.get(i));
            }
        }
        return multiLineStringBuilder.toString();
    }
    
}
