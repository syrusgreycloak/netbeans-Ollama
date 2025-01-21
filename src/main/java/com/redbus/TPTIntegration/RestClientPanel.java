/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;

/**
 *
 * @author manoj.kumar
 */
import com.redbus.store.MapDBVectorStore;
import com.stackleader.netbeans.chatgpt.Configuration;
import com.stackleader.netbeans.chatgpt.IDEHelper;
import com.stackleader.netbeans.chatgpt.OllamaHelpers;
import com.stackleader.netbeans.chatgpt.Task;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import org.json.JSONObject;
import org.openide.util.Exceptions;

public class RestClientPanel extends JPanel {

    private JTextField urlField;
    private JTextArea requestBodyArea;
    private JTextPane responseArea;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;

    private final List<RestClientHistory> historyEntries = new ArrayList<>();
     final MapDBVectorStore taskStore;

    public RestClientPanel(MapDBVectorStore taskStore) {
        this.taskStore=taskStore;
        setLayout(new BorderLayout());

        // URL Input Panel
        JPanel urlPanel = new JPanel(new BorderLayout());
        urlField = new JTextField("Enter URL here...");
        JButton getButton = new JButton("GET");
        JButton postButton = new JButton("POST");
        urlPanel.add(urlField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(getButton);
        buttonPanel.add(postButton);
        urlPanel.add(buttonPanel, BorderLayout.EAST);

        // Request Body Panel
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("Request Body (POST)"));
        requestBodyArea = new JTextArea(10, 30);
        requestPanel.add(new JScrollPane(requestBodyArea), BorderLayout.CENTER);

        // Response Panel
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
        responseArea = new JTextPane();
        responseArea.setEditable(false);
        responseArea.setEditorKit(new StyledEditorKit());
        responsePanel.add(new JScrollPane(responseArea), BorderLayout.CENTER);

        // History Panel
         // Bottom panel for history table
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyTableModel = new DefaultTableModel(new String[]{"ID", "Method", "URL","Request","Response"}, 0);
        historyTable = new JTable(historyTableModel);
        historyPanel.setBorder(BorderFactory.createTitledBorder("History"));
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        JScrollPane historyScrollPane = new JScrollPane(historyTable);

        // Add Remove Row Button
        JButton removeRowButton = new JButton("Remove Selected Row");
 
        removeRowButton.addActionListener(e -> deleteSelectedHistory());
        
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);
        
        JPanel historyButtonPanel = new JPanel();
 
        JButton loadHistoryButton = new JButton("Load Selected");
        loadHistoryButton.addActionListener(e -> loadSelectedHistory());
        
         JButton generateButton = new JButton("Generate Client Code");//Move this to Top component in future.
         generateButton.addActionListener(e-> suggestCode(null,OllamaHelpers.selectModelName()) );
        
        historyButtonPanel.add(removeRowButton);
        historyButtonPanel.add(loadHistoryButton);
       // historyButtonPanel.add(generateButton);
        historyPanel.add(historyButtonPanel, BorderLayout.SOUTH);
       

        // Top Section: Request Input and Response
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        topSplitPane.setTopComponent(requestPanel);
        topSplitPane.setBottomComponent(responsePanel);
        topSplitPane.setResizeWeight(0.5); // Split equally

        // Bottom Section: Top Split and History
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(topSplitPane);
        mainSplitPane.setBottomComponent(historyPanel);
        mainSplitPane.setResizeWeight(0.7); // Allocate more space to the top section

        // Add URL Panel and Main Split Pane
        add(urlPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Load history from MapDB, handle issue in loading older data.
        try {
            loadHistoryFromStore();
        } catch (Exception ex) {
        }
        // Event Listeners
        getButton.addActionListener(e -> performGetRequest());
        postButton.addActionListener(e -> performPostRequest());
    }

    private void performGetRequest() {
        String url = urlField.getText().trim();
        try {
            String response = sendRequest(url, "GET", null);
            displayFormattedResponse(response);
            // Save to history
            String id = String.valueOf(System.currentTimeMillis());
            taskStore.addRestClientHistory(id, "GET", url,"", response);
            addHistoryToTable(id, "GET", url,"",response);
        } catch (Exception e) {
            displayFormattedResponse("Error: " + e.getMessage());
            // Save to history
            String id = String.valueOf(System.currentTimeMillis());
            taskStore.addRestClientHistory(id, "GET", url,"", e.getMessage());
            addHistoryToTable(id, "GET", url,"",e.getMessage());
        }
    }

