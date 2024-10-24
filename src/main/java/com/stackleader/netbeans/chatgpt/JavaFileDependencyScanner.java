/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaFileDependencyScanner {

    // Method to recursively scan a folder and find all Java files and their dependencies
    public static Map<String, List<String>> scanJavaFiles(File folder) {
        Map<String, List<String>> fileDependenciesMap = new HashMap<>();

        // Recursively scan for Java files in the folder
        scanFolderForJavaFiles(folder, fileDependenciesMap);

        return fileDependenciesMap;
    }

    // Recursively scans the folder and processes Java files
    private static void scanFolderForJavaFiles(File folder, Map<String, List<String>> fileDependenciesMap) {
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    // Recursively scan subdirectories
                    scanFolderForJavaFiles(file, fileDependenciesMap);
                } else if (file.getName().endsWith(".java")) {
                    // Process Java file
                    List<String> dependencies = getJavaFileDependencies(file);
                    fileDependenciesMap.put(file.getPath(), dependencies);
                }
            }
        }
    }

    // Method to extract dependencies from a Java file (based on import statements)
    private static List<String> getJavaFileDependencies(File javaFile) {
        List<String> dependencies = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Look for lines that start with "import " to identify dependencies
                line = line.trim();
                if (line.startsWith("import ")) {
                    String importedClass = line.substring(7, line.length() - 1); // Remove "import" and semicolon
                    dependencies.add(importedClass);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dependencies;
    }

    // Main method to test the utility
    public static void main(String[] args) {
        File projectFolder = new File("/path/to/your/project"); // Change this to your project folder

        // Scan the folder and get the map of file dependencies
        Map<String, List<String>> result = scanJavaFiles(projectFolder);

        // Print the results
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            System.out.println("File: " + entry.getKey());
            System.out.println("Depends on:");
            for (String dependency : entry.getValue()) {
                System.out.println("  - " + dependency);
            }
            System.out.println();
        }
    }
}
