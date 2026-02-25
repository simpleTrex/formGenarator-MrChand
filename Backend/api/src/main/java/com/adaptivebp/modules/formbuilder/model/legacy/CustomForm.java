package com.adaptivebp.modules.formbuilder.model.legacy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Dynamic Business Object Model
 */
@Document(collection = "CustomForm")
public class CustomForm {

    public CustomForm updateToModel(CustomForm inModel) {
        if (inModel.getName().equals(getName())) {
            List<FormField> modelFields = inModel.getFields();
            List<FormField> newFields = new ArrayList<>();
            for (FormField modelField : modelFields) {
                for (FormField dataField : getFields()) {
                    if (modelField.getName() != null && dataField.getName() != null
                            && modelField.getName().equals(dataField.getName())) {
                        modelField.setValue(dataField.getValue());
                        break;
                    }
                }
                newFields.add(modelField);
            }
            setFields(newFields);
        }
        return this;
    }

    CustomForm() {}

    @Id
    private long id;
    private short idField;
    private String name;
    private long version = 0;
    private List<FormField> fields;
    private List<CustomForm> objects;
    private Process process;
    @Deprecated
    private FormField action;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<FormField> getFields() { return fields; }
    public void setFields(List<FormField> fields) { this.fields = fields; }
    public List<CustomForm> getObjects() { return objects; }
    public void setObjects(List<CustomForm> objects) { this.objects = objects; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public short getIdField() { return idField; }
    public void setIdField(short idField) { this.idField = idField; }
    public FormField getAction() { return action; }
    public void setAction(FormField action) { this.action = action; }
    public Process getProcess() { return process; }
    public void setProcess(Process process) { this.process = process; }
}
