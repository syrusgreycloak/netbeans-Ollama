/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

import com.redbus.TPTIntegration.SarvamHelper;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
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
    
    
    public static final Set<String> GEMINI_MODELS=new HashSet();
    
    public static final Set<String> SARVAM=new HashSet();
    
    static {
        //LLM Settings        
        String value_name = System.getenv("LLM_OLLAMA_HOST");//Get this from environment vaiable to add flexibility to refer to any other Ollama hosting.
        if (value_name != null) {
            OLLAMA_EP = value_name;
        }

//        OLLAMA_MODELS_TOOLS.add("qwen2.5:1.5b");
//        OLLAMA_MODELS_TOOLS.add("llama3.2:latest");
//        OLLAMA_MODELS_TOOLS.add("nemotron-mini:latest");
//        OLLAMA_MODELS_TOOLS.add("llama3.1:latest");
//        GEMINI_MODELS.add("gemini-2.5-pro-preview-03-25");
        GEMINI_MODELS.add("gemini-2.5-flash-preview-04-17");
        GEMINI_MODELS.add("gemini-2.0-flash");
        GEMINI_MODELS.add("gemini-2.0-flash-lite");
        GEMINI_MODELS.add("gemini-1.5-pro");
        GEMINI_MODELS.add("gemini-1.5-flash");
        GEMINI_MODELS.add("gemini-1.5-flash-8b");
        GEMINI_MODELS.add("gemma-3-1b-it");
        GEMINI_MODELS.add("gemma-3-4b-it");
        GEMINI_MODELS.add("gemma-3-12b-it");
        GEMINI_MODELS.add("gemma-3-27b-it");
        
        //SARVAM
        SARVAM.add("sarvam-m");

    }
    
    /**
     *
     * @param prompt
     * @param model
     * @param messages_raw
     * @param tools
     * @return
     */
    public static JSONObject callLLMChat(String prompt, String model, List<ChatMessage> messages_raw, JSONArray tools, RSyntaxTextArea editorPane) {

        JSONArray messages = new JSONArray();

        if(GEMINI_MODELS.contains(model)){
            
              JSONObject systemUserPart = new JSONObject()
                     .put("role", "user")
                     .put("parts", new JSONArray().put(new JSONObject().put("text", "You are a helpful application developer support assistant. Use the supplied tools to assist the user. Do not assume required properties values for tools, always ask for clarification to user.")));
                 JSONObject systemModelPart = new JSONObject()
                     .put("role", "model")
                     .put("parts", new JSONArray().put(new JSONObject().put("text", "Understood. I will act as a helpful developer support assistant and request clarification for tool parameters.")));
                 messages.put(systemUserPart);
                 messages.put(systemModelPart);
                 
                 
                   messages_raw.forEach(message -> {
            JSONObject usermsgPart = new JSONObject()
                     .put("role",(message.getRole().contentEquals("system")?"model":message.getRole())) // Very important to use Gemini and Ollama both togetehr in one session
                     .put("parts", new JSONArray().put(new JSONObject().put("text", message.getContent())));
                
            messages.put(usermsgPart);

        });
        
        
            // Show the dialog with RSyntaxTextArea embedded
            //JOptionPane.showMessageDialog(null, "callGeminiApiAndHandleResponse(model, messages)"+model);
            
           return   callGeminiStream(  model, messages,  editorPane) ;
             
           // return callGeminiApiAndHandleResponse( model,  messages,  prompt,  editorPane);
        }
        
        

         
        if (SARVAM.contains(model)) {

            messages_raw.forEach(message -> {
                JSONObject messageObject = new JSONObject();
                //messageObject.put("role", message.getRole());
                messageObject.put("role", (message.getRole().contentEquals("user") ? "user" : "assistant"));// Very important to use Gemini and Ollama both togetehr in one session
                messageObject.put("content", message.getContent());
                messages.put(messageObject);

            });

            try {
                return SarvamHelper.call(model, messages, true, editorPane);

            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }

        }
        
        //DEFAULT OLLAMA
         messages_raw.forEach(message -> {
            JSONObject messageObject = new JSONObject();
            //messageObject.put("role", message.getRole());
            messageObject.put("role",(message.getRole().contentEquals("model")?"system":message.getRole()));// Very important to use Gemini and Ollama both togetehr in one session
            messageObject.put("content", message.getContent());
            messages.put(messageObject);

        });
//        if (tools == null) {
//            return callLLMChat(prompt, model, messages, tools);
//        } else
        if (OLLAMA_MODELS_TOOLS.contains(model)) {

            // Show the dialog with RSyntaxTextArea embedded
            JOptionPane.showMessageDialog(null, "callApiAndHandleResponse(model, messages)");

            return callApiAndHandleResponse(model, messages, true, editorPane);

        } else {
            return callLLMChat(prompt, model, messages, tools, true, editorPane);
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
    public static JSONObject callLLMChat(String prompt, String model, JSONArray messages, JSONArray tools, boolean stream, RSyntaxTextArea outputArea)   {
         JSONObject responseJ = new JSONObject();
        try {
           
            // API endpoint URL
            String apiUrl =  OLLAMA_EP+"/api/chat";
            
            // Constructing the JSON payload
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("stream", stream);
            
            JSONObject options = new JSONObject();
            options.put("num_ctx", 8192);
            
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
                StringBuilder streamOutput=new StringBuilder();
                
                if(stream){
                    

                    
                    // Read each line (JSON object) from the stream
                    while ((line = reader.readLine()) != null) {
                        // Parse each line as a JSON object
                        JSONObject jsonObject = new JSONObject(line);

                        /**
                         * {
                            "model": "llama3.2",
                            "created_at": "2023-08-04T08:52:19.385406455-07:00",
                            "message": {
                              "role": "assistant",
                              "content": "The",
                              "images": null
                            },
                            "done": false
                          }
                         */
                        // Extract values from the JSON object
                        String responseText = jsonObject.getJSONObject("message").getString("content");
                        boolean done = jsonObject.getBoolean("done");
                        

                        // Display the parsed data
                        System.out.println("Model: " + model);
                        System.out.println("Response Text: " + responseText);
                        System.out.println("Done: " + done);
                        System.out.println("----------");
                       //  JOptionPane.showMessageDialog(null, "callLLMChat -> "+responseText);
                        streamOutput.append(responseText);

                        if (outputArea != null) {
                            // Append response to the RSyntaxTextArea
                            SwingUtilities.invokeLater(() -> {
                                outputArea.append(responseText);
                                 //outputArea.requestFocusInWindow();
                            });
                        }

                        // Break if response is marked as done
                        if (done) {
                            break;
                        }
                    }
                    
                    //reconstruct for further processing.
                     responseJ = new JSONObject();
                     //getJSONObject("message").getString("content");
                     JSONObject message=new JSONObject();
                     message.put("content", streamOutput.toString());
                     responseJ.put("message", message);
                
                } else {
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Response Body: " + response.toString());

                    responseJ = new JSONObject(response.toString());
                }
                
            }
            
            // Close connection
            connection.disconnect();
            
           
        }   catch (MalformedURLException ex) {
            Logger.getLogger(OllamaHelpers.class.getName()).log(Level.SEVERE, null, ex);
              JOptionPane.showMessageDialog(null, "callLLMChat -> "+ex.getMessage()+tools.toString(1));
        } catch (Exception ex) {
            Logger.getLogger(OllamaHelpers.class.getName()).log(Level.SEVERE, null, ex);
              JOptionPane.showMessageDialog(null, "callLLMChat -> "+ex.getMessage()+tools.toString(1));
        }
       return responseJ;
    }
    
    public static JSONObject callEmbeddings(String urlString, String jsonInputString, String model) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set request properties
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        // Write the JSON input string to the request body
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Get the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        // Convert the response string to JSONObject
        return new JSONObject(response.toString());
    }

    // Helper method to extract embeddings from a single input response
    public static double[] extractEmbeddings(JSONObject response) {
        JSONArray embeddingsArray = response.getJSONArray("embeddings").getJSONArray(0); // First input's embeddings
        return jsonArrayToDoubleArray(embeddingsArray);
    }

    // Helper method to extract embeddings from a specific input index in a multiple input response
    public static double[] extractEmbeddings(JSONObject response, int inputIndex) {
        JSONArray embeddingsArray = response.getJSONArray("embeddings").getJSONArray(inputIndex); // Specific input's embeddings
        return jsonArrayToDoubleArray(embeddingsArray);
    }

    // Helper method to convert JSONArray to double[]
    public  static double[] jsonArrayToDoubleArray(JSONArray jsonArray) {
        double[] result = new double[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            result[i] = jsonArray.getDouble(i);
        }
        return result;
    }
    
        public static double[] getChatEmbedding(List<String> chatMessages) {
        try {
            // Concatenate all chat messages into a single string
            String chatContent = String.join(" ", chatMessages);
            // API URL
            
            JSONObject request=new JSONObject();
            request.put("model", "nomic-embed-text:latest");
            request.put("input", chatContent);
            JSONObject singleResponse =OllamaHelpers.callEmbeddings(OLLAMA_EP+"/api/embed", request.toString(), "nomic-embed-text:latest");
            
            // Extract embeddings from the response
            return OllamaHelpers.extractEmbeddings(singleResponse);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
        
    /**
     * 
     * @param model
     * @param messagesArray
     * @return 
     */
    
    public static JSONObject callApiAndHandleResponse(String model, JSONArray messagesArray, boolean stream, RSyntaxTextArea outputArea) {
        try {
            // Construct the JSON payload
            JSONObject payload = new JSONObject();
            payload.put("model", model);

            //if(messagesArray.length()==0){// for first time call
            JSONObject messageSystemObject = new JSONObject();
            messageSystemObject.put("role", "system");
            messageSystemObject.put("content", "You are a helpful customer support assistant. Use the supplied tools to assist the user. Do not assume required properties values for tools, always ask for clarification to user."); //forecast for a location  What is the weather today 
            messagesArray.put(messageSystemObject);
            // }

            System.out.println(messagesArray.toString());

            // Get the user's message from the text field
            String userInput = "test"; // Replace with actual input

            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", userInput); //forecast for a location  What is the weather today // JOptionPane.showInputDialog("What is your question?")
            messagesArray.put(messageObject);
            payload.put("messages", messagesArray);

            payload.put("stream", stream);

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
            URL url = new URL(value_name + "/api/chat");

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

                if (stream) {

                    String line = "";
                    // Read each line (JSON object) from the stream
                    while ((line = in.readLine()) != null) {
                        // Parse each line as a JSON object
                        JSONObject jsonObject = new JSONObject(line);

                        // Extract values from the JSON object
                        String responseText = jsonObject.getString("response");
                        boolean done = jsonObject.getBoolean("done");

                        // Display the parsed data
                        System.out.println("Model: " + model);
                        System.out.println("Response Text: " + responseText);
                        System.out.println("Done: " + done);
                        System.out.println("----------");
                        //outputArea.append(responseText);

                        if (outputArea != null) {
                            // Append response to the RSyntaxTextArea
                            SwingUtilities.invokeLater(() -> {
                                outputArea.append(responseText + " ");
                            });
                        }

                        // Break if response is marked as done
                        if (done) {
                            break;
                        }
                    }
                } else {
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }

                in.close();

                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());

                // Extract tool calls and execute corresponding functions
                JSONObject messageObjectResponse = jsonResponse.getJSONObject("message");
                if (messageObjectResponse.has("tool_calls")) {
                    JSONArray toolCallsArray = messageObjectResponse.getJSONArray("tool_calls");

                    for (int i = 0; i < toolCallsArray.length(); i++) {
                        JSONObject toolCall = toolCallsArray.getJSONObject(i);
                        JSONObject functionResponse = toolCall.getJSONObject("function");
                        String functionName = functionResponse.getString("name");

                        // Execute the registered function
                        FunctionHandler handler = IDEHelper.getFunctionHandlers().get(functionName);
                        if (handler != null) {
                            JSONObject arguments = functionResponse.getJSONObject("arguments");
                            String funcVal = handler.execute(arguments);

                            JSONObject messageObjectT = new JSONObject();
                            messageObjectT.put("role", messageObjectResponse.getString("role"));
                            messageObjectT.put("content", funcVal); //forecast for a location  What is the weather today // JOptionPane.showInputDialog("What is your question?")
                            messagesArray.put(messageObjectT);

                            //callApiAndHandleResponse( messagesArray);
                            return messageObjectT;
                        } else {
                            System.out.println("No handler registered for function: " + functionName);
                            JOptionPane.showConfirmDialog(null, "No handler registered for function: " + functionName);
                        }
                    }
                } else {

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
            JOptionPane.showConfirmDialog(null, e.getMessage());
        }
        return null;
    }
    
    /**
     * Calls the Google Gemini API and handles the response, including function calls.
     *
     * @param model The Gemini model name (e.g., "gemini-1.5-flash")
     * @param conversationHistory A JSONArray containing the conversation history in Gemini format.
     *                            Each element should be a JSONObject like:
     *                            {"role": "user"|"model", "parts": [{"text": "..."}]}
     * @param userInput The latest user message to add to the conversation.
     * @param outputArea The text area to display the final text response (optional).
     * @return JSONObject representing the last message added to the conversation
     *         (either the model's text response or the result of a function call).
     *         Returns null on failure.
     */
    public static JSONObject callGeminiApiAndHandleResponse(String model, JSONArray conversationHistory, String userInput, RSyntaxTextArea outputArea) {
        try {
            // --- Get API Key ---
            String apiKey = System.getenv("GEMINI");
            if (apiKey == null || apiKey.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Error: GEMINI_API_KEY environment variable not set.", "API Key Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            // --- Construct Gemini Payload ---
            JSONObject payload = new JSONObject();
            JSONArray contentsArray = new JSONArray(conversationHistory.toString()); // Deep copy to avoid modifying original

            // --- Handle System Prompt (if history is empty) ---
            // Gemini prefers system instructions via the 'systemInstruction' field or
            // as part of the initial turns in 'contents'. Here's one way for 'contents':
            if (contentsArray.isEmpty()) {
                 JSONObject systemUserPart = new JSONObject()
                     .put("role", "user")
                     .put("parts", new JSONArray().put(new JSONObject().put("text", "You are a helpful customer support assistant. Use the supplied tools to assist the user. Do not assume required properties values for tools, always ask for clarification to user.")));
                 JSONObject systemModelPart = new JSONObject()
                     .put("role", "model")
                     .put("parts", new JSONArray().put(new JSONObject().put("text", "Understood. I will act as a helpful customer support assistant and request clarification for tool parameters.")));
                 contentsArray.put(systemUserPart);
                 contentsArray.put(systemModelPart);
            }
            // --- Alternatively, use systemInstruction field (preferred if model supports it) ---
            // JSONObject systemInstruction = new JSONObject()
            //      .put("parts", new JSONArray().put(new JSONObject().put("text", "System prompt here")));
            // payload.put("systemInstruction", systemInstruction);


            // --- Add User Message --- Not required
//            JSONObject userMessage = new JSONObject();
//            userMessage.put("role", "user");
//            JSONArray userParts = new JSONArray();
//            userParts.put(new JSONObject().put("text", userInput));
//            userMessage.put("parts", userParts);
//            contentsArray.put(userMessage);

            payload.put("contents", contentsArray);
            
            JOptionPane.showMessageDialog(null, "callGeminiApiAndHandleResponse "+payload.toString(1));

            // --- Add Tools ---
//            if (IDEHelper.getFunctionHandlers() != null && !IDEHelper.getFunctionHandlers().isEmpty()) {
//                JSONArray functionDeclarationsArray = new JSONArray();
//                for (FunctionHandler handler : IDEHelper.getFunctionHandlers().values()) {
//                    // Assuming getFunctionJSON() returns the declaration in the format Gemini expects:
//                    // { "name": "...", "description": "...", "parameters": { ... OpenAPI Schema ... } }
//                    functionDeclarationsArray.put(handler.getFunctionJSON());
//                }
//                if (functionDeclarationsArray.length() > 0) {
//                     JSONObject toolsObject = new JSONObject();
//                     toolsObject.put("functionDeclarations", functionDeclarationsArray);
//                     payload.put("tools", new JSONArray().put(toolsObject)); // Gemini expects 'tools' to be an array containing the tool config
//                }
//            }
             // --- Optional: Add tool configuration (e.g., auto-calling) ---
            // JSONObject toolConfig = new JSONObject();
            // toolConfig.put("functionCallingConfig", new JSONObject().put("mode", "AUTO")); // Or "ANY" or "NONE"
            // payload.put("toolConfig", toolConfig);


            System.out.println("Gemini Request Payload:");
            System.out.println(payload.toString(2)); // Pretty print JSON

            // --- API Call ---
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            URL url = new URL(apiUrl);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Send JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] inputBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(inputBytes, 0, inputBytes.length);
            }

            // --- Handle Response ---
            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();
            BufferedReader reader;

            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) { // 2xx Success codes
                 reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                 reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            System.out.println("Gemini Response Code: " + responseCode);
            System.out.println("Gemini Response Body:");
            System.out.println(response.toString()); // Log raw response

            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                JSONObject jsonResponse = new JSONObject(response.toString());

                // Check for potential content filtering or lack of response
                 if (!jsonResponse.has("candidates") || jsonResponse.getJSONArray("candidates").isEmpty()) {
                     System.err.println("Error: No candidates found in Gemini response.");
                     String finishReason = jsonResponse.optQuery("/promptFeedback/blockReason").toString();
                     if (finishReason != null) {
                        System.err.println("Potential Block Reason: " + finishReason);
                        JOptionPane.showMessageDialog(null,"Request blocked, reason: " + finishReason, "API Error", JOptionPane.WARNING_MESSAGE);
                     } else {
                         JOptionPane.showMessageDialog(null,"Gemini returned no candidates.", "API Error", JOptionPane.ERROR_MESSAGE);
                     }
                     return null; // Indicate failure or lack of response
                 }


                JSONObject firstCandidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
                JSONObject modelContent = firstCandidate.getJSONObject("content"); // This is the content object from the model {role: "model", parts: [...]}

                 // --- Check for Function Call ---
                 if (modelContent.has("parts")) {
                     JSONArray parts = modelContent.getJSONArray("parts");
                     if (!parts.isEmpty() && parts.getJSONObject(0).has("functionCall")) {
                         JSONObject functionCall = parts.getJSONObject(0).getJSONObject("functionCall");
                         String functionName = functionCall.getString("name");
                         JSONObject functionArgs = functionCall.getJSONObject("args");

                         System.out.println("Function Call Requested: " + functionName);
                         System.out.println("Arguments: " + functionArgs.toString());

                         FunctionHandler handler = IDEHelper.getFunctionHandlers().get(functionName);
                         if (handler != null) {
                             String functionResultValue = handler.execute(functionArgs); // Execute the function

                             // --- Prepare the function result message for the *next* API call ---
                             JSONObject functionResultContent = new JSONObject();
                             functionResultContent.put("role", "function"); // Use 'function' role for results

                             JSONObject functionResponsePart = new JSONObject();
                             functionResponsePart.put("name", functionName);
                             // Gemini expects the result within a 'response' object, often containing structured data
                             // For simplicity, putting the raw string result here. Adjust if your functions return JSON.
                             functionResponsePart.put("response", new JSONObject().put("result", functionResultValue));

                             JSONArray functionResultParts = new JSONArray();
                             functionResultParts.put(new JSONObject().put("functionResponse", functionResponsePart));
                             functionResultContent.put("parts", functionResultParts);

                             System.out.println("Returning Function Result Message for next call:");
                             System.out.println(functionResultContent.toString(2));

                             // Add the model's request AND the function result to the history for the *next* call
                             // (The calling code should handle adding these before the next iteration)
                             // We return the function *result* message, similar to the original code's pattern.
                             return functionResultContent;

                         } else {
                             System.err.println("Error: No handler registered for function: " + functionName);
                             JOptionPane.showMessageDialog(null, "No handler registered for function: " + functionName, "Function Error", JOptionPane.ERROR_MESSAGE);
                             // Decide how to proceed: maybe return an error message or stop.
                             // Returning the model's request message might be confusing. Let's return null.
                              return null;
                         }
                     }
                 }

                 // --- Handle Regular Text Response ---
                 if (modelContent.has("parts")) {
                    JSONArray parts = modelContent.getJSONArray("parts");
                     if (!parts.isEmpty() && parts.getJSONObject(0).has("text")) {
                         String responseText = parts.getJSONObject(0).getString("text");
                         System.out.println("Model Text Response: " + responseText);

                         if (outputArea != null) {
                             final String textToAppend = responseText; // Final for lambda
                             SwingUtilities.invokeLater(() -> {
                                 outputArea.append(textToAppend); // Append the complete text response
                                 outputArea.append("\n"); // Add newline for clarity
                             });
                         }

                         // Return the model's complete content message object
                         return modelContent;
                     }
                 }

                 // If no function call and no text part found (shouldn't usually happen with successful response)
                 System.err.println("Warning: Gemini response candidate has no function call or text part.");
                 return modelContent; // Return the model content anyway

            } else {
                // --- Handle API Errors ---
                System.err.println("POST request failed with response code: " + responseCode);
                System.err.println("Error Response: " + response.toString());
                 try {
                     // Try parsing the error response as JSON
                     JSONObject errorResponse = new JSONObject(response.toString());
                     if (errorResponse.has("error")) {
                         JSONObject errorDetails = errorResponse.getJSONObject("error");
                         String errorMessage = errorDetails.optString("message", "Unknown Gemini API Error");
                         String errorStatus = errorDetails.optString("status", "");
                         JOptionPane.showMessageDialog(null, "Gemini API Error (" + errorStatus + "): " + errorMessage, "API Error", JOptionPane.ERROR_MESSAGE);
                     } else {
                         JOptionPane.showMessageDialog(null, "API request failed (Code: " + responseCode + "). Response: " + response.toString(), "API Error", JOptionPane.ERROR_MESSAGE);
                     }
                 } catch (Exception jsonEx) {
                    // If error response wasn't JSON
                    JOptionPane.showMessageDialog(null, "API request failed (Code: " + responseCode + "). Response: " + response.toString(), "API Error", JOptionPane.ERROR_MESSAGE);
                 }

                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,"An exception occurred: " + e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
  
     private static final Logger LOGGER = Logger.getLogger(OllamaHelpers.class.getName());
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    /**
     * Calls the Gemini API for streaming content generation.
     *
     * @param apiKey       Your Google API Key.
     * @param modelName    The name of the Gemini model (e.g., "gemini-1.5-flash-latest", "gemini-pro"). Note: The example uses gemini-2.0-flash, ensure this model is available or use a suitable alternative.
     * @param prompt       The text prompt to send to the model.
     * @param chunkConsumer A consumer function that will be called with each text chunk received from the stream.
     *                     Can be null if you only need the final aggregated response.
     * @return A JSONObject containing the aggregated response details after the stream finishes,
     *         or an error structure if the call fails before streaming starts.
     *         The structure might vary based on Gemini's final output format for aggregated streams,
     *         currently it aggregates text. Returns empty JSONObject on stream reading error.
     */
    public static JSONObject callGeminiStream( String modelName, JSONArray contents,   RSyntaxTextArea outputArea) {
        // --- Get API Key ---
            String apiKey = System.getenv("GEMINI");
            if (apiKey == null || apiKey.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Error: GEMINI_API_KEY environment variable not set.", "API Key Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        StringBuilder aggregatedResponseText = new StringBuilder();
        JSONObject finalResponse = new JSONObject(); // To hold aggregated info if needed

        // Construct the API endpoint URL
        // Example uses gemini-2.0-flash, ensure this is correct and available.
        // Using gemini-1.5-flash-latest as a common default if needed.
        // Adjust modelName parameter or this default as necessary.
        // String effectiveModel = "gemini-2.0-flash"; // As per curl example
        String effectiveModel = modelName; // Use the provided model name
        String apiUrl = GEMINI_API_BASE_URL + effectiveModel + ":streamGenerateContent?alt=sse&key=" + apiKey;

        HttpURLConnection connection = null;

        try {
            // --- 1. Construct JSON Payload ---
            JSONObject payload = new JSONObject();
//            JSONArray contents = new JSONArray();
//            JSONObject content = new JSONObject();
//            JSONArray parts = new JSONArray();
//            JSONObject part = new JSONObject();
//
//            part.put("text", prompt);
//            parts.put(part);
//            content.put("parts", parts);
//            // Optional: Add role if needed for multi-turn chat history, default is "user"
//            // content.put("role", "user");
//            contents.put(content);
            payload.put("contents", contents);

            // Optional: Add generationConfig, safetySettings etc. here
            // JSONObject generationConfig = new JSONObject();
            // generationConfig.put("temperature", 0.7);
            // payload.put("generationConfig", generationConfig);

            String jsonString = payload.toString();
            LOGGER.log(Level.INFO, "Gemini Request URL: {0}", apiUrl);
            LOGGER.log(Level.INFO, "Gemini Request Payload: {0}", jsonString);

            // --- 2. Create URL and Open Connection ---
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();

            // --- 3. Set Request Method and Headers ---
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true); // Indicate we are sending data
            // connection.setFixedLengthStreamingMode(jsonString.getBytes(StandardCharsets.UTF_8).length); // Good practice
            // Gemini API might not strictly require Content-Length if chunked encoding is used implicitly by setDoOutput(true)

            // Optional: Set timeouts
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(60000 * 5); // 5 minutes for potentially long streams

            // --- 4. Write JSON data to output stream ---
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            // --- 5. Read Response ---
            int responseCode = connection.getResponseCode();
            LOGGER.log(Level.INFO, "Response Code: {0}", responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Use try-with-resources for BufferedReader
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // SSE format: "data: {JSON_CHUNK}"
                        if (line.startsWith("data:")) {
                            String jsonData = line.substring(5).trim(); // Remove "data:" prefix and trim whitespace
                            if (!jsonData.isEmpty()) {
                                try {
                                    JSONObject jsonObject = new JSONObject(jsonData);
                                    // Navigate to the text part: candidates[0].content.parts[0].text
                                    // Add null checks and error handling for robustness
                                    if (jsonObject.has("candidates")) {
                                        JSONArray candidates = jsonObject.getJSONArray("candidates");
                                        if (candidates.length() > 0) {
                                            JSONObject firstCandidate = candidates.getJSONObject(0);
                                            if (firstCandidate.has("content") && firstCandidate.getJSONObject("content").has("parts")) {
                                                JSONArray responseParts = firstCandidate.getJSONObject("content").getJSONArray("parts");
                                                if (responseParts.length() > 0 && responseParts.getJSONObject(0).has("text")) {
                                                    String textChunk = responseParts.getJSONObject(0).getString("text");
                                                    aggregatedResponseText.append(textChunk);
                                                    
                                                    if (outputArea != null) {
                                                        // Append response to the RSyntaxTextArea
                                                        SwingUtilities.invokeLater(() -> {
                                                            outputArea.append(textChunk);
                                                            //outputArea.requestFocusInWindow();
                                                        });
                                                    }

                                                    // Call the consumer with the new chunk
//                                                    if (chunkConsumer != null) {
//                                                         // If updating a Swing component, wrap in invokeLater
//                                                        // SwingUtilities.invokeLater(() -> chunkConsumer.accept(textChunk));
//                                                        // Otherwise, call directly:
//                                                        chunkConsumer.accept(textChunk);
//                                                    }
                                                }
                                            }
                                             // Optional: Check finishReason, safetyRatings etc. from firstCandidate if needed
                                            // String finishReason = firstCandidate.optString("finishReason", "");
                                            // if (!finishReason.isEmpty() && !"STOP".equals(finishReason)) { // Or other relevant reasons
                                                 // Potentially handle different finish reasons
                                            // }
                                        }
                                    }
                                     // Optional: Check promptFeedback
                                    // if (jsonObject.has("promptFeedback")) { ... }

                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Failed to parse JSON chunk: " + jsonData, e);
                                    // Decide how to handle parsing errors - skip chunk, log, etc.
                                }
                            }
                        } else if (!line.trim().isEmpty()) {
                            // Log lines that are not empty and don't start with "data:"
                            // SSE uses other event types or comments (lines starting with ':')
                            // but Gemini primarily uses 'data:'
                            LOGGER.log(Level.FINE, "Received non-data line: {0}", line);
                        }
                        // Empty lines separate SSE messages, handled by readLine loop
                    }
                } // BufferedReader closed automatically here

                // After stream finishes, populate the final response object (example)
                 // Gemini stream doesn't typically send a single aggregated JSON at the end,
                 // but we can structure our return value similarly to the reference if needed.
                JSONObject aggregatedContent = new JSONObject();
                JSONArray aggregatedParts = new JSONArray();
                JSONObject aggregatedPart = new JSONObject();
                aggregatedPart.put("text", aggregatedResponseText.toString());
                aggregatedParts.put(aggregatedPart);
                aggregatedContent.put("parts", aggregatedParts);
                // You might want to add aggregated safety ratings or other final info if available/needed
                finalResponse.put("aggregatedContent", aggregatedContent); // Custom structure
                finalResponse.put("status", "success");


            } else {
                // Handle error response code
                LOGGER.log(Level.SEVERE, "API call failed with response code: {0}", responseCode);
                String errorResponse = readErrorStream(connection);
                LOGGER.log(Level.SEVERE, "Error response body: {0}", errorResponse);
                finalResponse.put("status", "error");
                finalResponse.put("code", responseCode);
                try {
                     // Try to parse error response as JSON
                     finalResponse.put("errorDetails", new JSONObject(errorResponse));
                } catch (Exception e) {
                    // If not JSON, put raw string
                     finalResponse.put("errorDetails", errorResponse);
                }
            }

        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Invalid API URL.", e);
            finalResponse.put("status", "error");
            finalResponse.put("message", "Malformed URL: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Network or IO error.", e);
            finalResponse.put("status", "error");
            finalResponse.put("message", "IO Exception: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error constructing JSON payload.", e);
            finalResponse.put("status", "error");
            finalResponse.put("message", " Exception: " + e.getMessage());
       } finally {
            // --- 6. Close connection ---
            if (connection != null) {
                connection.disconnect();
            }
        }

        // Log the final aggregated text
        LOGGER.log(Level.INFO, "Aggregated Response Text:\n{0}", aggregatedResponseText.toString());

        return finalResponse;
    }

    // Helper method to read error stream
    private static String readErrorStream(HttpURLConnection connection) {
        StringBuilder errorResponse = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
        } catch (IOException | NullPointerException e) { // getErrorStream might return null
           LOGGER.log(Level.WARNING, "Could not read error stream.", e);
            return "Could not read error stream (Response Code: " + getResponseCodeSafe(connection) + ")";
        }
        return errorResponse.toString();
    }

     // Helper to safely get response code even if connection failed earlier
    private static int getResponseCodeSafe(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            return -1; // Indicate failure to get code
        }
    }

     public static String selectModelName() {
        String[] modelNames = fetchModelNames(); // Call your function to get the models

        if (modelNames.length == 0) {
            JOptionPane.showMessageDialog(null, "No models found.");
            return null;
        }

        // Create a JComboBox with model names as options
        JComboBox<String> modelComboBox = new JComboBox<>(modelNames);

        // Show the JComboBox in a JOptionPane dialog
        int result = JOptionPane.showConfirmDialog(
                null, modelComboBox, "Select a Model", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            return (String) modelComboBox.getSelectedItem();
        } else {
            return null;
        }
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
    
    
    public static StringBuilder makePostRequest(String jsonPayload, RSyntaxTextArea outputTextArea) throws Exception {
        URL url = new URL(OLLAMA_EP + "/api/generate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        StringBuilder outputCollected = new StringBuilder();

        // Send JSON payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read and parse the JSON response stream
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String line;
            StringBuilder response = new StringBuilder();

            // Read each line (JSON object) from the stream
            while ((line = br.readLine()) != null) {
                // Parse each line as a JSON object
                JSONObject jsonObject = new JSONObject(line);

                // Extract values from the JSON object
                String model = jsonObject.getString("model");
                String responseText = jsonObject.getString("response");
                boolean done = jsonObject.getBoolean("done");

                // Display the parsed data
                System.out.println("Model: " + model);
                System.out.println("Response Text: " + responseText);
                System.out.println("Done: " + done);
                System.out.println("----------");
                outputCollected.append(responseText);

                if (outputTextArea != null) {
                    // Append response to the RSyntaxTextArea
                    SwingUtilities.invokeLater(() -> {
                        outputTextArea.append(responseText + " ");
                    });
                }

                // Break if response is marked as done
                if (done) {
                    break;
                }
            }
        }
        return outputCollected;
    }
      
       /**
     * Makes a request with stream=false and processes a single JSON object response.
     * @param requestUrl The endpoint URL.
     * @param model The model to use.
     * @param prompt The prompt to send.
     */
    public static JSONObject makeNonStreamedRequest(  String model, String prompt, boolean jsonFormat) throws Exception {
        URL url = new URL(OLLAMA_EP+"/api/generate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Prepare JSON payload
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("prompt", prompt);
        jsonObject.put("stream", false);
        if(jsonFormat)jsonObject.put("format",   "json" );

        // Send JSON payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonObject.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read and parse the single JSON response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }

            // Parse JSON object from the response
            JSONObject jsonObjectr = new JSONObject(responseBuilder.toString());

            return jsonObjectr;
        }
        
        
    }
    
        public static List<String> chunkSentences(String text, int sentencesPerChunk) {
        List<String> chunks = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        iterator.setText(text);

        int start = iterator.first();
        int sentenceCount = 0;
        StringBuilder chunkBuilder = new StringBuilder();

        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            sentenceCount++;
            chunkBuilder.append(text, start, end).append(" ");

            if (sentenceCount == sentencesPerChunk) {
                chunks.add(chunkBuilder.toString().trim());
                chunkBuilder.setLength(0);
                sentenceCount = 0;
            }
        }

        // Add any remaining sentences in the last chunk
        if (!chunkBuilder.isEmpty() && !chunkBuilder.toString().trim().isEmpty()) {
            chunks.add(chunkBuilder.toString().trim());
        }

        return chunks;
    }
        
    public static void main(String[] args) {
        
        
        //========================GEMINI TESTING========================
        
          // Replace with your actual API Key
        String apiKey = System.getenv("GEMINI"); // Read from environment variable
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: GOOGLE_API_KEY environment variable not set.");
             // Or prompt user, read from file, etc.
             // apiKey = "YOUR_API_KEY"; // Hardcoding is discouraged
            return;
        }

        String model = "gemini-1.5-pro"; // Or "gemini-pro", "gemini-2.0-flash" etc.
        String prompt = "Write a very short, cute story about a curious kitten discovering a ball of yarn.";

        System.out.println("--- Calling Gemini Stream API ---");
        System.out.println("Model: " + model);
        System.out.println("Prompt: " + prompt);
        System.out.println("\n--- Streaming Response ---");

        // Define the consumer to print chunks as they arrive
        Consumer<String> chunkPrinter = chunk -> {
            System.out.print(chunk); // Print chunks without newline to see aggregation
            // System.out.flush(); // Ensure immediate output if needed
        };

        // Call the streaming method
                    JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            // Optional: Add role if needed for multi-turn chat history, default is "user"
            // content.put("role", "user");
            contents.put(content);
        JSONObject result = callGeminiStream(  model, contents, new RSyntaxTextArea()); //May not work unless used with consumer

        System.out.println("\n\n--- Stream Finished ---");
        System.out.println("Final Result Object:");
        // Use toString(2) for pretty printing JSON
        System.out.println(result.toString(2));

        // You can access the aggregated text from the result if needed
        if ("success".equals(result.optString("status"))) {
             try {
                 String fullText = result.getJSONObject("aggregatedContent")
                                        .getJSONArray("parts")
                                        .getJSONObject(0)
                                        .getString("text");
                System.out.println("\n--- Final Aggregated Text (from result object) ---");
                System.out.println(fullText);
             } catch( Exception e) {
                 System.err.println("Error extracting aggregated text from final result.");
             }
        } else {
            System.err.println("\nAPI call failed. Details: " + result.opt("errorDetails"));
        }
    