    private void performPostRequest() {
        String url = urlField.getText().trim();
        String requestBody = requestBodyArea.getText();
        try {
            String response = sendRequest(url, "POST", requestBody);
            displayFormattedResponse(response);
           // addHistory("POST", url, response);
            // Save to history
            String id = String.valueOf(System.currentTimeMillis());
            taskStore.addRestClientHistory(id, "POST", url,requestBody, response);
            addHistoryToTable(id, "POST", url,requestBody,response);
        } catch (Exception e) {
            displayFormattedResponse("Error: " + e.getMessage());
            // Save to history
            String id = String.valueOf(System.currentTimeMillis());
            taskStore.addRestClientHistory(id, "POST", url,requestBody, e.getMessage());
            addHistoryToTable(id, "POST", url,requestBody, e.getMessage());
            
        }
    }
    

    private String sendRequest(String urlString, String method, String body) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput("POST".equals(method));

        if (body != null && !body.isEmpty()) {
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes());
                os.flush();
            }
        }

        int responseCode = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }
        reader.close();
        connection.disconnect();
        return response.toString().trim();
    }

    private void displayFormattedResponse(String response) {
        StyledDocument doc = responseArea.getStyledDocument();
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        StyleConstants.setFontSize(attrSet, 14);
        StyleConstants.setFontFamily(attrSet, "Monospaced");

        try {
            doc.remove(0, doc.getLength()); // Clear previous content
            doc.insertString(0, formatJson(response), attrSet);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

       private String formatJson(String json) {
        try {
            
            JSONObject resp=new JSONObject(json);
           return resp.toString(1);
        } catch (Exception e) {
            return json; // Return as-is if not valid JSON
        }
    }

       
       private void addHistoryToTable(String id, String method, String url,String request, String response) {
        historyTableModel.addRow(new Object[]{id, method, url,request,response});
    }

    private void loadHistoryFromStore() {
        List<RestClientHistory> historyList = taskStore.getAllRestClientHistory();
        for (RestClientHistory history : historyList) {
            historyTableModel.addRow(new Object[]{history.getId(), history.getMethod(), history.getUrl(),history.getRequest(),history.getResponse()});
        }
    }
    
    public  RestClientHistory loadSelectedHistory() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow >= 0) {
            String id = (String) historyTableModel.getValueAt(selectedRow, 0);
            String method = (String) historyTableModel.getValueAt(selectedRow, 1);
            String url = (String) historyTableModel.getValueAt(selectedRow, 2);

            RestClientHistory history = taskStore.getRestClientHistory(id);
            if (history != null) {

                urlField.setText(url);
                requestBodyArea.setText(history.getRequest());
                responseArea.setText(history.getResponse());
            }
            return history;
        } else {
            JOptionPane.showMessageDialog(this, "No row selected to load.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
    
 
    
    private void deleteSelectedHistory() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow >= 0) {
            String id = (String) historyTableModel.getValueAt(selectedRow, 0);

            // Remove from store
            taskStore.removeRestClientHistory(id);

            // Remove from table
            historyTableModel.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(this, "No row selected for deletion.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
        public  String suggestCode(  String prompt, String selectedModel) {
          //  String selectedModel=OllamaHelpers.selectModelName();
            //First load selected
           RestClientHistory requestHistory= loadSelectedHistory();
        try {
             
            String fixPrompt =prompt!=null?prompt: "Suggest a java client code that can make the request and also parse it into serilizable object:\n";
            
            fixPrompt=fixPrompt+"\n"+requestHistory.toString();
            
            JSONObject codeSummary = OllamaHelpers.makeNonStreamedRequest(selectedModel, fixPrompt,false);
            
            String aiResponse=codeSummary.getString("response");
            IDEHelper.showCodeInPopup(aiResponse, "java");//Handle it better in future for all languages.
            
            return aiResponse;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

      // Main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog((Frame) null, "Rest Client", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setSize(800, 600);
            MapDBVectorStore store;
            Configuration config = Configuration.getInstance();
            Preferences prefs = config.getPreferences();
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
            store = new MapDBVectorStore(pluginHomeDir + "/MKVECOLLAMA.db", "vectors");
            RestClientPanel restClientPanel = new RestClientPanel(store);
            dialog.add(restClientPanel);

            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
    }
    
}
