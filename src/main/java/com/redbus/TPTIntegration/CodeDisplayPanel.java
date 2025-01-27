/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;

/**
 *
 * @author manoj.kumar
 */
import com.stackleader.netbeans.chatgpt.IDEHelper;
import com.stackleader.netbeans.chatgpt.OllamaHelpers;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.json.JSONArray;
import org.json.JSONObject;

public class CodeDisplayPanel extends JPanel {

    public CodeDisplayPanel(String code, String language, Map<String, Object> codeMap) {
        super();
        setLayout(new BorderLayout()); // Set layout to BorderLayout for this panel

        // Create components (same as your popup logic)
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Key");
        tableModel.addColumn("Value");

        for (Map.Entry<String, Object> entry : codeMap.entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        JTable table = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(table);

        RSyntaxTextArea textArea = new RSyntaxTextArea(30, 120);
        textArea.setText(code);
        textArea.setEditable(true);
        textArea.setSyntaxEditingStyle(IDEHelper.getSyntaxStyle(language));
        textArea.setCodeFoldingEnabled(true);

        RTextScrollPane codeScrollPane = new RTextScrollPane(textArea);
        codeScrollPane.setFoldIndicatorEnabled(true);

        JTextField inputField = new JTextField("Prompt goes here...");
        JPanel tablePanelWithInput = new JPanel(new BorderLayout());
        tablePanelWithInput.add(tableScrollPane, BorderLayout.CENTER);
        tablePanelWithInput.add(inputField, BorderLayout.SOUTH);

        // Add components to the main panel
        add(codeScrollPane, BorderLayout.NORTH); // Text area at top
        add(tablePanelWithInput, BorderLayout.CENTER);

        // Add buttons (customize as needed)
        JButton copyButton = new JButton("Copy");
        JButton toolsButton = new JButton("Test Tools Call");
        JButton closeButton = new JButton("Close");
        
        toolsButton.addActionListener(e->{
               java.util.List<ChatMessage> messages = new CopyOnWriteArrayList<>();
                final ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), inputField.getText());
                messages.add(userMessage);
                JSONArray tools = new JSONArray();
                tools.put(new JSONObject(code));
                String llmResp = OllamaHelpers.callLLMChat(null, OllamaHelpers.selectModelName(), messages, tools, textArea).getJSONObject("message").getString("content");
                JOptionPane.showMessageDialog(null, llmResp, "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        
        copyButton.addActionListener(e->{
            IDEHelper.copyToClipboard(code);
                JOptionPane.showMessageDialog(null, "Code copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel buttonPanel = new JPanel(); // Panel to hold buttons
        buttonPanel.add(copyButton);
        buttonPanel.add(toolsButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH); 
    }

   
    
        public static void main(String[] args) {
        JFrame frame = new JFrame("Code Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Sample data
        String code = "System.out.println(\"Hello World!\");";
        String language = "java";
        Map<String, Object> codeMap = Map.of(
            "Key1", "Value1",
            "Key2", "Value2"
        );

        CodeDisplayPanel panel = new CodeDisplayPanel(code, language, codeMap);
        frame.getContentPane().add(panel);

        frame.pack();
        frame.setVisible(true);
    }
}


//public static void showCodeInJFrame(String code, String language, Map<String, Object> codeMap) {
//    SwingUtilities.invokeLater(() -> {
//        new CodeDisplayFrame(code, language, codeMap); 
//    });
//}

