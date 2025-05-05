/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class VoiceRecorderApp extends JFrame {
    private static final String AUDIO_FILE = "recorded_audio.wav";
    private boolean recording = false;
    private ByteArrayOutputStream audioOutputStream;
    private TargetDataLine microphone;

    public VoiceRecorderApp() {
        setTitle("Voice Recorder");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel label = new JLabel("Press & Hold SPACE to Record", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        add(label);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !recording) {
                    startRecording();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && recording) {
                    stopRecording();
                    playAudio(AUDIO_FILE);
                    sendAudioToServer(AUDIO_FILE);
                }
            }
        });

        setFocusable(true);
        setVisible(true);
    }

    private void startRecording() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Microphone not supported.");
                    return;
                }

                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                audioOutputStream = new ByteArrayOutputStream();
                recording = true;

                byte[] buffer = new byte[1024];
                while (recording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioOutputStream.write(buffer, 0, bytesRead);
                    }
                }

                saveAudioFile(AUDIO_FILE, audioOutputStream.toByteArray(), format);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void stopRecording() {
        recording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }

    private void saveAudioFile(String fileName, byte[] audioData, AudioFormat format) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
             FileOutputStream fos = new FileOutputStream(fileName)) {

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(fileName));
            System.out.println("Audio saved to " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playAudio(String filePath) {
        new Thread(() -> {
            try {
                File audioFile = new File(filePath);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
                while (clip.isRunning()) {
                    Thread.sleep(100);
                }
                clip.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
private void sendAudioToServer(String filePath) {
    new Thread(() -> {
        try {
            File audioFile = new File(filePath);
            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";  // Arbitrary boundary string

            URL url = new URL("http://localhost:5000/transcribe");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
                 FileInputStream fis = new FileInputStream(audioFile)) {

                // Write multipart form data
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + audioFile.getName() + "\"\r\n");
                writer.append("Content-Type: audio/wav\r\n\r\n");
                writer.flush();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();

                // End of multipart form data
                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }

            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response = reader.readLine();
                    System.out.println("Transcription: " + response);
                    JOptionPane.showMessageDialog(null, "Transcription: " + response, "Result", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                System.err.println("Failed to send audio: " + connection.getResponseMessage());
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VoiceRecorderApp::new);
    }
}
