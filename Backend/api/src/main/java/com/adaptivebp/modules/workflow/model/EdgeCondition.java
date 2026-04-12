package com.adaptivebp.modules.workflow.model;

import com.adaptivebp.modules.workflow.model.enums.ConditionOperator;

public class EdgeCondition {
    private String field;
    private ConditionOperator operator;
    private Object value;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
