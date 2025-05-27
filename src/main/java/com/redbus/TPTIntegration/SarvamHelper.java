/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openide.util.Exceptions;
/**
 *
 * @author manoj.kumar
 */
public class SarvamHelper {
    private String id;
    private String method;
    public   static String url="https://api.sarvam.ai/v1/chat/completions"; //not making final
    private String request;

    // Getters and setters

    public static void main(String[] args) {
        try {
            // Create the request payload
            String requestPayload = "{\"messages\": [    {\n" +
"      \"role\": \"system\",\n" +
"      \"content\": \"greet user with hello\"\n" +
"    },\n" +
"     {\n" +
"      \"role\": \"user\",\n" +
"      \"content\": \"java code for matrix multiplication\"\n" +
"    }], \"model\": \"sarvam-m\"}";

            // Make the HTTP request
            String responseJson = makeHttpRequest(url, requestPayload);
            
                    try {
            // 1. Parse the overall JSON string into a JSONObject
            JSONObject jsonResponse = new JSONObject(responseJson);

            // 2. Get the "choices" array
            JSONArray choicesArray = jsonResponse.getJSONArray("choices");

            // 3. Get the first object in the "choices" array (assuming there's at least one)
            if (choicesArray.length() > 0) {
                JSONObject firstChoice = choicesArray.getJSONObject(0);

                // 4. Get the "message" object from the first choice
                JSONObject messageObject = firstChoice.getJSONObject("message");

                // 5. Get the "content" string from the "message" object
                String content = messageObject.getString("content");

                // 6. Print the extracted content
                System.out.println("Extracted Content: " + content);

            } else {
                System.out.println("The 'choices' array is empty.");
            }

        } catch (JSONException e) {
            // Handle potential parsing errors or missing keys
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

            // Parse the response into a Java object
 //           ObjectMapper objectMapper = new ObjectMapper();
//            Response response = objectMapper.readValue(responseJson, Response.class);

            // Create the RestClientHistory object
//            RestClientHistory history = new RestClientHistory();
//            history.setId("1748257196568");
//            history.setMethod("POST");
//            history.setUrl("https://api.sarvam.ai/v1/chat/completions");
//            history.setRequest(requestPayload);
//            history.setResponse(response);

            // Serialize the RestClientHistory object to JSON
            //String historyJson = objectMapper.writeValueAsString(history);
            //System.out.println(historyJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static JSONObject call(String model,JSONArray messages, boolean stream, RSyntaxTextArea outputArea){
    
                // Create the root JSON object
        JSONObject payload = new JSONObject();

        // Create the messages array
        JSONArray messagesArray = new JSONArray();

        // Create the first message object (system)
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "greet user with hello");

        // Create the second message object (user)
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "java code for matrix multiplication");

        // Add message objects to the array
        messagesArray.put(systemMessage);
        messagesArray.put(userMessage);

        // Add the messages array and model key to the root payload object
        payload.put("messages", messages);
        payload.put("model", model);
        payload.put("stream", false);

        // Convert the JSONObject to a string (formatted for readability)
        String jsonString = payload.toString(4); // 4 is indentation spaces

        // Print the resulting JSON string
        System.out.println(jsonString);
        
        String responseJson = makeHttpRequest(url, jsonString);
        if(responseJson!=null){
            
             try {
            // 1. Parse the overall JSON string into a JSONObject
            JSONObject jsonResponse = new JSONObject(responseJson);

            // 2. Get the "choices" array
            JSONArray choicesArray = jsonResponse.getJSONArray("choices");

            // 3. Get the first object in the "choices" array (assuming there's at least one)
            if (choicesArray.length() > 0) {
                JSONObject firstChoice = choicesArray.getJSONObject(0);

                // 4. Get the "message" object from the first choice
                JSONObject messageObject = firstChoice.getJSONObject("message");

                // 5. Get the "content" string from the "message" object
                String content = messageObject.getString("content");

                // 6. Print the extracted content
                System.out.println("Extracted Content: " + content);
                
                  JSONObject responseJ = new JSONObject();
                     //getJSONObject("message").getString("content");
                     JSONObject message=new JSONObject();
                     message.put("content", content);
                     message.put("role", "assistant");
                     responseJ.put("message", message);

                     if (outputArea != null) {
                            // Append response to the RSyntaxTextArea
                            SwingUtilities.invokeLater(() -> {
                                outputArea.append(content);
                                 //outputArea.requestFocusInWindow();
                            });
                        }
                     
                     return responseJ;

            } else {
                System.out.println("The 'choices' array is empty.");
            }

        } catch (JSONException e) {
            // Handle potential parsing errors or missing keys
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }
            
           
        
        }
            

        // Or print without formatting (more common for API requests)
        // String compactJsonString = payload.toString();
        // System.out.println(compactJsonString);
        return null;
    }

    public static String makeHttpRequest(String urlString, String requestPayload)   {
        try {
            String method="POST";
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            String value_name = System.getenv("SARVAM_API_KEY");
            connection.setRequestProperty("Authorization", "Bearer "+value_name);
            connection.setDoOutput(true);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // Success
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                //throw new Exception("HTTP request failed with response code: " + responseCode);
                JOptionPane.showMessageDialog(null,requestPayload+"\n\nHTTP request failed with response code: " + responseCode);
            }
        }   catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

 
}
