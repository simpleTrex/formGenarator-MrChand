package com.adaptivebp.modules.formbuilder.model.legacy;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Actions {
    private String name;
    private String script;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }
}
