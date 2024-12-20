/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskManager {
    private static TaskManager instance;
    private final List<Task> tasks;

    private TaskManager() {
        tasks = new ArrayList<>();
    }

    // Singleton instance getter
    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    // Add a new task
    public synchronized void addTask(String description, String FilePath) {
        tasks.add(new Task(description,FilePath, TaskStatus.PENDING));
    }

    // Update a task's status
    public synchronized void updateTaskStatus(int taskId, TaskStatus newStatus) {
        tasks.stream()
                .filter(task -> task.getId() == taskId)
                .findFirst()
                .ifPresent(task -> task.setStatus(newStatus));
    }

    // Remove a task by ID
    public synchronized void removeTask(int taskId) {
        tasks.removeIf(task -> task.getId() == taskId);
    }

    // Search tasks by description (case-insensitive)
    public List<Task> searchTasks(String keyword) {
        return tasks.stream()
                .filter(task -> task.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    // Automatically verify task completion
    public synchronized void verifyTaskCompletion() {
        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.PENDING) {
                if (checkIfTaskIsCompleted(task)) {
                    task.setStatus(TaskStatus.COMPLETED);
                }
            }
        }
    }

    // Custom logic to verify if a task is completed
    private boolean checkIfTaskIsCompleted(Task task) {
        // Replace this with your actual verification logic
        return task.getDescription().toLowerCase().contains("done");
    }
}
