package com.adaptivebp.modules.formbuilder.model.legacy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "FormField")
public class FormField {
    @Id
    private long id;
    private String name;
    private String elementType;
    private String placeHolder;
    private String value;
    private String model;
    private List<CustomOptions> options;
    private List<CustomRegularExpression> regularExpression;

    public FormField() {
        this.options = new ArrayList<>();
        this.regularExpression = new ArrayList<>();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getElementType() { return elementType; }
    public void setElementType(String elementType) { this.elementType = elementType; }
    public String getPlaceHolder() { return placeHolder; }
    public void setPlaceHolder(String placeHolder) { this.placeHolder = placeHolder; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<CustomOptions> getOptions() { return options; }
    public void setOptions(List<CustomOptions> options) { this.options = options; }
    public List<CustomRegularExpression> getRegularExpression() { return regularExpression; }
    public void setRegularExpression(List<CustomRegularExpression> regularExpression) { this.regularExpression = regularExpression; }
}
