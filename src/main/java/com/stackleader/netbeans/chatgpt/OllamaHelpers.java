/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
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
        
//        OLLAMA_MODELS_TOOLS.add("qwen2.5:1.5b");
//        OLLAMA_MODELS_TOOLS.add("llama3.2:latest");
//        OLLAMA_MODELS_TOOLS.add("nemotron-mini:latest");
//        OLLAMA_MODELS_TOOLS.add("llama3.1:latest");
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

//        if (tools == null) {
//            return callLLMChat(prompt, model, messages, tools);
//        } else
            if (OLLAMA_MODELS_TOOLS.contains(model)) {
                
                // Show the dialog with RSyntaxTextArea embedded
             JOptionPane.showMessageDialog(null,"callApiAndHandleResponse(model, messages)");  

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
    
    public static JSONObject callApiAndHandleResponse( String model,JSONArray messagesArray) {
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
                        JOptionPane.showConfirmDialog(null, "No handler registered for function: " + functionName);
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
        JOptionPane.showConfirmDialog(null, e.getMessage());
    }
        return null;
}
    
    public static JSONObject callLLMGenerate(String model,String prompt,boolean jsonFormat) {
    try {
        HttpClient httpClient = HttpClient.newHttpClient();
        // Build the request payload
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("model", model);
        requestPayload.put("prompt", prompt);
        //requestPayload.put("system", systemPrompt);
        requestPayload.put("format", jsonFormat ? "json" : "text");
        requestPayload.put("stream", false); // Set to true for streaming

        JSONObject options=new JSONObject();
        options.put("num_ctx", 4096);

        System.out.println("Request=>"+requestPayload.toString(1));
        // Create the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_EP+"/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload.toString()))
                .build();

         // Send the request and get the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        return new JSONObject(response);
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
    
    
      public static void makePostRequest(  String jsonPayload, RSyntaxTextArea outputTextArea) throws Exception {
         URL url = new URL(OLLAMA_EP+"/api/generate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

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
                
                if(outputTextArea!=null){
                     // Append response to the RSyntaxTextArea
                SwingUtilities.invokeLater(() -> {
                    outputTextArea.append(responseText + " ");
                });
                }

                // Break if response is marked as done
                if (done) break;
            }
        }
    }
      
       /**
     * Makes a request with stream=false and processes a single JSON object response.
     * @param requestUrl The endpoint URL.
     * @param model The model to use.
     * @param prompt The prompt to send.
     */
    public static JSONObject makeNonStreamedRequest(  String model, String prompt) throws Exception {
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
        
        
        //Test Vision Model
         String apiUrl = "http://localhost:11434/api/chat";
        String model = "llama3.2-vision";
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
            String payload = "{\"model\": \"llama3.2\", \"prompt\": \"Why is the sky blue?\"}";

            // Make HTTP POST request and parse the response
            makePostRequest( payload,null);
            
            makeNonStreamedRequest(  "llama3.2", "Why is the sky blue?");
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
    
}
