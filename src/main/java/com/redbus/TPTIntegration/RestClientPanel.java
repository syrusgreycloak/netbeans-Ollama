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
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.json.JSONObject;
import org.openide.util.Exceptions;

public class RestClientPanel extends JPanel {

    private JTextField urlField;
     private final JTable headerTable = new JTable(new DefaultTableModel(
            new Object[][]{},
            new String[]{"Header", "Value"}
        ));
    private RSyntaxTextArea requestBodyArea;
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
        // Header Panel
 
        JPanel headerPanel = new JPanel();
        headerPanel.setBorder(BorderFactory.createTitledBorder("Headers"));
        addHeaderButtons(headerPanel); // Your method to add buttons
        
        // Set up the scroll pane and make sure only 3 rows are visible
        JScrollPane headerScrollPane = new JScrollPane(headerTable);
        // Set preferred size for headerScrollPane, where height is calculated based on row height
        int preferredHeight = (int)headerTable.getRowHeight() * 6;
        Dimension prefSize = new Dimension(200, preferredHeight); // Width can be adjusted as needed
        headerScrollPane.setPreferredSize(prefSize);
        
        headerPanel.add(headerScrollPane);
        // Add the header panel to the URL Panel
        urlPanel.add(headerPanel, BorderLayout.SOUTH); 

        // Request Body Panel
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("Request Body (POST)"));
        requestBodyArea = new RSyntaxTextArea(10, 30);
        requestBodyArea.setSyntaxEditingStyle(IDEHelper.getSyntaxStyle("json"));
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
        historyTableModel = new DefaultTableModel(new String[]{"ID", "Method", "URL","Request","Response","Headers"}, 0);
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
    
        private void addHeaderButtons(JPanel panel) {
        JButton addButton = new JButton("Add Header");
        JButton removeButton = new JButton("Remove Selected");

        addButton.addActionListener(e -> {
            String headerName = JOptionPane.showInputDialog(this, "Enter header name:");
            if (headerName != null && !headerName.isEmpty()) {
                String headerValue = JOptionPane.showInputDialog(this, "Enter value for '" + headerName + "':");
                ((DefaultTableModel)headerTable.getModel()).addRow(new Object[]{headerName, headerValue});
            }
        });

        removeButton.addActionListener(e -> {
            int selectedRow = headerTable.getSelectedRow();
            if (selectedRow >= 0) {
                ((DefaultTableModel)headerTable.getModel()).removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "No row selected to remove.");
            }
        });

        panel.add(addButton);
        panel.add(removeButton);
    }
        
        private HashMap<String, String> parseHeadersTable(DefaultTableModel model) {
    HashMap<String, String> headers = new HashMap<>();
    for (int i = 0; i < model.getRowCount(); i++) {
        String key = model.getValueAt(i, 0).toString().trim();
        String value = model.getValueAt(i, 1).toString().trim();
        if (!key.isEmpty()) {
            headers.put(key, value);
        }
    }
    return headers;
}
        
            private HashMap<String, String> parseHeaders(String text) {
        HashMap<String, String> headers = new HashMap<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (!line.isEmpty()) {
                int separatorIndex = line.indexOf(": ");
                if (separatorIndex != -1 && separatorIndex < line.length() - 2) {
                    String key = line.substring(0, separatorIndex);
                    String value = line.substring(separatorIndex + 2).trim();
                    headers.put(key.trim(), value);
                }
            }
        }
        return headers;
    }
            
            private String formatHeaders(HashMap<String, String> headers) {
    StringBuilder sb = new StringBuilder();
    for (String key : headers.keySet()) {
        sb.append(key).append(": ").append(headers.get(key)).append("\n");
    }
    return sb.toString();
}

    private void performGetRequest() {
        String url = urlField.getText().trim();
        try {
            String response = sendRequest(url, "GET", null);
            displayFormattedResponse(response);
            // Save to history
            String id = String.valueOf(System.currentTimeMillis());
            taskStore.addRestClientHistory(id, "GET", url,"", response);
            addHistoryToTable(id, "GET", url,"",response,parseHeadersTable( (DefaultTableModel) headerTable.getModel()));
        } catch (Exception e) {
            displayFormattedResponse("Error: " + e.getMessage());
            // Save to history
            String id = String.valueOf(System.currentTimeMillis());
            taskStore.addRestClientHistory(id, "GET", url,"", e.getMessage());
            addHistoryToTable(id, "GET", url,"",e.getMessage(),parseHeadersTable( (DefaultTableModel) headerTable.getModel()));
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
            addHistoryToTable(id, "POST", url,requestBody,response,parseHeadersTable( (DefaultTableModel) headerTable.getModel()));
        } catch (Exception e) {
            displayFormattedResponse("Error: " + e.getMessage());
            // Save to history
            String id = String.valueOf(System.currentTimeMillis());
            taskStore.addRestClientHistory(id, "POST", url,requestBody, e.getMessage());
            addHistoryToTable(id, "POST", url,requestBody, e.getMessage(),parseHeadersTable( (DefaultTableModel) headerTable.getModel()));
            
        }
    }
    

    private String sendRequest(String urlString, String method, String body) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput("POST".equals(method));
        
        // Add Headers
        DefaultTableModel model = (DefaultTableModel) headerTable.getModel();
        for(int i=0; i < model.getRowCount(); i++) {
            String key = model.getValueAt(i, 0).toString().trim();
            String value = model.getValueAt(i, 1).toString().trim();
            if (!key.isEmpty()) connection.setRequestProperty(key, value);
        }

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

       
    private void addHistoryToTable(String id, String method, String url, String request, String response, HashMap<String, String> headers) {
        StringBuilder headerStr = new StringBuilder();
        for (String key : headers.keySet()) {
            headerStr.append(key).append(": ").append(headers.get(key)).append("\n");
        }
        
        response=headerStr.toString()+"\n\n"+response;
        
        historyTableModel.addRow(new Object[]{id, method, url, request, response});
    }

    private void loadHistoryFromStore() {
        List<RestClientHistory> historyList = taskStore.getAllRestClientHistory();
        for (RestClientHistory history : historyList) {
            historyTableModel.addRow(new Object[]{history.getId(), history.getMethod(), history.getUrl(), history.getRequest(), history.getResponse()});
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
