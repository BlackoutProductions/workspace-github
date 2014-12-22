package com.kurt.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JSONItem {
    private Map<String, Object> properties = new HashMap<String, Object>();
    public JSONItem(String raw) {
        String temp = raw;
        // strip curly braces
        temp = temp.replace("{", "");
        temp = temp.replace("}", "");
        temp = temp.trim();
        
        // break into lines
        //String[] lines = temp.split(",");
        String[] lines;
        
        for (String line : lines)
        {
            line.trim();
            // break into key-value set
            String[] parts = line.split(":");
            String key = parts[0];
            String value = parts[1];
            // trim white space
            key = key.trim();
            value = key.trim();
            // replace quotes
            key.replace("\"", "");
            value.replace("\"", "");
            // store in map
            properties.put(key, value);
        }
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public Set<String> getKeys() {
        return properties.keySet();
    }
}
