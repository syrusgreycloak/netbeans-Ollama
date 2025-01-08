/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;

/**
 *
 * @author manoj.kumar
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class JiraApiRequest {
    public static void main(String[] args) {
        String username = "manr@r**s.com";
        String apiToken = "ATA*****0imyk=A9A0038C";
        String urlString = "https://jira.redbus.in/rest/api/2/issue/*****/comment";

        try {
            // Create URL and connection
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");

            // Add Basic Authentication
            String auth = username + ":" + apiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
           // connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            
            // Add cookies (example cookies)
            String cookies = "_ga=G7******377; JSESSIONID=9B4*****87C9620; atlassian.xsrf.token=B****V_1e****0837dd23n";
            connection.setRequestProperty("Cookie", cookies);

            // Send request and handle response
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Response: " + response.toString());
                }
            } else {
                System.err.println("Request failed. Response Code: " + responseCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