//=============================GEMINI TESTING COMPLETES====================
        
        
        //Test Vision Model
         String apiUrl = "http://localhost:11434/api/chat";
          model = "llama3.2-vision";
        String userMessage = "what is in this image?";
        String imagePath = "C:\\Users\\manoj.kumar\\Pictures\\Screenshots\\Screenshot 2024-11-11 122916.png"; // Replace with your image path

        try {
            // Convert image to Base64
            String base64ImageData = convertImageToBase64(imagePath);

            // Create JSON payload
            String jsonPayload = createJsonPayload(model, userMessage, base64ImageData);

            // Make the POST request and handle the response
            String response = callLLMVision( jsonPayload);
            System.out.println("Response: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
          String text = "This is the first sentence. Here is another one! Is this the third sentence? Yes, it is. And this is the fifth one. Finally, this is the sixth sentence.";

        int sentencesPerChunk = 4;
        List<String> chunks = chunkSentences(text, sentencesPerChunk);
        for (String chunk : chunks) {
            System.out.println(chunk);
            System.out.println("---");
        }
        
        
        try {
            // Define the endpoint URL and JSON payload
            String url = "http://localhost:11434/api/generate";
            String payload = "{\"model\": \"qwen2.5:32b-instruct-q3_K_S\", \"prompt\": \"Why is the sky blue?\"}";

            // Make HTTP POST request and parse the response
            makePostRequest( payload,null);
            
            makeNonStreamedRequest("llama3.2:1b", "Why is the sky blue?", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
        try {
            // API URL
            String url = "http://localhost:11434/api/embed";

            // Example for Single Input
            String singleInput = "{ \"model\": \"nomic-embed-text:latest\", \"input\": \"Why is the sky blue?\" }";
            JSONObject singleResponse = callEmbeddings(url, singleInput, "nomic-embed-text:latest");
            System.out.println("Single Input Response:");
            System.out.println(singleResponse.toString(4));

            // Extract embeddings for single input
            double[] singleEmbeddings = extractEmbeddings(singleResponse);
            System.out.println("\nSingle Input Embeddings:");
            for (double value : singleEmbeddings) {
                System.out.print(value + " ");
            }

            // Example for Multiple Inputs
            String multipleInput = "{ \"model\": \"nomic-embed-text:latest\", \"input\": [\"Why is the sky blue?\", \"Why is the grass green?\"] }";
            JSONObject multipleResponse = callEmbeddings(url, multipleInput, "nomic-embed-text:latest");
            System.out.println("\n\nMultiple Input Response:");
            System.out.println(multipleResponse.toString(4));

            // Extract embeddings for multiple inputs (just the first input)
            double[] multipleEmbeddings = extractEmbeddings(multipleResponse, 0);
            System.out.println("\nFirst Embeddings from Multiple Input:");
            for (double value : multipleEmbeddings) {
                System.out.print(value + " ");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
     /**
     * Makes a POST request to the given URL with the provided JSON payload.
     * 
     * @param requestUrl  The URL to send the request to.
     * @param jsonPayload The JSON payload as a String.
     * @return The server's response as a String.
     * @throws Exception If an error occurs during the HTTP request.
     */
    public static String callLLMVision( String jsonPayload) throws Exception {
      //  URL url = new URL(requestUrl);
         URL url = new URL(OLLAMA_EP+"/api/chat");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Send JSON payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read response from the server
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }

        return response.toString();
    }
    
        /**
     * Creates a JSON payload for the chat API request.
     * 
     * @param model           The model to use.
     * @param userMessage     The message content from the user.
     * @param base64ImageData The base64-encoded image data.
     * @return JSON payload as a String.
     */
    public static String createJsonPayload(String model, String userMessage, String base64ImageData) {
        JSONObject jsonObject = new JSONObject();

        JSONObject options = new JSONObject();
        options.put("num_ctx", 8192);

        jsonObject.put("options", options);

        jsonObject.put("model", model);
        jsonObject.put("stream", false);

        JSONArray messages = new JSONArray();
        JSONObject userMessageObject = new JSONObject();
        userMessageObject.put("role", "user");
        userMessageObject.put("content", userMessage);
        userMessageObject.put("images", new JSONArray().put(base64ImageData));
        messages.put(userMessageObject);

        jsonObject.put("messages", messages);

        return jsonObject.toString();
    }
    
        /**
     * Converts an image file to a Base64-encoded string.
     * 
     * @param imagePath Path to the image file.
     * @return Base64-encoded string of the image.
     * @throws IOException If an error occurs during file reading.
     */
    public static String convertImageToBase64(String imagePath) throws IOException {
        File file = new File(imagePath);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] imageBytes = fileInputStream.readAllBytes(); // Read the entire image file as a byte array
            return Base64.getEncoder().encodeToString(imageBytes); // Encode the byte array to Base64
        }
    }
    
    ///Prompts
    public static String CODE_REVIEW_FORMAT="{\n" +
"  \"fileName\": \"MyClass.java\",\n" +
"  \"filePath\": \"src/main/java/com/example/MyClass.java\",\n" +
"  \"issues\": [\n" +
"    {\n" +
"      \"lineNumber\": 23,\n" +
"      \"severity\": \"high\",\n" +
"      \"description\": \"Null pointer exception risk. Add a null check for 'object'.\",\n" +
"      \"tags\": [\"null-check\", \"exception-handling\"]\n" +
"    },\n" +
"    {\n" +
"      \"lineNumber\": 45,\n" +
"      \"severity\": \"medium\",\n" +
"      \"description\": \"Magic number used. Replace with a constant.\",\n" +
"      \"tags\": [\"magic-number\", \"best-practices\"]\n" +
"    }\n" +
"  ],\n" +
"  \"summary\": {\n" +
"    \"totalIssues\": 2,\n" +
"    \"highSeverity\": 1,\n" +
"    \"mediumSeverity\": 1,\n" +
"    \"lowSeverity\": 0\n" +
"  }\n" +
"}";
    
    public static String TOOL_FORMAT="{\n" +
"  \"type\": \"function\",\n" +
"  \"function\": {\n" +
"    \"name\": \"function_name\",\n" +
"    \"description\": \"function_description\",\n" +
"    \"parameters\": {\n" +
"      \"type\": \"object\",\n" +
"      \"properties\": {\n" +
"        \"parameter1_name\": {\n" +
"          \"type\": \"parameter1_type\",\n" +
"          \"description\": \"parameter1_description\",\n" +
"          \"enum\": [\"optional_enum_value1\", \"optional_enum_value2\"]\n" +
"        },\n" +
"        \"parameter2_name\": {\n" +
"          \"type\": \"parameter2_type\",\n" +
"          \"description\": \"parameter2_description\",\n" +
"          \"enum\": [\"optional_enum_value1\", \"optional_enum_value2\"]\n" +
"        }\n" +
"      },\n" +
"      \"required\": [\"parameter1_name\", \"parameter2_name\"]\n" +
"    }\n" +
"  }\n" +
"}";
    
}
