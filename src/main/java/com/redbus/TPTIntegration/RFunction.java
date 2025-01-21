/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.redbus.TPTIntegration;

/**
 *
 * @author manoj.kumar
 */
import com.fasterxml.jackson.annotation.JsonProperty;

public class RFunction {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("parameters")
    private Parameters parameters;

    // Getters and setters for the fields
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}

  class Parameters {
    @JsonProperty("type")
    private String type;

    @JsonProperty("properties")
    private Properties properties;

    // Getters and setters for the fields
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}

  class Properties {
    @JsonProperty("model")
    private Parameter model;

    @JsonProperty("prompt")
    private Parameter prompt;

    // Getters and setters for the fields
    
    public Parameter getModel() {
        return model;
    }

    public void setModel(Parameter model) {
        this.model = model;
    }
    
    public Parameter getPrompt() {
        return prompt;
    }

    public void setPrompt(Parameter prompt) {
        this.prompt = prompt;
    }
}

  class Parameter {
    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type;

    @JsonProperty("enum")
    private String[] enumValues; // This can be a String array or null if empty

    // Getters and setters for the fields
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String[] getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(String[] enumValues) {
        this.enumValues = enumValues;
    }
}