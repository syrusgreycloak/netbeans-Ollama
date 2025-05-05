/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.sound.sampled.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class AssistantClient extends JFrame {
    private JButton recordButton, transcribeButton, askButton, speakButton;
    private JTextArea transcriptArea, responseArea;
    private JLabel audioVisualizer;
    
    public AssistantClient() {
        setTitle("AI Assistant Client");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel();
        recordButton = new JButton("Record");
        transcribeButton = new JButton("Transcribe");
        askButton = new JButton("Ask AI");
        speakButton = new JButton("Speak");
        
        topPanel.add(recordButton);
        topPanel.add(transcribeButton);
        topPanel.add(askButton);
        topPanel.add(speakButton);
        
        transcriptArea = new JTextArea(5, 40);
        responseArea = new JTextArea(5, 40);
        
        audioVisualizer = new JLabel("Audio Visualizer Here", SwingConstants.CENTER);
        audioVisualizer.setOpaque(true);
        audioVisualizer.setBackground(Color.BLACK);
        audioVisualizer.setForeground(Color.GREEN);
        
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(transcriptArea), BorderLayout.CENTER);
        add(new JScrollPane(responseArea), BorderLayout.SOUTH);
        add(audioVisualizer, BorderLayout.WEST);
        
        recordButton.addActionListener(e -> recordAudio());
        transcribeButton.addActionListener(e -> transcribeAudio());
        askButton.addActionListener(e -> askAI());
        speakButton.addActionListener(e -> speakResponse());
    }
    
    private void recordAudio() {
    new Thread(() -> {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                JOptionPane.showMessageDialog(this, "Microphone not supported!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            audioVisualizer.setText("Recording...");
            while (recordButton.getModel().isPressed()) {
                bytesRead = line.read(buffer, 0, buffer.length);
                out.write(buffer, 0, bytesRead);
            }

            line.stop();
            line.close();
            
            audioVisualizer.setText("Recording Stopped");

            // Send recorded audio to API
            byte[] audioData = out.toByteArray();
            sendAudioToServer(audioData);
            
        } catch (Exception e) {
            e.printStackTrace();
            audioVisualizer.setText("Recording Error");
        }
    }).start();
}
    
    private void sendAudioToServer(byte[] audioData) {
    new Thread(() -> {
        try {
            URL url = new URL("http://localhost:5000/record");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(audioData);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                audioVisualizer.setText("Audio Sent Successfully");
            } else {
                audioVisualizer.setText("Server Error: " + responseCode);
            }

        } catch (IOException e) {
            e.printStackTrace();
            audioVisualizer.setText("Failed to Send Audio");
        }
    }).start();
}


    
    private void transcribeAudio() {
        new Thread(() -> {
            try {
                String response = sendPostRequest("http://localhost:5000/transcribe", "{}");
                JSONObject json = new JSONObject(response);
                transcriptArea.setText(json.getString("transcription"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void askAI() {
        new Thread(() -> {
            try {
                String prompt = transcriptArea.getText();
                String response = sendPostRequest("http://localhost:5000/ask", "{\"prompt\": \"" + prompt + "\"}");
                JSONObject json = new JSONObject(response);
                responseArea.setText(json.getString("response"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void speakResponse() {
        new Thread(() -> {
            try {
                String text = responseArea.getText();
                sendPostRequest("http://localhost:5000/speak", "{\"text\": \"" + text + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private String sendPostRequest(String urlString, String jsonPayload) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        return response.toString();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AssistantClient client = new AssistantClient();
            client.setVisible(true);
        });
    }
}
