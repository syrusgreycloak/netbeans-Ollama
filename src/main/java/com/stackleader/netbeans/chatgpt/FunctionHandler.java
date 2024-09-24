/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.stackleader.netbeans.chatgpt;

import java.util.Map;
import org.json.JSONObject;

/**
 *
 * @author manoj.kumar
 */
// Interface for handling functions
interface FunctionHandler {
    String getFunctionName();
    String getDescription();
    JSONObject getFunctionJSON();
    Map<String, VariableDefinition> getVariables();
    String execute(JSONObject arguments);
}