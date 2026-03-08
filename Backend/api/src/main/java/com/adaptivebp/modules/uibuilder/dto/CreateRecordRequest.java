package com.adaptivebp.modules.uibuilder.dto;

import java.util.Map;

public class CreateRecordRequest {
    private Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
