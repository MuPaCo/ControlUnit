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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides thread-safe utility methods for reading and writing files.
 * 
 * @author kroeher
 *
 */
public class FileUtilities {
    
    /**
     * The singleton instance of this class.
     */
    public static final FileUtilities INSTANCE = new FileUtilities();
    
    /**
     * The identifier if this class, e.g. for printing messages.
     */
    private static final String ID = "FileUtilities";
    
    /**
     * This enumeration defines the different options how to write content to a file. This class differentiates:
     * <ul>
     * <li>CREATE: Creates a new file and writes the new content to that empty file; if the file already exists, an
     *             exception will be thrown</li>
     * <li>OVERWRITE: Overwrites the content of an existing file with the new content; if the file does not exist, an
     *                exception will be thrown</li>
     * <li>APPEND: Appends the new content to the end of the content of an existing file; if the file does not exist, an
     *             exception will be thrown</li>
     * </ul>
     * 
     * @author Christian Kroeher
     *
     */
    public enum WriteOption { CREATE, OVERWRITE, APPEND }
    
    /**
     * The reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * Constructs a new {@link FileUtilities} instance.
     */
    private FileUtilities() {}
    
    /**
     * Creates a new {@link File} object denoting a directory based on the given path. If the directory does not exist,
     * this methods creates it including all non-existing parent directories on its path.
     *  
     * @param path the {@link String} denoting the path to the directory
     * @return the respective file object
     * @throws FileUtilitiesException if the given path is either <code>null</code> or empty, if the paths do not denote
     *         a directory, if the directory already exists, or creating the directory fails
     */
    public synchronized File createDirectory(String path) throws FileUtilitiesException {
        File directory = getFileObject(path);
        createDirectory(directory);
        return directory;
    }
    
    /**
     * Creates a new {@link File} object denoting a directory based on the given parent and child paths. If the
     * directory does not exist, this methods creates it including all non-existing parent directories on its path.
     *  
     * @param parent the {@link String} denoting the parent path to the directory
     * @param child the {@link String} defining the child path to or the name of the new directory
     * @return the respective file object
     * @throws FileUtilitiesException if the given parent or child paths are either <code>null</code> or empty, if the
     *         paths do not denote a directory, if the directory already exists, or creating the directory fails
     */
    public synchronized File createDirectory(String parent, String child) throws FileUtilitiesException {
        File directory = getFileObject(parent, child);
        createDirectory(directory);
        return directory;
    }
    
    /**
     * Creates all directories on the path of the given {@link File}. If the directories already exist, no further
     * actions are performed.
     *
     * @param directory the {@link File} denoting a directory to create
     * @throws FileUtilitiesException if the given {@link File} is <code>null</code>, does not denote a directory, or
     *         creating the directories fails
     */
    private void createDirectory(File directory) throws FileUtilitiesException {
        if (directory == null) {
            throw new FileUtilitiesException("Creating new directory denied: file is \"null\"");
        }
        if (!directory.exists()) {            
            try {
                Files.createDirectories(directory.toPath());
            } catch (UnsupportedOperationException e) {
                throw new FileUtilitiesException("Creating new directory \"" + directory.getAbsolutePath()
                        + "\" failed: path malformed", e);
            } catch (FileAlreadyExistsException e) {
                throw new FileUtilitiesException("Creating new directory \"" + directory.getAbsolutePath()
                        + "\" failed: directory already exists", e);
            } catch (SecurityException e) {
                throw new FileUtilitiesException("Creating new directory \"" + directory.getAbsolutePath()
                        + "\" failed: security manager denies checking directory", e);
            } catch (IOException e) {
                throw new FileUtilitiesException("Creating new directory \"" + directory.getAbsolutePath()
                        + "\" failed", e);
            }
        } else {
            logger.logDebug(ID, "Directory to create already exists: no further actions", "Directory: \"" 
                    + directory.getAbsolutePath() + "\"");
        }
    }
    
