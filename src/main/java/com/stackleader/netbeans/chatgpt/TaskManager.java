/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
import com.redbus.store.MapDBVectorStore;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskManager {
    private static TaskManager instance;
    private final List<Task> tasks;
    final MapDBVectorStore taskStore;

    private TaskManager(MapDBVectorStore taskStore) {
        tasks = new ArrayList<>();
         loadTasksFromDB(taskStore); // Load tasks from DB during initialization
        this.taskStore = taskStore;
    }

    // Singleton instance getter
    public static synchronized TaskManager getInstance(MapDBVectorStore taskStore) {
        if (instance == null) {
            instance = new TaskManager(taskStore);
        }
        return instance;
    }

      // Load tasks from database
    private void loadTasksFromDB(MapDBVectorStore taskStore) {
        List<Task> storedTasks = taskStore.getAllTasks();
        if (storedTasks != null) {
            tasks.addAll(storedTasks);
        }
    }
    // Add a new task
    public synchronized void addTask(String description, String filePath) {
        tasks.add(new Task(description, filePath, TaskStatus.PENDING));
    }

    // Add a task with additional parameters
    public synchronized Task  addTask(String description, String filePath, int lineNumber, String severity, List<String> tags) {
        Task task = new Task(description, filePath, lineNumber, severity, tags, TaskStatus.PENDING);
        tasks.add(task);
        //taskStore.addTask(task);
        return task;
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
        taskStore.removeTask(String.valueOf(taskId));
        
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

    // Parse tasks from a JSON string and add them to the task list
    public synchronized LinkedList<Task> parseTasksFromJson(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        
        JSONArray issuesArray =jsonObject.has("issues")? jsonObject.getJSONArray("issues"):new JSONArray();
        
        LinkedList<Task> taskList=new LinkedList<>();

        for (int i = 0; i < issuesArray.length(); i++) {
            JSONObject issue = issuesArray.getJSONObject(i);

            int lineNumber =-1 ;
            try{
                lineNumber=issue.getInt("lineNumber");
            }catch (Exception ex){}
            String severity =issue.has("severity")? issue.get("severity").toString():"-NA-";
            String description = issue.getString("description");
            String filePath = jsonObject.getString("filePath");

            JSONArray tagsArray =jsonObject.has("tags")?  issue.getJSONArray("tags"):new JSONArray();
            List<String> tags = new ArrayList<>();
            for (int j = 0; j < tagsArray.length(); j++) {
                tags.add(tagsArray.getString(j));
            }

           Task newTask= addTask(description, filePath, lineNumber, severity, tags);
           taskList.add(newTask);
        }
        
        return taskList;
    }
}
