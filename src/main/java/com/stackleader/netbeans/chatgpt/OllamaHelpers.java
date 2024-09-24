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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    
    public static final Set<String> OLLAMA_MODELS_TOOLS=new HashSet();
    
    static{
        //LLM Settings        
        String value_name = System.getenv("LLM_OLLAMA_HOST");//Get this from environment vaiable to add flexibility to refer to any other Ollama hosting.
        if(value_name!=null) OLLAMA_EP=value_name;
        
        OLLAMA_MODELS_TOOLS.add("nemotron-mini");
        OLLAMA_MODELS_TOOLS.add("llama3.1");
    }
    
    /**
     * 
     * @param prompt
     * @param model
     * @param messages_raw
     * @param tools
     * @return 
     */
    public static JSONObject callLLMChat(String prompt, String model, List<ChatMessage> messages_raw, JSONArray tools) {

        JSONArray messages = new JSONArray();

        messages_raw.forEach(message -> {
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", message.getRole());
            messageObject.put("content", message.getContent());
            messages.put(messageObject);

        });

        if (tools == null) {
            return callLLMChat(prompt, model, messages, tools);
        } else if (OLLAMA_MODELS_TOOLS.contains(model)) {

            return callApiAndHandleResponse(model, messages);

        } else {
            return callLLMChat(prompt, model, messages, tools);
        }

    }
    
    /**
     * Use prompt parameter to moderate the questions is prompt!=null, using the generate
     * "options": {"num_ctx": 4096}
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
    
    
    /**
     * 
     * @param model
     * @param messagesArray
     * @return 
     */
    
    public static JSONObject callApiAndHandleResponse( String model,JSONArray messagesArray) {
    try {
        // Construct the JSON payload
        JSONObject payload = new JSONObject();
        payload.put("model", model);

        if(messagesArray.length()==0){// for first time call
            JSONObject messageSystemObject = new JSONObject();
            messageSystemObject.put("role", "system");
            messageSystemObject.put("content", "You are a helpful customer support assistant. Use the supplied tools to assist the user. Do not assume required properties values for tools, always ask for clarification to user."); //forecast for a location  What is the weather today 
            messagesArray.put(messageSystemObject);
        }

        System.out.println(messagesArray.toString());

        // Get the user's message from the text field
        String userInput = "test"; // Replace with actual input

        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", userInput); //forecast for a location  What is the weather today // JOptionPane.showInputDialog("What is your question?")
        messagesArray.put(messageObject);
        payload.put("messages", messagesArray);

        payload.put("stream", false);

        JSONArray toolsArray = new JSONArray();

        // Add each function's JSON to the payload
        // (Assuming you have a map of FunctionHandlers)
        for (FunctionHandler handler : IDEHelper.getFunctionHandlers().values()) {
            JSONObject toolObject = new JSONObject();
            toolObject.put("type", "function");
            toolObject.put("function", handler.getFunctionJSON());
            toolsArray.put(toolObject);
        }

        payload.put("tools", toolsArray);

        System.out.println(payload.toString(1));

        // URL of the API endpoint
        String value_name = System.getenv("LLM_OLLAMA_HOST");
        URL url = new URL(value_name+"/api/chat");

        // Set request method to POST
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        // Send JSON payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Handle the response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());

            // Extract tool calls and execute corresponding functions
            JSONObject messageObjectResponse = jsonResponse.getJSONObject("message");
            if(messageObjectResponse.has("tool_calls"))
            {
                JSONArray toolCallsArray = messageObjectResponse.getJSONArray("tool_calls");

                for (int i = 0; i < toolCallsArray.length(); i++) {
                    JSONObject toolCall = toolCallsArray.getJSONObject(i);
                    JSONObject functionResponse = toolCall.getJSONObject("function");
                    String functionName = functionResponse.getString("name");

                    // Execute the registered function
                    FunctionHandler handler = IDEHelper.getFunctionHandlers().get(functionName);
                    if (handler != null) {
                        JSONObject arguments = functionResponse.getJSONObject("arguments");
                        String funcVal=handler.execute(arguments);

                        JSONObject messageObjectT = new JSONObject();
                        messageObjectT.put("role",  messageObjectResponse.getString("role"));
                        messageObjectT.put("content", funcVal); //forecast for a location  What is the weather today // JOptionPane.showInputDialog("What is your question?")
                        messagesArray.put(messageObjectT);

                        //callApiAndHandleResponse( messagesArray);
                        
                        return messageObjectT;
                    } else {
                        System.out.println("No handler registered for function: " + functionName);
                    }
                }
            }else {

//                System.out.println(messageObjectResponse); // print messageObjectResponse
//
//                JSONObject messageObjectT = new JSONObject();
//                messageObjectT.put("role",  messageObjectResponse.getString("role"));
//                messageObjectT.put("content", messageObjectResponse.getString("content")); //forecast for a location  What is the weather today // JOptionPane.showInputDialog("What is your question?")
//                messagesArray.put(messageObjectT);
//
//                callApiAndHandleResponse( messagesArray);
                
                return messageObjectResponse;

            }

            System.out.println("Response handled successfully");
        } else {
            System.out.println("POST request failed with response code: " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
        return null;
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
