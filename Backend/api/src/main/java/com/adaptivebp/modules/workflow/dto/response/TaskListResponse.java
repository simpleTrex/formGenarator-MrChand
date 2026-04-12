package com.adaptivebp.modules.workflow.dto.response;

import java.util.ArrayList;
import java.util.List;

public class TaskListResponse {
    private int count;
    private List<TaskResponse> tasks = new ArrayList<>();

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<TaskResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskResponse> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.count = this.tasks.size();
    }
}
