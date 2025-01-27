/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;


import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.AdjustmentListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

/**
 *
 * @author manoj.kumar
 */
public class IDEHelper {
    
    
     private static Map<String, FunctionHandler> functionHandlers = new HashMap<>();

    public static Map<String, FunctionHandler> getFunctionHandlers() {
        return functionHandlers;
    }
     
       public static void registerFunction(FunctionHandler handler) {
        functionHandlers.put(handler.getFunctionName(), handler);
    }
    
    
         // Method to show the identified code in RSyntaxTextArea with "Copy to Clipboard" option
    public static void showCodeInPopup(String code, String language) {
        SwingUtilities.invokeLater(() -> {
            // Create RSyntaxTextArea with appropriate syntax style
            RSyntaxTextArea textArea = new RSyntaxTextArea(30, 120);
            textArea.setText(code);
            textArea.setEditable(false);
            textArea.setSyntaxEditingStyle(getSyntaxStyle(language));
            textArea.setCodeFoldingEnabled(true);

            // Create a scroll pane for the text area
            RTextScrollPane scrollPane = new RTextScrollPane(textArea);
            scrollPane.setFoldIndicatorEnabled(true);

            // Create custom buttons for the dialog
            Object[] options = {"Copy", "Close"};

            // Show the dialog with RSyntaxTextArea embedded
            int result = JOptionPane.showOptionDialog(
                    null, scrollPane, "Code Block Detected (" + language + ")",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]
            );

            // Handle the user's selection
            if (result == JOptionPane.YES_OPTION) {
                copyToClipboard(code);
                JOptionPane.showMessageDialog(null, "Code copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

          // Method to show the identified code in RSyntaxTextArea with "Copy to Clipboard" option
    public static void showCodeInPopup(String code, String language, Map<String, Object> codeMap) {
        SwingUtilities.invokeLater(() -> {
            // Create JTable from the HashMap
            DefaultTableModel tableModel = new DefaultTableModel();
            tableModel.addColumn("Key");
            tableModel.addColumn("Value");

            for (Map.Entry<String, Object> entry : codeMap.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }

            JTable table = new JTable(tableModel);
            table.setPreferredScrollableViewportSize(new Dimension(300, 200));
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            // Create a scroll pane for the table
            JScrollPane scrollPanet = new JScrollPane(table);

            // Create RSyntaxTextArea with appropriate syntax style
            RSyntaxTextArea textArea = new RSyntaxTextArea(30, 120);
            textArea.setText(code);
            textArea.setEditable(false);
            textArea.setSyntaxEditingStyle(getSyntaxStyle(language));
            textArea.setCodeFoldingEnabled(true);

            // Create a scroll pane for the text area
            RTextScrollPane scrollPane = new RTextScrollPane(textArea);
            scrollPane.setFoldIndicatorEnabled(true);

            // Add an input field below the table
            JTextField inputField = new JTextField("Prompt goes here...");
            JPanel tablePanelWithInput = new JPanel(new BorderLayout());
            tablePanelWithInput.add(scrollPanet, BorderLayout.CENTER);
            tablePanelWithInput.add(inputField, BorderLayout.SOUTH);

            // Create a JPanel to hold both scroll panes
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(scrollPane, BorderLayout.NORTH); // Add table scroll pane on top
            panel.add(tablePanelWithInput, BorderLayout.CENTER); // Add text area scroll pane in the center

            // Create custom buttons for the dialog
            Object[] options = {"Copy", "Test Tools Call", "Close"};

            // Show the dialog with RSyntaxTextArea embedded
            int result = JOptionPane.showOptionDialog(
                    null, panel, "Code Block Detected (" + language + ")",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]
            );

            // Handle the user's selection
            if (result == JOptionPane.YES_OPTION) {
                copyToClipboard(code);
                JOptionPane.showMessageDialog(null, "Code copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else if (result == 1) { // Custom Function button clicked
                java.util.List<ChatMessage> messages = new CopyOnWriteArrayList<>();
                final ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), inputField.getText());
                messages.add(userMessage);
                JSONArray tools = new JSONArray();
                tools.put(new JSONObject(code));
                String llmResp = OllamaHelpers.callLLMChat(null, OllamaHelpers.selectModelName(), messages, tools, textArea).getJSONObject("message").getString("content");
                JOptionPane.showMessageDialog(null, llmResp, "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
    // Method to determine syntax style based on language string
    public static String getSyntaxStyle(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return SyntaxConstants.SYNTAX_STYLE_JAVA;
            case "python":
                return SyntaxConstants.SYNTAX_STYLE_PYTHON;
            case "javascript":
                return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
            case "xml":
                return SyntaxConstants.SYNTAX_STYLE_XML;
            case "html":
                return SyntaxConstants.SYNTAX_STYLE_HTML;
            case "c":
            case "cpp":
                return SyntaxConstants.SYNTAX_STYLE_C;
              case "json":
                return SyntaxConstants.SYNTAX_STYLE_JSON;  
            // Add more cases as needed for other languages
            default:
                return SyntaxConstants.SYNTAX_STYLE_NONE;
        }
    }

  public static void listAllFiles(StringBuilder output) {
    Mutex.EVENT.readAccess(() -> {
        // Get the currently opened projects
        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();

        if (openProjects.length > 0) {
            Project activeProject = openProjects[0];  // Assuming you want the first active project

            // Get the root folder of the active project
            FileObject projectDirectory = activeProject.getProjectDirectory();

            // Recursively list all files
            listFilesRecursively(projectDirectory, output);
        } else {
            System.out.println("No active projects found.");
        }
        return null;  // Return value is required for readAccess
    });
}

    private static void listFilesRecursively(FileObject folder,StringBuilder output) {
        for (FileObject file : folder.getChildren()) {
            if (file.isFolder()) {
                listFilesRecursively(file,output);  // Recursively list files in subdirectories
            } else {
                //System.out.println("File: " + file.getPath());
                output.append(file.getPath()).append("\n");
            }
        }
    }
    
        public void detectProjectLanguage() {
        // Get the currently opened projects
        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();

        if (openProjects.length > 0) {
            Project activeProject = openProjects[0];  // Assuming you want the first active project
            FileObject projectDirectory = activeProject.getProjectDirectory();
            
            // Scan the project files to detect the language
            String language = detectLanguageByFileExtension(projectDirectory);
            if (language != null) {
                System.out.println("Detected Project Language: " + language);
            } else {
                System.out.println("Language could not be determined.");
            }
        } else {
            System.out.println("No active projects found.");
        }
    }

    private String detectLanguageByFileExtension(FileObject folder) {
        for (FileObject file : folder.getChildren()) {
            if (file.isFolder()) {
                // Recursively check subdirectories
                String language = detectLanguageByFileExtension(file);
                if (language != null) {
                    return language;
                }
            } else {
                // Check file extension to determine language
                String ext = file.getExt();
                switch (ext) {
                    case "java":
                        return "Java";
                    case "cpp":
                    case "h":
                        return "C++";
                    case "py":
                        return "Python";
                    case "js":
                        return "JavaScript";
                    case "html":
                        return "HTML";
                    case "php":
                        return "PHP";
                    // Add more file extensions for other languages as needed
                    default:
                        break;
                }
            }
        }
        return null;
    }
    
    //TEST THE METHODS
        public static void main(String[] args) {
        // Step 1: Register the FilesList handler
        FilesList filesListHandler = new FilesList();
        IDEHelper.registerFunction(filesListHandler);

        // Step 2: Test the listAllFiles functionality through the FilesList handler
        System.out.println("=== Testing listAllFiles ===");
        FunctionHandler filesHandler = IDEHelper.getFunctionHandlers().get("projetc_files_list");
        
        if (filesHandler != null) {
            // Simulate passing empty JSON arguments since no variables are used
            JSONObject arguments = new JSONObject();
            String result = filesHandler.execute(arguments);
            System.out.println(result);  // Output the list of files
        } else {
            System.out.println("FilesList handler not found.");
        }

        // Step 3: Test the detectProjectLanguage method
        System.out.println("=== Testing detectProjectLanguage ===");
        IDEHelper ideHelperInstance = new IDEHelper();
        ideHelperInstance.detectProjectLanguage();  // Detect and print the language
        
        System.out.println("=== This Project ===");
        JTextComponent editorPane = EditorRegistry.lastFocusedComponent();  
        System.out.println(editorPane.getDocument().getLength());
       
        Project thisProject=OpenProjects.getDefault().getOpenProjects()[0];
        System.out.println(thisProject.getProjectDirectory().getPath());
    }
    
    /**
     * Displays a dialog with a JComboBox of open projects, allowing the user to select one.
     * 
     * @return The selected Project, or null if no project was selected.
     */
    public static Project selectProjectFromOpenProjects() {
        // Retrieve the open projects
        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();

        // Check if there are any open projects
        if (openProjects.length == 0) {
            JOptionPane.showMessageDialog(null, "No open projects found.");
            return null;
        }

        // Create a JComboBox with the project names as options
        JComboBox<Project> projectComboBox = new JComboBox<>(openProjects);

        // Set a custom renderer to display project names instead of object references
        projectComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                // Assuming each Project object has a method getDisplayName() to show project name
                if (value instanceof Project) {
                    Project project = (Project) value;
                    setText(project.getProjectDirectory().getName()); // Adjust as per Project API
                }
                return this;
            }
        });

        // Show the JComboBox in a JOptionPane dialog
        int result = JOptionPane.showConfirmDialog(
                null, projectComboBox, "Select a Project", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            return (Project) projectComboBox.getSelectedItem();
        } else {
            return null;
        }
    }
    
    
    
    //Visuals
    
    public static JSplitPane createCodeComparisonPane(String code1, String code2, String syntaxStyle) {
        // Create RSyntaxTextAreas for both panels
        RSyntaxTextArea textArea1 = createSyntaxTextArea(syntaxStyle);
        RSyntaxTextArea textArea2 = createSyntaxTextArea(syntaxStyle);

        // Set the code content
        textArea1.setText(code1);
        textArea2.setText(code2);

        // Create scroll panes for the text areas
        RTextScrollPane scrollPane1 = new RTextScrollPane(textArea1);
        RTextScrollPane scrollPane2 = new RTextScrollPane(textArea2);

        // Synchronize scrolling
        synchronizeScrollPanes(scrollPane1.getVerticalScrollBar(), scrollPane2.getVerticalScrollBar());

        // Create and return a split pane to hold the two scroll panes
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane1, scrollPane2);
        splitPane.setDividerLocation(600); // Set initial divider position
        return splitPane;
    }

    private static RSyntaxTextArea createSyntaxTextArea(String syntaxStyle) {
        RSyntaxTextArea textArea = new RSyntaxTextArea(30,120);
        textArea.setSyntaxEditingStyle(syntaxStyle);
        textArea.setCodeFoldingEnabled(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    private static void synchronizeScrollPanes(JScrollBar bar1, JScrollBar bar2) {
        AdjustmentListener listener = e -> {
            JScrollBar source = (JScrollBar) e.getSource();
            JScrollBar target = (source == bar1) ? bar2 : bar1;
            target.setValue(source.getValue());
        };

        bar1.addAdjustmentListener(listener);
        bar2.addAdjustmentListener(listener);
    }
    
     public static JDialog createCodeComparisonDialog( JFrame dummyParent, String code1, String code2, String syntaxStyle) {
        // Create a modal dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(dummyParent), "Code Comparison", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1024, 800);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Create RSyntaxTextAreas for both panels
        RSyntaxTextArea textArea1 = createSyntaxTextArea(syntaxStyle);
        RSyntaxTextArea textArea2 = createSyntaxTextArea(syntaxStyle);

        // Set the code content
        textArea1.setText(code1);
        textArea2.setText(code2);

        // Create scroll panes for the text areas
        RTextScrollPane scrollPane1 = new RTextScrollPane(textArea1);
        RTextScrollPane scrollPane2 = new RTextScrollPane(textArea2);

        // Synchronize scrolling
        synchronizeScrollPanes(scrollPane1.getVerticalScrollBar(), scrollPane2.getVerticalScrollBar());

        // Create and add a split pane to hold the two scroll panes
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane1, scrollPane2);
        splitPane.setDividerLocation(600); // Set initial divider position
        dialog.add(splitPane, BorderLayout.CENTER);

        // Add an OK button to close the dialog
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        return dialog;
    }
     
         public static String prefixLineNumbers(String inputFilePath) throws IOException {
        Path inputPath = Paths.get(inputFilePath);

        // Ensure the input file exists
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("Input file not found: " + inputFilePath);
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                result.append("Line ").append(lineNumber).append(": ").append(line).append(System.lineSeparator());
                lineNumber++;
            }
        }
        return result.toString();
    }
         
                   // Method to copy code to the system clipboard
    public static void copyToClipboard(String code) {
        StringSelection stringSelection = new StringSelection(code);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

}

class FilesList implements FunctionHandler {

    @Override
    public String getFunctionName() {
        return "projetc_files_list";
    }

    @Override
    public String getDescription() {
        return "List all the files in the provided project.";
    }

    @Override
    public JSONObject getFunctionJSON() {
        JSONObject functionObject = new JSONObject();
        functionObject.put("name", getFunctionName());
        functionObject.put("description", getDescription());

        JSONObject parametersObject = new JSONObject();
        parametersObject.put("type", "object");

        JSONObject propertiesObject = new JSONObject();

        for (VariableDefinition variable : getVariables().values()) {
            propertiesObject.put(variable.getName(), variable.toJSON());
        }

        parametersObject.put("properties", propertiesObject);
        parametersObject.put("required", new JSONArray(getVariables().keySet()));

        functionObject.put("parameters", parametersObject);

        return functionObject;
    }

    @Override
    public Map<String, VariableDefinition> getVariables() {
        Map<String, VariableDefinition> variables = new HashMap<>();
//        variables.put("length", new VariableDefinition("length", "number", "The length of the cuboid.", null));
//        variables.put("width", new VariableDefinition("width", "number", "The width of the cuboid.", null));
//        variables.put("height", new VariableDefinition("height", "number", "The height of the cuboid.", null));
        return variables;
    }

    @Override
    public String execute(JSONObject arguments) {
//        double length = arguments.getDouble("length");
//        double width = arguments.getDouble("width");
//        double height = arguments.getDouble("height");
//
//        double volume = length * width * height;
        StringBuilder output = new StringBuilder();
        output.append("** List of files ***").append("\n");

        IDEHelper.listAllFiles(output);
        output.append("\n*****").append("\n");

        return output.toString();
    }
    
        public static String removeLineNumbers(String content) {
        // Split the content into lines, process each line to remove the prefix, and join them back
        return content.lines()
                .map(line -> line.replaceFirst("^Line \\d+:\\s*", "")) // Regex to match and remove "Line N: "
                .collect(Collectors.joining(System.lineSeparator()));
    }
        
 
    
    
}
