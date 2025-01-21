/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;

/**
 *
 * @author manoj.kumar
 */
// Inner class for REST client history
import java.io.Serializable;

public class RestClientHistory implements Serializable {
    private static final long serialVersionUID = 4746311040410747881L; // Added serialVersionUID

    private final String id;
    private final String method;
    private final String url;
    private final String response;
    // Add a parameter to store request details 
    private final String request;

    public RestClientHistory(String id, String method, String url, String request, String response) {
        this.id = id;
        this.method = method;
        this.url = url;
        this.request = request; // Store the request details
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String getRequest() { // Add a getter for the request parameter
        return request;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "RestClientHistory{" +
                "id='" + id + '\'' +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", request='" + request + '\'' + // Include request in the string representation
                ", response='" + response + '\'' +
                '}';
    }
    
    
    /**
     * reserved for future.
     * @param functionJSON
     * @return
     * @throws JsonProcessingException 
     */
    public static RestClientHistory functionMapper(String functionJSON) throws JsonProcessingException{
    
         ObjectMapper objectMapper = new ObjectMapper();
        RFunction function = objectMapper.readValue(functionJSON, RFunction.class);
        
        //Parameters params = function.getParameters().getProperties();

        // Example usage of parameters
        //System.out.println("Model: " + params.getModel().getDescription());
        //System.out.println("Prompt: " + params.getPrompt().getDescription());
        return null;
    }
}

