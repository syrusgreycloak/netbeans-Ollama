package com.stackleader.netbeans.chatgpt;

import com.redbus.store.ChatSimilarityResult;
import com.redbus.store.MapDBVectorStore;
import com.redbus.store.PDFReaderUtils;
import static com.stackleader.netbeans.chatgpt.FilesList.copyToClipboard;
import static com.stackleader.netbeans.chatgpt.JavaFileDependencyScanner.scanJavaFiles;
import static com.stackleader.netbeans.chatgpt.OllamaHelpers.callLLMVision;
import static com.stackleader.netbeans.chatgpt.OllamaHelpers.convertImageToBase64;
import static com.stackleader.netbeans.chatgpt.OllamaHelpers.createJsonPayload;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import io.reactivex.functions.Consumer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.json.JSONObject;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.cookies.EditorCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.util.Utilities;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author dcnorris
 */
@TopComponent.Description(
        preferredID = ChatTopComponent.PREFERRED_ID,
        iconBase = "icons/chat.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = true, position = 500)
@ActionID(category = "Window", id = "com.stackleader.netbeans.chatgpt.ChatTopComponent")
@ActionReference(path = "Menu/Window", position = 333)
@TopComponent.OpenActionRegistration(
        displayName = "#Chat_TopComponent_Title",
        preferredID = ChatTopComponent.PREFERRED_ID)
public class ChatTopComponent extends TopComponent {

    System.Logger LOG = System.getLogger("ChatTopComponent");

    public static final String PREFERRED_ID = "ChatTopComponent";
    private static final int BOTTOM_PANEL_HEIGHT = 70;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 25;
    private static final int ACTIONS_PANEL_WIDTH = 20;
    public static final String QUICK_COPY_TEXT = "(Quick Copy: Ctrl+Click here)";

    private RSyntaxTextArea outputTextArea;
    JEditorPane editorPane;
    private final static List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private final static Parser parser = Parser.builder().build();
    private final static HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    private Gutter gutter;
    private boolean shouldAnnotateCodeBlock = true;
    private JComboBox<String> modelSelection;
    private OpenAiService service;
    MapDBVectorStore store;
    private static final RequestProcessor RP = new RequestProcessor(ChatTopComponent.class);
    DefaultTableModel taskTableModel ;

    public ChatTopComponent() {
        setName(NbBundle.getMessage(ChatTopComponent.class, "Chat_TopComponent_Title")); // NOI18N
        setLayout(new BorderLayout());
        Configuration config = Configuration.getInstance();
        Preferences prefs = config.getPreferences();
        String token = prefs.get(Configuration.OPENAI_TOKEN_KEY, null);
        if (token == null || token.isBlank()) {
            token = promptForToken();
            if (token != null && !token.isBlank()) {
                prefs.put(Configuration.OPENAI_TOKEN_KEY, token);
            } else {
                add(createMissingTokenBanner(), BorderLayout.CENTER);
                return;
            }
        }

        //Set Plugin Home Directory
        String pluginHomeDir = prefs.get(Configuration.PLUGIN_HOME_DIR, null);
        if (pluginHomeDir == null || pluginHomeDir.isEmpty()) {
            // Set a default home directory if none exists
            pluginHomeDir = System.getProperty("user.home") + File.separator + "MyPluginData";
            prefs.put(Configuration.PLUGIN_HOME_DIR, pluginHomeDir);
        }

        File pluginDir = new File(pluginHomeDir);

        if (!pluginDir.exists()) {
            if (pluginDir.mkdirs()) {
                System.out.println("Plugin home directory created at: " + pluginHomeDir);
            } else {
                System.err.println("Failed to create plugin home directory at: " + pluginHomeDir);
                return; // Exit if the directory cannot be created
            }
        } else {
            System.out.println("Using existing plugin home directory at: " + pluginHomeDir);
        }

        // Continue with plugin initialization
        System.out.println("Plugin initialized with home directory: " + pluginHomeDir);
        
        //Initiate Vector Store
        store = new MapDBVectorStore(pluginHomeDir + "/MKVECOLLAMA.db");

        
        //Task List
        //Task Table
        String[] columnNames = {"Task", "Description","Code File","lineNumber","severity","tags", "Status"};//description, filePath, lineNumber, severity, tags, TaskStatus.PENDING
        taskTableModel = new DefaultTableModel(columnNames, 0);

        addComponentsToFrame();
        service = new OpenAiService(token);

        //Function calls
        IDEHelper.registerFunction(new FilesList());

        
        // String currentDirectory = System.getProperty("user.dir");
        appendText("Plugin working directory: " + pluginHomeDir + "\n");
        appendText("Necessary models: nomic-embed-text, llama3.2:1b, llama3.2-vision  " + pluginHomeDir + "\n");
        //nomic-embed-text
        //llama3.2-vision
        //llama3.2:1b
        appendText("Vector store : MKVECOLLAMA.db \n");

        //Get Project Info
        // Get the currently opened projects
        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
        appendText("Open Projects that I can access:\n");
        for (Project project : openProjects) {
            // JOptionPane.showConfirmDialog(outputTextArea,"Open Projects: "+ OpenProjects.getDefault().getOpenProjects()[0].getProjectDirectory().getPath());//Testing here
            appendText(project.getProjectDirectory().getName() + ":" + project.getProjectDirectory().getPath() + "\n");
        }
        
       

    }

    private String promptForToken() {
        NotifyDescriptor.InputLine inputLine = new NotifyDescriptor.InputLine(
                "Enter OpenAI API Token:",
                "API Token Required"
        );
        if (DialogDisplayer.getDefault().notify(inputLine) == NotifyDescriptor.OK_OPTION) {
            return inputLine.getInputText();
        }
        return null;
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
        setName(NbBundle.getMessage(ChatTopComponent.class,
                "Chat_TopComponent_Title")); // NOI18N
    }

    private void addComponentsToFrame() {
        add(createActionsPanel(), BorderLayout.WEST);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createTabbedPane(), BorderLayout.CENTER); // createOutputJEditorPane()  createOutputScrollPane()
        //createOutputScrollPane();//Place holder code, remove afterwards.
        //mainPanel.add(createOutputJEditorPane(), BorderLayout.EAST); // createOutputJEditorPane()  createOutputScrollPane()
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createMissingTokenBanner() {
        JPanel missingTokenBanner = new JPanel();
        JLabel messageLabel = new JLabel("<html><center>OPENAI_TOKEN environment variable is not defined.<br>Please restart NetBeans after defining this variable.</center></html>");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        missingTokenBanner.setLayout(new BorderLayout());
        missingTokenBanner.add(messageLabel, BorderLayout.CENTER);

        return missingTokenBanner;
    }

    private JPanel createActionsPanel() {
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setPreferredSize(new Dimension(ACTIONS_PANEL_WIDTH, 500));
        JButton optionsButton = new JButton(ImageUtilities.loadImageIcon("icons/options.png", true));
        optionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOptionsButtonClick();
            }
        });
        actionsPanel.add(optionsButton);

        //Index action
        JButton indexButton = new JButton(ImageUtilities.loadImageIcon("icons/index.png", true));
        indexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                indexProject();
            }
        });
        actionsPanel.add(indexButton);

        //Index action
        JButton bugLocatorButton = new JButton(ImageUtilities.loadImageIcon("icons/mite.png", true));
        bugLocatorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                detectBugs();
            }
        });
        actionsPanel.add(bugLocatorButton);

        //PDF action
        JButton pdfaddButton = new JButton(ImageUtilities.loadImageIcon("icons/pdf16x16.png", true));
        pdfaddButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String selectedModel = (String) modelSelection.getSelectedItem();

                File selectedFile = PDFReaderUtils.selectPDFFile();
                if (selectedFile != null) {
                    // String pdfContent = PDFReaderUtils.extractTextFromPDF(selectedFile);
                    String[] pdfPara = PDFReaderUtils.extractTextInChunks(selectedFile, 256);
//                    if (pdfContent != null) {
//                        System.out.println("PDF Content:\n" + pdfContent);
//                        appendText(pdfContent);
//                        
//                    } else {
//                        System.out.println("Failed to extract text from the selected PDF.");
//                    }

                    // Paragraph printing for comparision
                    appendText(selectedModel + " is being used ... \n");
                    for (String para : pdfPara) {
                        appendText(para);
                        //Add PDF OCR data to memory
                        List<String> chat1 = Arrays.asList(para);
                        double[] embedding1 = OllamaHelpers.getChatEmbedding(chat1); // Similar chat to query
                        chat1 = Arrays.asList(para, selectedFile.getAbsolutePath());
                        //The key for storage is Project-name and the sys-millisec
                        store.storeChat(selectedFile.getName(), chat1, embedding1);
                        appendText("[+m]\n");
                    }

                    appendText(selectedFile.getName() + " [PDF INDEXING COMPLETE] \n");

                } else {
                    System.out.println("No file selected.");
                }
            }
        });
        actionsPanel.add(pdfaddButton);

        //Search action
        JButton searchButton = new JButton(ImageUtilities.loadImageIcon("icons/glass.png", true));
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchIndex();
            }
        });
        actionsPanel.add(searchButton);

        return actionsPanel;
    }

    private void handleOptionsButtonClick() {
        Configuration config = Configuration.getInstance();
        Preferences prefs = config.getPreferences();
        String currentToken = prefs.get(Configuration.OPENAI_TOKEN_KEY, "");

        NotifyDescriptor.InputLine inputLine = new NotifyDescriptor.InputLine(
                "OpenAI API Token:",
                "Edit API Token"
        );
        inputLine.setInputText(currentToken);

        if (DialogDisplayer.getDefault().notify(inputLine) == NotifyDescriptor.OK_OPTION) {
            String newToken = inputLine.getInputText();
            prefs.put(Configuration.OPENAI_TOKEN_KEY, newToken);
            service = new OpenAiService(newToken);
        }
    }

    private JTabbedPane createTabbedPane() {
        // Create a tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Add Output Tab
        JPanel outputPanel = createOutputScrollPane();
        tabbedPane.addTab("Assistant", null, outputPanel, "View Output");

        // Add Task List Tab
        JPanel taskListPanel = createTaskListPanel();
        tabbedPane.addTab("Task List", null, taskListPanel, "Manage Tasks");

        return tabbedPane;
    }

    private JPanel createTaskListPanel() {
        JPanel taskListPanel = new JPanel(new BorderLayout());

 
        
        JTable taskTable = new JTable(taskTableModel);
        JScrollPane taskScrollPane = new JScrollPane(taskTable);
        taskListPanel.add(taskScrollPane, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        //JButton addTaskButton = new JButton("Add Task");
        JButton removeAllTaskButton = new JButton("Purge All Task");
        JButton removeTaskButton = new JButton("Remove Task");
        
        JButton verifyTasksButton = new JButton("Resolve");

        //buttonsPanel.add(addTaskButton);
        buttonsPanel.add(removeAllTaskButton);
        buttonsPanel.add(removeTaskButton);
        buttonsPanel.add(verifyTasksButton);
        taskListPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add Task Button Action
//        addTaskButton.addActionListener(e -> {
//            String taskText = JOptionPane.showInputDialog(taskListPanel, "Enter Task Description:");
//            if (taskText != null && !taskText.trim().isEmpty()) {
//                TaskManager.getInstance(store).addTask(taskText.trim(),"NA");
//                refreshTaskTable(taskTableModel);
//            }
//        });

        // Remove Task Button Action
        removeTaskButton.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow != -1) {
                int taskId = (int) taskTable.getValueAt(selectedRow, 0); // Assuming the task ID is in the first column
                TaskManager.getInstance(store).removeTask(taskId);
                refreshTaskTable(taskTableModel);
            } else {
                JOptionPane.showMessageDialog(taskListPanel, "Please select a task to remove.");
            }
        });
   
         
         removeAllTaskButton.addActionListener(e -> {
    if (taskTableModel.getRowCount() > 0) {
        int result = JOptionPane.showConfirmDialog(taskListPanel, "Are you sure you want to remove all tasks?", "Warning", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            for (int i = taskTableModel.getRowCount() - 1; i >= 0; i--) {
                int taskId = (int) taskTable.getValueAt(i, 0); // Assuming the task ID is in the first column
                TaskManager.getInstance(store).removeTask(taskId);
            }
            refreshTaskTable(taskTableModel);
        }
    } else {
        JOptionPane.showMessageDialog(taskListPanel, "There are no tasks to remove.");
    }
});

        // Verify Tasks Button Action
        verifyTasksButton.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow != -1) {
                int taskId = (int) taskTable.getValueAt(selectedRow, 0); // Assuming the task ID is in the first column
               
            Task task=TaskManager.getInstance(store).getTask(taskId);
            TaskManager.getInstance(store).verifyTaskCompletion();
            refreshTaskTable(taskTableModel);
        
            HyperlinkDemo.openSourceCode(task.getCodeFile(), task.getLineNumber()>0?task.getLineNumber():1);
            
            String selectedModel = (String) modelSelection.getSelectedItem(); // Get the selected model
            TaskManager.getInstance(store).processTasks(  task,  selectedModel);
            
            }
        });

        // Initial Population of Task Table
        refreshTaskTable(taskTableModel);

        return taskListPanel;
    }

    private void refreshTaskTable(DefaultTableModel taskTableModel) {
        taskTableModel.setRowCount(0); // Clear existing rows

        // Populate with current tasks
        TaskManager.getInstance(store).getAllTasks().forEach(task -> {
            // {"Task", "Description","Code File","lineNumber","severity","tags", "Status"}
            taskTableModel.addRow(new Object[]{task.getId(), task.getDescription(),task.getCodeFile(),task.getLineNumber(), task.getSeverity(), String.join(", ", task.getTags()), task.getStatus()});
        });
    }

    private JPanel createOutputScrollPane() {
        JPanel cp = new JPanel(new BorderLayout());
        outputTextArea = createOutputTextArea();
        RTextScrollPane outputScrollPane = new RTextScrollPane(outputTextArea);
        outputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        outputScrollPane.setBorder(new TitledBorder("Output"));
        gutter = outputScrollPane.getGutter();
        cp.add(outputScrollPane);
        return cp;
    }

    private RSyntaxTextArea createOutputTextArea() {
        RSyntaxTextArea outputTextArea = new RSyntaxTextArea();
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Color bg = defaults.getColor("EditorPane.background");
        Color fg = defaults.getColor("EditorPane.foreground");
        Color menuBackground = defaults.getColor("Menu.background");
        Color selectedTextColor = new Color(100, 149, 237);
        outputTextArea.setForeground(fg);
        outputTextArea.setBackground(menuBackground);
        outputTextArea.setSelectedTextColor(selectedTextColor);
        outputTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setLinkGenerator(new CodeBlockLinkGenerator());
        return outputTextArea;
    }

    private JEditorPane createOutputJEditorPane() {
        // Create a JEditorPane
        JEditorPane outputTextArea = new JEditorPane();
        outputTextArea.setContentType("text/html"); // Enable HTML support

        // Get UI defaults
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Color bg = defaults.getColor("EditorPane.background");
        Color fg = defaults.getColor("EditorPane.foreground");
        Color menuBackground = defaults.getColor("Menu.background");
        Color selectedTextColor = new Color(100, 149, 237);

        // Set colors and styles
        outputTextArea.setForeground(fg);
        outputTextArea.setBackground(menuBackground);
        outputTextArea.setEditable(false); // Make it read-only

        // Style with CSS
        String style = String.format(" <style>\n"
                + "                body {\n"
                + "                    background-color: rgb(%d, %d, %d);\n"
                + "                    color: rgb(%d, %d, %d);\n"
                + "                    font-family: Arial, sans-serif;\n"
                + "                    line-height: 1.5;\n"
                + "                }\n"
                + "                a {\n"
                + "                    color: rgb(%d, %d, %d);\n"
                + "                    text-decoration: none;\n"
                + "                }\n"
                + "                a:hover {\n"
                + "                    text-decoration: underline;\n"
                + "                }\n"
                + "            </style>",
                menuBackground.getRed(), menuBackground.getGreen(), menuBackground.getBlue(),
                fg.getRed(), fg.getGreen(), fg.getBlue(),
                selectedTextColor.getRed(), selectedTextColor.getGreen(), selectedTextColor.getBlue()
        );

        // Set initial content with custom CSS
        outputTextArea.setText("<html>" + style + "<body></body></html>");

        // Add a HyperlinkListener for link handling
        outputTextArea.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                System.out.println("Link clicked: " + event.getDescription());
                // Handle link clicks (e.g., navigate or open files)
                handleLinkClick(event.getDescription());
            }
        });

        return outputTextArea;
    }

    /**
     * Handles link clicks from the JEditorPane.
     *
     * @param link The link that was clicked
     */
    private void handleLinkClick(String link) {
        if (link.startsWith("file://")) {
            String filePath = link.substring(7); // Remove "file://"
            System.out.println("Opening file: " + filePath);
            // Implement logic to open the file in NetBeans or another editor
        } else {
            try { //Open in editor?
                //Desktop.getDesktop().browse(new URI(link));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(createButtonPanel(), BorderLayout.WEST);
        bottomPanel.add(createInputScrollPane(), BorderLayout.CENTER);
        bottomPanel.setPreferredSize(new Dimension(0, BOTTOM_PANEL_HEIGHT));
        return bottomPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 5, 5, 5);
        String[] models = OllamaHelpers.mergeArrays(OllamaHelpers.fetchModelNames(), new String[]{"gpt-3.5-turbo-1106", "gpt-3.5-turbo-16k-0613", "gpt-4o"});
        modelSelection = new JComboBox<>(models);
        modelSelection.setSelectedItem(models[0]);
        buttonPanel.add(modelSelection, gbc);
        gbc.gridy++;
        JButton resetButton = createResetButton();
        buttonPanel.add(resetButton, gbc);
        gbc.gridy++;
        JButton imageButton = createImageAddButton();
        buttonPanel.add(imageButton, gbc);

//        gbc.gridy++;
//        JButton submitButton = createSubmitButton();
//        buttonPanel.add(submitButton, gbc);
//        gbc.gridy++;
//        buttonPanel.add(createChatHistoryButton(),gbc);//To load history
//        //Index codebase
//        gbc.gridy++;
//        buttonPanel.add(createIndexButton(),gbc);//To load history
        return buttonPanel;
    }

    private JButton createResetButton() {
        final JButton resetButton = createButton("Session Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
        return resetButton;
    }

    private JButton createImageAddButton() {
        final JButton resetButton = createButton("Image-OCR");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {

                    JFileChooser fileChooser = new JFileChooser();

                    // Set the file filter to allow only image files
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            "Image Files (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"
                    );
                    fileChooser.setFileFilter(filter);

                    // Open the file chooser dialog
                    int result = fileChooser.showOpenDialog(null);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        System.out.println("Selected image file: " + selectedFile.getAbsolutePath());
                        String model = "llama3.2-vision";
                        String userMessage = inputTextArea.getText().isBlank() ? "what is in this image?" : inputTextArea.getText();
                        // Convert image to Base64
                        String base64ImageData = convertImageToBase64(selectedFile.getAbsolutePath());

                        // Create JSON payload
                        String jsonPayload = createJsonPayload(model, userMessage, base64ImageData);

                        // Make the POST request and handle the response
                        String response = callLLMVision(jsonPayload);
                        JSONObject jresp = new JSONObject(response);
                        String llmresp = jresp.getJSONObject("message").getString("content");
                        System.out.println("Response: " + response);
                        appendText("\n" + jresp.getJSONObject("message").getString("content") + "\n");
                        //Add Image OCR data to memory
                        List<String> chat1 = Arrays.asList(llmresp);
                        double[] embedding1 = OllamaHelpers.getChatEmbedding(chat1); // Similar chat to query
                        chat1 = Arrays.asList(llmresp, selectedFile.getAbsolutePath());
                        //The key for storage is Project-name and the sys-millisec
                        store.storeChat(selectedFile.getName(), chat1, embedding1);
                        appendText("[+m]\n");

                        //Display image
                        PDFReaderUtils.displayImageWithLabel(selectedFile, null, llmresp);
                    } else {
                        System.out.println("No file selected.");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        });
        return resetButton;
    }

    private JButton createSubmitButton() {
        final JButton submitButton = createButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit();
            }
        });
        return submitButton;
    }

    private JButton createIndexButton() {
        final JButton submitButton = createButton("Index Project");

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                indexProject();
            }
        });

        return submitButton;
    }

    private JButton createChatHistoryButton() {
        final JButton submitButton = createButton("Search History");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchIndex();
            }
        });
        return submitButton;
    }

    private JButton createButton(String buttonText) {
        JButton button = new JButton(buttonText);
        button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    private JScrollPane createInputScrollPane() {
        inputTextArea = new JTextArea();
        inputTextArea.setWrapStyleWord(true);
        inputTextArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        int caret = inputTextArea.getCaretPosition();
                        inputTextArea.insert("\n", caret);
                        inputTextArea.setCaretPosition(caret + 1);
                    } else {
                        e.consume(); // to prevent newline being added by default behavior
                        submit();
                    }
                }
            }
        });
        return new JScrollPane(inputTextArea);
    }
    private JTextArea inputTextArea;

    private void submit() {
        String userInput = inputTextArea.getText();
        String selectedModel = (String) modelSelection.getSelectedItem(); // Get the selected model
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                //Get Editior
                JTextComponent editorPane = EditorRegistry.lastFocusedComponent();

                String selectedText = editorPane.getSelectedText();
                if (selectedText != null && (!selectedText.isBlank())) {
                    final ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), userInput + "( in context of " + selectedText + ")");
                    messages.add(userMessage);
                } else {
                    final ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), userInput);
                    messages.add(userMessage);
                }

                appendToOutputDocument("User: ");
                appendToOutputDocument(System.lineSeparator());
                appendToOutputDocument(userInput);
                appendToOutputDocument(System.lineSeparator());
                if (OllamaHelpers.OLLAMA_MODELS.contains(selectedModel)) {

                    appendToOutputDocument("Ollama(" + selectedModel + "): responding...");

                    String llmResp = OllamaHelpers.callLLMChat(null, selectedModel, messages, null, outputTextArea).getJSONObject("message").getString("content");
                    appendToOutputDocumentOllama(llmResp, false);
                    //
                    //Store chat 
                    List<String> chat1 = Arrays.asList(userInput, llmResp);
                    double[] embedding1 = OllamaHelpers.getChatEmbedding(chat1); // Similar chat to query
                    store.storeChat("NBCHAT-" + System.currentTimeMillis(), chat1, embedding1);
                    appendText("[+m]\n");
                    return null;
                } else {
                    callChatGPT(userInput);

                }
                return null;
            }

            private void callChatGPT(String userInput) {
                // Create chat completion request
                ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                        .builder()
                        .model(selectedModel)
                        .messages(messages)
                        .n(1)
                        .maxTokens(1000)
                        .logitBias(new HashMap<>())
                        .build();
                try {
                    appendToOutputDocument("ChatGPT: ");
                    appendToOutputDocument(System.lineSeparator());
                    StringBuilder gptResponse = new StringBuilder();
                    service.streamChatCompletion(chatCompletionRequest)
                            .doOnError(throwable -> {
                                String errorMessage = "Error calling OpenAI API: " + throwable.getMessage();
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(ChatTopComponent.this, errorMessage, "API Error", JOptionPane.ERROR_MESSAGE);
                                });
                            })
                            .blockingForEach(new Consumer<ChatCompletionChunk>() {
                                StringBuilder codeBlockIndicatorBuffer = new StringBuilder();

                                @Override
                                public void accept(ChatCompletionChunk chunk) throws Exception {
                                    for (ChatCompletionChoice choice : chunk.getChoices()) {
                                        if (choice.getMessage().getContent() != null) {
                                            String content = choice.getMessage().getContent();
                                            gptResponse.append(content);
                                            if (!codeBlockIndicatorBuffer.isEmpty()) {
                                                codeBlockIndicatorBuffer.append(content);
                                                if (content.contains(System.lineSeparator())) {
                                                    //flush buffer
                                                    appendToOutputDocument(codeBlockIndicatorBuffer.toString());
                                                    codeBlockIndicatorBuffer.setLength(0);
                                                }
                                            } else {
                                                if (content.startsWith("`")) {
                                                    codeBlockIndicatorBuffer.append(content);
                                                } else {
                                                    appendToOutputDocument(content);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                    final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), gptResponse.toString());
                    messages.add(systemMessage);
                    appendToOutputDocument(System.lineSeparator());
                } catch (OpenAiHttpException ex) {
                    LOG.log(System.Logger.Level.ERROR, "Error calling OpenAI API", ex);
                }
            }

        };
        worker.execute();
        inputTextArea.setText("");
    }

    // Method to append content to outputTextArea and handle code annotations
    // Method to append content to outputTextArea and show code blocks in pop-ups
    private void appendToOutputDocumentOllama(String content, boolean addText) {
        // Split the content by code block marker (```)
        String[] parts = content.split("```");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (i % 2 == 0) {
                // Even index: regular text (outside code blocks)
                if(addText) appendText(part);
            } else {
                String language = part.split("\\r?\\n")[0];
                part = part.substring(language.length());
                // Odd index: code block, show in RSyntaxTextArea with syntax highlighting
                showCodeInPopup(part.trim(), language);  // Assuming Java for this example
                if(addText)appendText(part);
            }
        }

        //
       // appendToOutputDocumentOllama(editorPane, content);
    }

    /**
     * Appends content to the JEditorPane. Supports text and code blocks.
     *
     * @param editorPane The JEditorPane to append content to.
     * @param content The content to append, supporting Markdown-style code
     * blocks.
     */
    private void appendToOutputDocumentOllama(JEditorPane editorPane, String content) {
        try {
            // Get the document and editor kit
            HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
            HTMLEditorKit editorKit = (HTMLEditorKit) editorPane.getEditorKit();

            // Split content into parts by code block markers
            String[] parts = content.split("```");

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];

                if (i % 2 == 0) {
                    // Even index: Regular text (outside code blocks)
                    editorKit.insertHTML(doc, doc.getLength(), "<p>" + escapeHtml(part) + "</p>", 0, 0, null);
                } else {
                    // Odd index: Code block (```language\n<code>```)
                    String[] lines = part.split("\\r?\\n", 2);
                    String language = lines[0]; // First line indicates the language
                    String code = (lines.length > 1) ? lines[1] : "";

                    // Add code block with styling
                    String styledCode = "<pre style='background-color:#f4f4f4; color:#333; border:1px solid #ccc; padding:5px;'>"
                            + "<code>" + escapeHtml(code.trim()) + "</code></pre>";
                    editorKit.insertHTML(doc, doc.getLength(), styledCode, 0, 0, null);
                }
            }

            // Scroll to the latest addition
            editorPane.setCaretPosition(doc.getLength());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Escapes special HTML characters in a string.
     *
     * @param text The text to escape.
     * @return The escaped HTML text.
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Method to append text to the outputTextArea
    private void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            outputTextArea.append(text);
        });

        appendText(editorPane, text);
    }

    /**
     * Appends the given text (HTML supported) to the JEditorPane.
     *
     * @param editorPane The JEditorPane to append the text to.
     * @param text The text to append (HTML supported).
     */
    private void appendText(JEditorPane editorPane, String text) {
        try {
            // Get the document model of the editor
            HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
            HTMLEditorKit editorKit = (HTMLEditorKit) editorPane.getEditorKit();

            // Insert the new text at the end of the document
            editorKit.insertHTML(doc, doc.getLength(), text, 0, 0, null);

            // Scroll to the end to make the appended text visible
            editorPane.setCaretPosition(doc.getLength());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Method to show the identified code in RSyntaxTextArea with "Copy to Clipboard" option
    private void showCodeInPopup(String code, String language) {
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

 

    // Method to determine syntax style based on language string
    private String getSyntaxStyle(String language) {
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
            // Add more cases as needed for other languages
            default:
                return SyntaxConstants.SYNTAX_STYLE_NONE;
        }
    }

    private void appendToOutputDocument(String content) {
        if (content.startsWith("```")) {
            if (shouldAnnotateCodeBlock) {
                int newlinePos = content.indexOf("\n");

                // If a newline character is found, insert the annotation before it
                if (newlinePos != -1) {
                    String beforeNewline = content.substring(0, newlinePos);
                    String afterNewline = content.substring(newlinePos);
                    SwingUtilities.invokeLater(() -> {
                        outputTextArea.append(beforeNewline + " " + QUICK_COPY_TEXT + afterNewline);
                    });
                } else {
                    // If no newline character is found, append the content as is
                    SwingUtilities.invokeLater(() -> {
                        outputTextArea.append(content);
                    });
                }
                shouldAnnotateCodeBlock = false;
            } else {
                SwingUtilities.invokeLater(() -> {
                    outputTextArea.append(content);
                });
                shouldAnnotateCodeBlock = true;
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                outputTextArea.append(content);
            });
        }

        //
       // appendToOutputDocument(editorPane, content);

    }

    /**
     * Appends content to the JEditorPane, handling code blocks and annotations
     * dynamically.
     *
     * @param editorPane The JEditorPane to append content to.
     * @param content The content to append, supporting Markdown-style code
     * blocks.
     */
    private void appendToOutputDocument(JEditorPane editorPane, String content) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Get the document and editor kit
                HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
                HTMLEditorKit editorKit = (HTMLEditorKit) editorPane.getEditorKit();

                if (content.startsWith("```")) {
                    // Handle code block
                    int newlinePos = content.indexOf("\n");

                    if (newlinePos != -1) {
                        // Split content into annotation and code block
                        String beforeNewline = content.substring(0, newlinePos).trim();
                        String afterNewline = content.substring(newlinePos).trim();

                        String annotatedContent = escapeHtml(beforeNewline) + " <span style='color:blue; font-style:italic;'>[Quick Copy]</span>";
                        String styledCode = "<pre style='background-color:#f4f4f4; color:#333; border:1px solid #ccc; padding:5px;'>"
                                + "<code>" + escapeHtml(afterNewline) + "</code></pre>";

                        // Append annotation and code block
                        editorKit.insertHTML(doc, doc.getLength(), annotatedContent, 0, 0, null);
                        editorKit.insertHTML(doc, doc.getLength(), styledCode, 0, 0, null);
                    } else {
                        // Append the content as-is if no newline found
                        String styledCode = "<pre style='background-color:#f4f4f4; color:#333; border:1px solid #ccc; padding:5px;'>"
                                + "<code>" + escapeHtml(content) + "</code></pre>";
                        editorKit.insertHTML(doc, doc.getLength(), styledCode, 0, 0, null);
                    }
                } else {
                    // Append regular text
                    editorKit.insertHTML(doc, doc.getLength(), "<p>" + escapeHtml(content) + "</p>", 0, 0, null);
                }

                // Scroll to the latest addition
                editorPane.setCaretPosition(doc.getLength());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void reset() {
        messages.clear();
        shouldAnnotateCodeBlock = true;
        SwingUtilities.invokeLater(() -> {
            outputTextArea.setText("");
        });
    }

    /**
     * 1. Get path to the folder, 2. Scan all the Java files 3. Create chunks of
     * 256 characters, 4. Call embeddings, 5. Store them into MapDB vector
     * store, 6. The vector db should be created with name of the project.
     */
    private void indexProject() {

        Project selectedProject = IDEHelper.selectProjectFromOpenProjects();
        if (selectedProject == null) {
            System.out.println("No project selected.");
            return;
        }

        System.out.println("Selected project: " + selectedProject.getProjectDirectory().getName());

        // Start a background task using RequestProcessor
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                // Initialize ProgressHandle
                ProgressHandle progressHandle = ProgressHandle.createHandle("Indexing Project: " + selectedProject.getProjectDirectory().getName());
                progressHandle.start();  // Start the progress bar

                try {
                    // Get the file dependencies
                    Map<String, List<String>> result = scanJavaFiles(new File(selectedProject.getProjectDirectory().getPath()));
                    appendText("==== Generating Data for Code Search ====\n");

                    int totalFiles = result.size();
                    int currentFile = 0;

                    for (String key : result.keySet()) {
                        appendText(key + "\n");
                        progressHandle.progress("Processing file: " + key, currentFile * 100 / totalFiles);
                        currentFile++;

                        Path path = Paths.get(key);
                        try {
                            String fileContent = Files.readString(path);
                            String userInput = inputTextArea.getText();
                            String selectedModel = (String) modelSelection.getSelectedItem();
                            appendText(selectedModel + " is being used ... \n");

                            String prompt = (userInput.isBlank() ? "Describe the file content given below in just one sentence.\n" : userInput) + fileContent;
                            JSONObject codeSummary = OllamaHelpers.makeNonStreamedRequest(selectedModel, prompt, false);

                            if (codeSummary != null) {
                                String llmresp = codeSummary.getString("response");
                                appendText(llmresp + "\n");

                                List<String> chat1 = Arrays.asList(llmresp);
                                double[] embedding1 = OllamaHelpers.getChatEmbedding(chat1); // Similar chat to query
                                chat1 = Arrays.asList(llmresp, path.toFile().getAbsolutePath());
                                //The key for storage is Project-name and the sys-millisec
                                store.storeChat(selectedProject.getProjectDirectory().getName() + "-" + System.currentTimeMillis(), chat1, embedding1);
                            }
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }

                    appendText("==== Completed the Indexing ====\n");
                    appendText("Now your questions will be answered by looking at the project code. Also, regularly re-index for updated results.\n");
                } finally {
                    // Complete the progress handle to stop the progress bar
                    progressHandle.finish();
                }
            }
        });

    }

    private void detectBugs() {

        Project selectedProject = IDEHelper.selectProjectFromOpenProjects();
        if (selectedProject == null) {
            System.out.println("No project selected.");
            return;
        }

        System.out.println("Selected project: " + selectedProject.getProjectDirectory().getName());

        if(taskTableModel.getRowCount()==0)
        // Start a background task using RequestProcessor
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                // Initialize ProgressHandle
                ProgressHandle progressHandle = ProgressHandle.createHandle("Bug Detection Project: " + selectedProject.getProjectDirectory().getName());
                progressHandle.start();  // Start the progress bar

                try {
                    // Get the file dependencies
                    Map<String, List<String>> result = scanJavaFiles(new File(selectedProject.getProjectDirectory().getPath()));
                    appendText("==== Detecting Bugs ====\n");

                    int totalFiles = result.size();
                    int currentFile = 0;

                    for (String key : result.keySet()) {
                        appendText("<code> " + key + "\n");
                        progressHandle.progress("Processing file: " + key, currentFile * 100 / totalFiles);
                        currentFile++;

                        Path path = Paths.get(key);
                        try {
                            String fileContent =IDEHelper.prefixLineNumbers(key); //Files.readString(path);
                            String userInput = inputTextArea.getText();
                            String selectedModel = (String) modelSelection.getSelectedItem();
                            appendText(selectedModel + " is being used ... \n");

                            String prompt = (userInput.isBlank() ? "Review the source code and determine the important issues.\n" +
"Then label the issue under various types. Output should be in JSON\n.Schema: \\n"+OllamaHelpers.CODE_REVIEW_FORMAT : userInput) + fileContent;
                            JSONObject codeSummary = OllamaHelpers.makeNonStreamedRequest(selectedModel, prompt,true);
                           //  appendText(codeSummary.toString() + "\n");
                            JSONObject jsonObject = new JSONObject(codeSummary.getString("response"));
                            //Correct filePath
                            jsonObject.put("filePath", path.toFile().getAbsolutePath());
                             LinkedList<Task> taskList=TaskManager.getInstance(store).parseTasksFromJson(jsonObject);
                             store.addTasks(taskList);
                             refreshTaskTable(taskTableModel);
                             appendText((taskList.size()>0?taskList.size():"NO ") + " Task(s) added in 'Task List' tab\n");
                            
                           
//                            if (codeSummary != null) {
//                                String llmresp = codeSummary.getString("response");
//                                appendText(llmresp + "\n");
//                                //If P0 issues are found then create a task in task list
//                                if(llmresp.contains("P0")&& (!llmresp.contains("GOOD"))){
//                                    String refine="Fill in information in format in JSONObject format {  \n" +
//"  \"tags\": \"memory issue, algorithm issue, performance issue, UI issue, dependency issue, API issue, compatibility issue, configuration issue, data processing issue, security issue, scalability issue\"  \n" +
//"} => code review summary =>"+llmresp;
//                                    JSONObject codeSummaryTaskCreation = OllamaHelpers.makeNonStreamedRequest(selectedModel, refine, true);
//                                    String title = codeSummaryTaskCreation.getString("response");
//                                     appendText(  "\n Coding task created -> "+title+"\n");
//                                     TaskManager.getInstance().addTask("P0 Bug",key);
//                                     refreshTaskTable(taskTableModel);
//                                }
//                            }
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }

                    appendText("==== Completed tasks for bug identifications ====\n");
                    appendText("==== I can't raise bug in Github or JIRA yet ====\n");

                } finally {
                    // Complete the progress handle to stop the progress bar
                    progressHandle.finish();
                }
            }
        });
        else
            appendText("'Task List' is not empty. New tasks are added when previous ones are removed.\n");
    }

    private void searchIndex() {
        String userInput = inputTextArea.getText();

        if (!userInput.isBlank()) {
            List<String> chat1 = Arrays.asList(userInput);
            double[] embedding1 = OllamaHelpers.getChatEmbedding(chat1); // Similar chat to query
            List<ChatSimilarityResult> chats = store.findSimilarChats(embedding1, 0.4, 3);
            chats.forEach(chatresult -> {
                List<String> kvChats = store.getChat(chatresult.getChatId());
                kvChats.forEach(chat -> {
                    appendToOutputDocumentOllama(chat + "\n", true);
                });

            });
        } else {
            JOptionPane.showMessageDialog(inputTextArea, "Please put question in input box, for this search.");
        }
    }

}
