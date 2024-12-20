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

class Task {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private final int id;
    private final String description;
    private final String codeFile;
    private TaskStatus status;

    public Task(String description,String codeFile, TaskStatus status) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.description = description;
        this.status = status;
        this.codeFile=codeFile;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getCodeFile() {
        return codeFile;
    }
    
    
}
