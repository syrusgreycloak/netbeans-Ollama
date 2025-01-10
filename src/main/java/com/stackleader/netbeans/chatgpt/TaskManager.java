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
import static com.stackleader.netbeans.chatgpt.FilesList.copyToClipboard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.openide.util.Exceptions;

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
    
     public synchronized Task getTask(int taskId) {
        //.removeIf(task -> task.getId() == taskId);
        return taskStore.getTask(String.valueOf(taskId));
        
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
    public synchronized LinkedList<Task> parseTasksFromJson(JSONObject jsonObject) {
       // JSONObject jsonObject = new JSONObject(jsonString);
        
        JSONArray issuesArray =jsonObject.has("issues")? jsonObject.getJSONArray("issues"):new JSONArray();
        
        LinkedList<Task> taskList=new LinkedList<>();

        for (int i = 0; i < issuesArray.length(); i++) {
            JSONObject issue = issuesArray.getJSONObject(i);

            int lineNumber =-1 ;//To be implemented properly. 
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
 
    public void processTasks(MapDBVectorStore taskStore, String selectedModel) {

        TaskManager taskManager = TaskManager.getInstance(taskStore);

// Fetch tasks
        List<Task> pendingTasks = taskManager.getAllTasks();

        for (Task task : pendingTasks) {
           processTasks(  task,  selectedModel);
        }
    }
    
     public void processTasks(Task task, String selectedModel) {

  
            try {
                // Verify bug
                if (verifyBug(task,selectedModel)) {
                    String fix = suggestFix(task,selectedModel);

                    // Apply fix and compile
                    applyCodeChange(task.getCodeFile(),"("+task.getSeverity()+"@"+task.getLineNumber()+")"+task.getDescription(), fix);
                    if (compileCode(task.getCodeFile())) {

                        // Add and run test cases
                        String testCode = generateTestCase(task,selectedModel);
                        String testFilePath = task.getCodeFile().replace(".java", "Test.java"); //Find a better location fo this
                        addTestCase(testFilePath, testCode);
                        if (runTests(testFilePath)) {
                           updateTaskStatus(task.getId(), TaskStatus.COMPLETED);
                        } else {
                            throw new RuntimeException("Tests failed");
                        }
                        
                    } else {
                        throw new RuntimeException("Compilation failed");
                    }
                } else {
                    
                    JOptionPane.showMessageDialog(null, "("+task.getSeverity()+"@"+task.getLineNumber()+")"+task.getDescription(), "Not an issue", JOptionPane.CLOSED_OPTION);
                    // taskManager.updateTaskStatus(task.getId(), TaskStatus.INVALID);
                    task.setStatus(TaskStatus.INVALID);
                    //Also add it...
                }
            } catch (Exception e) {
                // taskManager.updateTaskStatus(task.getId(), TaskStatus.FAILED);
                task.setStatus(TaskStatus.FAILED);
            }
        
    }

    public boolean runTests(String testFilePath) {
        try {
           // org.junit.runner.JUnitCore.runClasses(Class.forName(testFilePath));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void addTestCase(String testFilePath, String testCode) throws IOException {
        Path path = Paths.get(testFilePath);
       // Files.writeString(path, testCode, StandardOpenOption.CREATE_NEW); //Yet to do it right.
    }

    public String generateTestCase(Task task, String selectedModel) {
        try {
            String fileContent = Files.readString(Paths.get(task.getCodeFile()));
            String codeSnippet = fileContent;
            
            String testPrompt = "Generate a JUnit test case for the following code snippet:\n" + codeSnippet;
            
            JSONObject codeSummary = OllamaHelpers.makeNonStreamedRequest(selectedModel, testPrompt,false);
            
            String aiResponse=codeSummary.getString("response");
            
            return aiResponse;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public boolean compileCode(String filePath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, filePath);
        return result == 0; // 0 indicates success
    }

    public void applyCodeChange(String filePath, String issue, String updatedCode)  {
        try {
            // Path path = Paths.get(filePath);
            //Files.writeString(path, updatedCode, StandardOpenOption.TRUNCATE_EXISTING);
            ///
            String fileContent = Files.readString(Paths.get(filePath));
            
            String code1 = fileContent;
            String code2 = updatedCode;
            String language = updatedCode.split("\\r?\\n")[0];
                updatedCode = updatedCode.substring(language.length());
            //JFrame dummyParent = new JFrame(); // Parent for modal dialog (can be null)
            //dummyParent.setVisible(true);
            
            //IDEHelper.createCodeComparisonDialog(dummyParent,  code1,  code2,  SyntaxConstants.SYNTAX_STYLE_JAVA);
            
            // Create custom buttons for the dialog
            Object[] options = {"Copy", "Close"};
            //SwingUtilities.invokeLater(() -> {
            // Show the dialog with RSyntaxTextArea embedded
            int result = JOptionPane.showOptionDialog(
                    null, IDEHelper.createCodeComparisonPane(  code1,  updatedCode,  SyntaxConstants.SYNTAX_STYLE_JAVA), "Issue:"+issue,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0] );
            
            // Handle the user's selection
            if (result == JOptionPane.YES_OPTION) {
                copyToClipboard(updatedCode);
                JOptionPane.showMessageDialog(null, "Code copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            //});
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } }

    public String suggestFix(Task task, String selectedModel) {
        try {
            String fileContent = Files.readString(Paths.get(task.getCodeFile()));
            String codeSnippet = fileContent;
            String bugDescription = task.getDescription();
            
            
            String fixPrompt = "Suggest a fix for the following bug in the given code snippet:\n"
                    + "Bug Description: " + bugDescription + "\n"
                    + "Code Snippet:\n" + codeSnippet;
            
            JSONObject codeSummary = OllamaHelpers.makeNonStreamedRequest(selectedModel, fixPrompt,false);
            
            String aiResponse=codeSummary.getString("response");
            
            return aiResponse;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public boolean verifyBug(Task task, String selectedModel ) {
        try {
            String fileContent = Files.readString(Paths.get(task.getCodeFile()));
            String codeSnippet = fileContent;
            String bugDescription = task.getDescription();
            
            String verificationPrompt = "Respond in JSON Schema format {hasBug: 'true'\'false'}.\n Verify if the following bug is present in the given code snippet:\n"
                    + "Bug Description: " + bugDescription + "\n"
                    + "Code Snippet:\n" + codeSnippet;
            
            JSONObject codeSummary = OllamaHelpers.makeNonStreamedRequest(selectedModel, verificationPrompt,true);
            
            String aiResponse=codeSummary.getString("response");
            JSONObject hasBug=new JSONObject(aiResponse);
            
            //String aiResponse = aiService.ask(verificationPrompt);
            return hasBug.has("hasBug")?hasBug.getBoolean("hasBug"):false;//aiResponse.contains("Bug exists");
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }
    
  public static void main(String... args){
      
      //Say Hi to LLM
       String verificationPrompt = "Say Hi!" ;
            
            JSONObject codeSummary;
        try {
            codeSummary = OllamaHelpers.makeNonStreamedRequest("llama3.2:1b", verificationPrompt,false);
            // String aiResponse=codeSummary.getString("response");
             System.out.println(codeSummary.toString(1));
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
            
           
      
      // Create a JFileChooser instance
        JFileChooser fileChooser = new JFileChooser();

        // Set the title for the dialog
        fileChooser.setDialogTitle("Select a File");

        // Show the file chooser dialog
        int userSelection = fileChooser.showOpenDialog(null);

        // Process the user's selection
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected File: " + selectedFile.getAbsolutePath());
            
            MapDBVectorStore store=new MapDBVectorStore(selectedFile.getAbsolutePath());
            TaskManager.getInstance(store).processTasks(store, "llama3.2:1b");
            
        } else {
            System.out.println("No file selected");
        }
      
  }
}
