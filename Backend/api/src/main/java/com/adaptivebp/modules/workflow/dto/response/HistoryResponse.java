package com.adaptivebp.modules.workflow.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.adaptivebp.modules.workflow.model.InstanceHistory;

public class HistoryResponse {
    private List<InstanceHistory> history = new ArrayList<>();

    public List<InstanceHistory> getHistory() {
        return history;
    }

    public void setHistory(List<InstanceHistory> history) {
        this.history = history != null ? history : new ArrayList<>();
    }
}
