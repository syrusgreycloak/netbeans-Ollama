/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

/**
 *
 * @author manoj.kumar
 */
 
import com.redbus.store.ChatSimilarityResult;
import com.redbus.store.MapDBVectorStore;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;
import org.openide.util.Exceptions;

public class MapDBVectorStoreTestWithEmbeddings {
    
    public static void main(String[] args) {
        // Initialize the vector store (change the path to your database file)
        MapDBVectorStore vectorStore = new MapDBVectorStore("chat_vectors.db", "vectors");

        // Example chats (some similar and some dissimilar to the query)
        List<String> chat1 = Arrays.asList("Hello, how are you?", "I'm fine, thank you.");
        List<String> chat2 = Arrays.asList("What's the weather like?", "It's sunny and warm today.");
        List<String> chat3 = Arrays.asList("Do you like coffee?", "Yes, I love it.");
        List<String> chat4 = Arrays.asList("I enjoy hiking in the mountains.", "The views are spectacular.");
        List<String> chat5 = Arrays.asList("Where can I find good pizza around here?", "Try the new Italian place downtown.");

        // Get embeddings for the chats
        double[] embedding1 = getChatEmbedding(chat1); // Similar chat to query
        double[] embedding2 = getChatEmbedding(chat2); // Moderately similar
        double[] embedding3 = getChatEmbedding(chat3); // Less similar
        double[] embedding4 = getChatEmbedding(chat4); // Dissimilar
        double[] embedding5 = getChatEmbedding(chat5); // Unrelated chat

        // Store chats with embeddings in the vector store
        vectorStore.storeChat("chat1", chat1, embedding1);
        vectorStore.storeChat("chat2", chat2, embedding2);
        vectorStore.storeChat("chat3", chat3, embedding3);
        vectorStore.storeChat("chat4", chat4, embedding4);
        vectorStore.storeChat("chat5", chat5, embedding5);

        // Retrieve and display a stored chat (positive case)
        System.out.println("Retrieved Chat 1: " + vectorStore.getChat("chat1"));

        // Find similar chats to a query (using chat1's content as the query for the example)
        double[] queryEmbedding = getChatEmbedding(chat1);  // Using chat1's content as the query

        // Find top 3 similar chats with similarity threshold 0.3
        List<ChatSimilarityResult> similarChats = vectorStore.findSimilarChats(queryEmbedding, 0.3, 3);

        System.out.println("\nPositive Case: Similar Chats to Query:");
        for (ChatSimilarityResult result : similarChats) {
            System.out.println("Chat ID: " + result.getChatId() + ", Similarity Score: " + result.getScore());
        }

        // Now test the negative case: a query that's dissimilar
        List<String> dissimilarChat = Arrays.asList("I want to learn about space travel.", "What are the latest updates?");
        double[] dissimilarQueryEmbedding = getChatEmbedding(dissimilarChat);

        // Search for similar chats (there should be no highly similar chats)
        List<ChatSimilarityResult> dissimilarChats = vectorStore.findSimilarChats(dissimilarQueryEmbedding, 0.5, 3);

        System.out.println("\nNegative Case: Similar Chats to Dissimilar Query:");
        if (dissimilarChats.isEmpty()) {
            System.out.println("No similar chats found for the dissimilar query.");
        } else {
            for (ChatSimilarityResult result : dissimilarChats) {
                System.out.println("Chat ID: " + result.getChatId() + ", Similarity Score: " + result.getScore());
            }
        }

        // Close the vector store when done
        vectorStore.close();
    }

    /**
     * Helper method to get chat embeddings from the Embedding API
     * @param chatMessages List of chat messages
     * @return double[] embedding array for the chat
     */
    private static double[] getChatEmbedding(List<String> chatMessages) {
        try {
            // Concatenate all chat messages into a single string
            String chatContent = String.join(" ", chatMessages);
            // API URL
            String url = "http://localhost:11434/api/embed";
            
            JSONObject request=new JSONObject();
            request.put("model", "nomic-embed-text:latest");
            request.put("input", chatContent);
            JSONObject singleResponse =OllamaHelpers.callEmbeddings(url, request.toString(), "nomic-embed-text:latest");
            
            // Extract embeddings from the response
            return OllamaHelpers.extractEmbeddings(singleResponse);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
}
