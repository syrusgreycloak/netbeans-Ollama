/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;

import java.io.Serializable;

/**
 *
 * @author manoj.kumar
 */
// Inner class for REST client history
import java.io.Serializable;

public class RestClientHistory implements Serializable {
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
}