    /**
     * Writes the given content to the file specified by the given path and file name. Depending on the given
     * {@link WriteOption}, this method creates a new file, overwrites an existing file, or appends the given content to
     * the end of an existing file.
     * 
     * @param path the fully-qualified path to the file to write to
     * @param fileName the name of the file (including the file extension) to write to
     * @param fileContent the input stream providing the content to write
     * @param writeOption the write option defining how to write
     * @throws FileUtilitiesException if creating, writing, overwriting or appending the desired file fails
     */
    public synchronized void writeFile(String path, String fileName, InputStream fileContent, WriteOption writeOption)
            throws FileUtilitiesException {
        logger.logDebug(ID, "Writing file", "Path: \"" + path + "\"", "Name: \"" + fileName + "\"", "Option: "
                + writeOption.name());
        String fileContentString = new BufferedReader(new InputStreamReader(fileContent, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining(System.lineSeparator()));
        writeFile(path, fileName, fileContentString, writeOption);
    }
    
    /**
     * Writes the given content to the file specified by the given path and file name. Depending on the given
     * {@link WriteOption}, this method creates a new file, overwrites an existing file, or appends the given content to
     * the end of an existing file.
     * 
     * @param path the fully-qualified path to the file to write to
     * @param fileName the name of the file (including the file extension) to write to
     * @param fileContent the content to write
     * @param writeOption the write option defining how to write
     * @throws FileUtilitiesException if creating, writing, overwriting or appending the desired file fails
     */
    public synchronized void writeFile(String path, String fileName, String fileContent, WriteOption writeOption)
            throws FileUtilitiesException {
        logger.logDebug(ID, "Writing file", "Path: \"" + path + "\"", "Name: \"" + fileName + "\"", "Option: "
                + writeOption.name());
        File file = getFileObject(path, fileName);
        switch(writeOption) {
        case CREATE:
            writeFile(file, fileContent);
            break;
        case OVERWRITE:
            overwriteFile(file, fileContent);
            break;
        case APPEND:
            appendFile(file, fileContent);
            break;
        default:
            // should never be reached
            throw new FileUtilitiesException("Unknown write option \"" + writeOption + "\" for writing file \"" 
                    + file.getAbsolutePath());
        }
    }
    
    /**
     * Creates the given file and writes the given file content to that empty file.
     * 
     * @param file the {@link File} to be created and to which the given file content shall be written
     * @param fileContent the {@link String} defining the content that shall be written to the new file 
     * @throws FileUtilitiesException if the file already exists or writing the file content fails
     */
    private void writeFile(File file, String fileContent) throws FileUtilitiesException {
        logger.logDebug(ID, "Creating and writing new file", "File: \"" + file.getAbsolutePath() + "\"");
        if (file.exists()) {
            throw new FileUtilitiesException("Creating new file \"" + file.getAbsolutePath()
                    + "\" denied as the file already exists");
        }
        // Create parent directories, if they do not exist
        File parentDirectory = file.getParentFile();
        if (parentDirectory != null) {
            try {                
                createDirectory(parentDirectory);
            } catch (FileUtilitiesException e) {
                throw new FileUtilitiesException("Creating parent directories for new file \"" + file.getAbsolutePath()
                        + "\" failed", e);
            }
        }
        // Create and write file
        try {
            Files.write(file.toPath(), fileContent.getBytes());
        } catch (IOException e) {
            throw new FileUtilitiesException("Writing content to file \"" + file.getAbsolutePath()
                + "\" failed", e);
        }
    }
    
    /**
     * Overwrites the content of the given file with the given file content.
     * 
     * @param file the existing {@link File} to be overwritten
     * @param fileContent the {@link String} defining the new content that shall be written to the existing file 
     * @throws FileUtilitiesException if the file does not exist or writing the new file content fails
     */
    private void overwriteFile(File file, String fileContent) throws FileUtilitiesException {
        logger.logDebug(ID, "Overwriting existing file", "File: \"" + file.getAbsolutePath() + "\"");
        if (!file.exists()) {
            throw new FileUtilitiesException("Overwriting file \"" + file.getAbsolutePath()
                    + "\" impossible as the file does not exist");
        }
        // Overwrite file
        try {
            Files.write(file.toPath(), fileContent.getBytes());
        } catch (IOException e) {
            throw new FileUtilitiesException("Overwriting content of file \"" + file.getAbsolutePath()
                + "\" failed", e);
        }
    }
    
    /**
     * Appends the given file content to the end of the content of the given file.
     * 
     * @param file the existing {@link File} to which the new file content shall be appended
     * @param fileContent the {@link String} defining the new content that shall be appended to the existing file 
     * @throws FileUtilitiesException if the file does not exist or writing the new file content fails
     */
    private void appendFile(File file, String fileContent) throws FileUtilitiesException {
        logger.logDebug(ID, "Appending content to existing file", "File: \"" + file.getAbsolutePath() + "\"");
        if (!file.exists()) {
            throw new FileUtilitiesException("Appending file \"" + file.getAbsolutePath()
                    + "\" impossible as the file does not exist");
        }
        // Append file
        try {
            Files.write(file.toPath(), fileContent.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new FileUtilitiesException("Appending content to file \"" + file.getAbsolutePath()
                + "\" failed", e);
        }
    }
    
    /**
     * Reads the content of the given {@link File} and returns a {@link List} of {@link String}s, in which each string
     * contains a single line of the content of the file.
     * 
     * @param file the {@link File} that shall be read
     * @return a {@link List} of {@link String}s representing the line-wise content of the given file
     * @throws FileUtilitiesException if the given file is <code>null</code> or reading the file fails
     */
    public synchronized List<String> readFile(File file) throws FileUtilitiesException {
        List<String> fileLines = null;
        if (file != null) {
            logger.logDebug(ID, "Reading file \"" + file.getAbsolutePath() + "\"");
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;       
            try {
                fileLines = new ArrayList<String>();
                fileReader = new FileReader(file);
                bufferedReader = new BufferedReader(fileReader);
                String fileLine;
                while ((fileLine = bufferedReader.readLine()) != null) {
                    fileLines.add(fileLine);
                }
            } catch (IOException | OutOfMemoryError e) {
                throw new FileUtilitiesException("Reading content from file \"" + file.getAbsolutePath() + "\" failed",
                        e);
            } finally {
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException e) {
                        throw new FileUtilitiesException("Closing file reader for \"" + file.getAbsolutePath()
                        + "\" failed", e);
                    }
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        throw new FileUtilitiesException("Closing buffered reader for \"" + file.getAbsolutePath()
                        + "\" failed", e);
                    }
                }
            }
        } else {
            throw new FileUtilitiesException("The given file to read is \"null\"");
        }
        return fileLines;
    }
    
    /**
     * Creates a new {@link File} object based on the given path.
     * 
     * @param path the {@link String} denoting absolute path to the file
     * @return the respective file object
     * @throws FileUtilitiesException if the given path is either <code>null</code> or empty
     */
    public synchronized File getFileObject(String path) throws FileUtilitiesException {
        if (path == null) {
            throw new FileUtilitiesException("No file object for path \"null\"");
        }
        if (path.isBlank()) {
            throw new FileUtilitiesException("No file object for empty path");
        }
        return new File(path);
    }
    
    /**
     * Creates a new {@link File} object based on the given parent and child paths.
     * 
     * @param parent the {@link String} denoting the parent path to the file
     * @param child the {@link String} defining the child path to or the name of the file
     * @return the respective file object
     * @throws FileUtilitiesException if the given parent or child paths are either <code>null</code> or empty
     */
    public synchronized File getFileObject(String parent, String child) throws FileUtilitiesException {
        if (parent == null) {
            throw new FileUtilitiesException("No file object for parent path \"null\"");
        }
        if (parent.isBlank()) {
            throw new FileUtilitiesException("No file object for empty parent path");
        }
        if (child == null) {
            throw new FileUtilitiesException("No file object for child path \"null\"");
        }
        if (child.isBlank()) {
            throw new FileUtilitiesException("No file object for empty child path");
        }
        return new File(parent, child);
    }
    
    /**
     * Creates a new {@link File} object based on the given path. Further, the file represented by this object is
     * checked for existence and whether it is a file or a directory as indicated by the <code>isDirectory</code>
     * parameter. 
     * 
     * @param path the {@link String} denoting absolute path to the file
     * @param isDirectory <code>true</code>, if the path denotes a directory, or <code>false</code>, if the path denotes
     *        a file
     * @return the respective file object
     * @throws FileUtilitiesException if the given path is either <code>null</code> or empty, if the file does not
     *         exist, or, if the file is not a file or a directory as indicated by <code>isDirectory</code>
     */
    public synchronized File getCheckedFileObject(String path, boolean isDirectory) throws FileUtilitiesException {
        return getCheckedFileObject(getFileObject(path), isDirectory);
    }
    
    /**
     * Creates a new {@link File} object based on the given parent and child paths. Further, the file represented by
     * this object is checked for existence and whether it is a file or a directory as indicated by the
     * <code>isDirectory</code> parameter.
     * 
     * @param parent the {@link String} denoting the parent path to the file
     * @param child the {@link String} defining the child path to or the name of the file
     * @param isDirectory <code>true</code>, if the paths denote a directory, or <code>false</code>, if the paths denote
     *        a file
     * @return the respective file object
     * @throws FileUtilitiesException if the given parent or child paths are either <code>null</code> or empty, if the
     *         file does not exist, or, if the file is not a file or a directory as indicated by
     *         <code>isDirectory</code>
     */
    public synchronized File getCheckedFileObject(String parent, String child, boolean isDirectory)
            throws FileUtilitiesException {
        return getCheckedFileObject(getFileObject(parent, child), isDirectory);
    }
    
    /**
     * Checks the given {@link File} for existence and whether it is a file or a directory as indicated by the
     * <code>isDirectory</code> parameter. If the file passes all checks, this method will return the same file object.
     * If any check fails, this method throws an exception.
     * 
     * @param file the {@link File} to check; must not be <code>null</code>
     * @param isDirectory <code>true</code>, if the file denotes a directory, or <code>false</code>, if the file denotes
     *        a file
     * @return the given {@link File}
     * @throws FileUtilitiesException if the file does not exist, or, if the file is not a file or a directory as
     *         indicated by <code>isDirectory</code> 
     */
    private File getCheckedFileObject(File file, boolean isDirectory) throws FileUtilitiesException {
        if (!file.exists()) {
            throw new FileUtilitiesException("File \"" + file.getAbsolutePath() + "\" does not exist");
        }
        if (isDirectory && !file.isDirectory()) {
            throw new FileUtilitiesException("File \"" + file.getAbsolutePath() + "\" is not a directory");
        }
        if (!isDirectory && !file.isFile()) {
            throw new FileUtilitiesException("File \"" + file.getAbsolutePath() + "\" is not a file");
        }
        return file;
    }
    
    /**
     * Deletes the given {@link File}. If the given file is a directory, this method deletes all its content as well as
     * the directory itself.
     * 
     * @param file the {@link File} to delete
     * @throws FileUtilitiesException if the given file is <code>null</code>, does not exist, or deleting the file fails
     */
    public synchronized void delete(File file) throws FileUtilitiesException {
        if (file == null) {
            throw new FileUtilitiesException("Deleting file failed: file is \"null\"");
        }
        if (!file.exists()) {
            throw new FileUtilitiesException("Deleting file \"" + file.getAbsolutePath() 
                    + " failed: file does not exist");
        }
        deleteUnchecked(file);
    }
    
    /**
     * Deletes the given {@link File} without any previous checks, like being <code>null</code> or existence. If the
     * given file is a directory, this method deletes all its content as well as the directory itself.
     * 
     * @param file the {@link File} to delete
     * @throws FileUtilitiesException if deleting the file fails
     */
    private void deleteUnchecked(File file) throws FileUtilitiesException {
        if (file.isDirectory()) {
            File[] nestedFiles = file.listFiles();
            for (int i = 0; i < nestedFiles.length; i++) {
                deleteUnchecked(nestedFiles[i]);
            }
        }
        try {
            Files.delete(file.toPath());
        } catch (IOException | SecurityException e) {
            throw new FileUtilitiesException("Deleting file \"" + file.getAbsolutePath() 
                    + " failed", e);
        }
    }
    
}
