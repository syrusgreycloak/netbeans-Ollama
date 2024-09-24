/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author manoj.kumar
 */
public class VariableDefinition {
    private final String name;
    private final String type;
    private final String description;
    private final String[] enumValues;

    public VariableDefinition(String name, String type, String description, String[] enumValues) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.enumValues = enumValues;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", this.type);
        jsonObject.put("description", this.description);
        if (enumValues != null) {
            jsonObject.put("enum", new JSONArray(enumValues));
        }
        return jsonObject;
    }

    public String getName() {
        return name;
    }
}