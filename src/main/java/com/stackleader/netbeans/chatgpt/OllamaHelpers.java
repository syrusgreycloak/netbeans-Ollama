/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openide.util.Exceptions;

/**
 *
 * @author manoj.kumar
 */
public class OllamaHelpers {
    
    static String OLLAMA_EP="http://localhost:11434";
    
    public static final Set<String> OLLAMA_MODELS=new HashSet();
    
    static{
        //LLM Settings        
        String value_name = System.getenv("LLM_OLLAMA_HOST");//Get this from environment vaiable to add flexibility to refer to any other Ollama hosting.
        if(value_name!=null) OLLAMA_EP=value_name;
    }
    
    public static JSONObject callLLMChat(String prompt, String model,List<ChatMessage> messages_raw, JSONArray tools)   {
         JSONArray messages=new JSONArray();
         
         messages_raw.forEach(message->{
             JSONObject messageObject=new JSONObject();
             messageObject.put("role", message.getRole());
             messageObject.put("content", message.getContent());
             messages.put(messageObject);
             
         });
         
         return callLLMChat(prompt,  model,  messages,  tools);
    
    }
    
    /**
     * Use prompt parameter to moderate the questions is prompt!=null, using the generate
     *   "options": {
    "num_ctx": 4096
  }
     * @param prompt
     * @param model
     * @param messages
     * @return 
     */
    public static JSONObject callLLMChat(String prompt, String model, JSONArray messages, JSONArray tools)   {
         JSONObject responseJ = new JSONObject();
        try {
           
            // API endpoint URL
            String apiUrl =  OLLAMA_EP+"/api/chat";
            
            // Constructing the JSON payload
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("stream", false);
            
            JSONObject options = new JSONObject();
            options.put("num_ctx", 4096);
            
            payload.put("messages", messages);
            if(tools!=null){
                payload.put("tools", tools);
            }
            payload.put("options", options);
            
            // Convert payload to string
            String jsonString = payload.toString();
            
            // Create URL object
            URL url = new URL(apiUrl);
            
            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set request method
            connection.setRequestMethod("POST");
            
            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Enable output and set content length
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(jsonString.getBytes().length);
            
            // Write JSON data to output stream
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.writeBytes(jsonString);
                outputStream.flush();
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // Read response body
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Response Body: " + response.toString());
                
                responseJ = new JSONObject(response.toString());
                
            }
            
            // Close connection
            connection.disconnect();
            
           
        }   catch (MalformedURLException ex) {
            Logger.getLogger(OllamaHelpers.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OllamaHelpers.class.getName()).log(Level.SEVERE, null, ex);
        }
       return responseJ;
    }
    
      public static String[] fetchModelNames()  {
          
        try {
            // API endpoint URL
            String url =  OLLAMA_EP+"/api/tags";
            
            // Create an HttpClient
            HttpClient client = HttpClient.newHttpClient();
            
            // Create an HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Parse JSON using JSONObject from json.org
            JSONObject jsonObject = new JSONObject(response.body());
            JSONArray modelsArray = jsonObject.getJSONArray("models");
            
            // Create a list to store the names
            List<String> namesList = new ArrayList<>();
            
            // Iterate over the models array and get the "name" field
            for (int i = 0; i < modelsArray.length(); i++) {
                JSONObject modelObject = modelsArray.getJSONObject(i);
                String name = modelObject.getString("name");
                namesList.add(name);
                OLLAMA_MODELS.add(name);
            }
            
            // Convert list to array and return
            return namesList.toArray(new String[0]);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new String[]{};
    }
      
    public static String[] mergeArrays(String[] array1, String[] array2) {
        // Create a new array with a size equal to the sum of both arrays
        String[] mergedArray = new String[array1.length + array2.length];

        // Copy the elements of the first array into the merged array
        System.arraycopy(array1, 0, mergedArray, 0, array1.length);

        // Copy the elements of the second array into the merged array
        System.arraycopy(array2, 0, mergedArray, array1.length, array2.length);

        // Return the merged array
        return mergedArray;
    }
    
}
