package com.kurt.swapi.data;

import java.util.Date;
import java.util.Map;

public class Person extends BaseData {
    private Map<String, String> properties;
    private Map<String, String[]> externalproperties;
    private Date created;
    private Date modifed;
    
    public Person()
    {
        
    }
}
