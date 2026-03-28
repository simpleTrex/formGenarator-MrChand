package com.adaptivebp.modules.workflow.dto.response;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<String> errors = new ArrayList<>();

    public static ValidationResult ok() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        return result;
    }

    public static ValidationResult fail(List<String> errors) {
        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setErrors(errors);
        return result;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}
