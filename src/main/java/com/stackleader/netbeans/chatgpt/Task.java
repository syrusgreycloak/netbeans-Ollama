/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
import java.util.concurrent.atomic.AtomicInteger;

import java.util.List;

public class Task implements java.io.Serializable  {
    private int lineNumber;
    private String severity;
    private String description;
    private List<String> tags;
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private final int id;
    private String codeFile;
    private TaskStatus status;

    public Task(int lineNumber, String severity, String description, List<String> tags) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.lineNumber = lineNumber;
        this.severity = severity;
        this.description = description;
        this.tags = tags;
    }

    Task(String description, String filePath, TaskStatus taskStatus) {
        
         this.id = ID_GENERATOR.getAndIncrement();
        this.description = description;
        this.status = taskStatus;
        this.codeFile=filePath;
    }

    Task(String description, String filePath, int lineNumber, String severity, List<String> tags, TaskStatus taskStatus) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.description = description;
        this.status = taskStatus;
        this.codeFile=filePath;
        this.lineNumber=lineNumber;
        this.severity=severity;
         this.tags = tags;
    }

    // Getters and setters
    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getId() {
        return id;
    }

    public String getCodeFile() {
        return codeFile;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    

    @Override
    public String toString() {
        return "Task{" +
                "lineNumber=" + lineNumber +
                ", severity='" + severity + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                '}';
    }
}
