package com.adaptivebp.modules.process.dto;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private boolean valid;
    private List<String> errors = new ArrayList<>();

    public static ValidationResult ok() {
        ValidationResult r = new ValidationResult();
        r.valid = true;
        return r;
    }

    public static ValidationResult fail(List<String> errors) {
        ValidationResult r = new ValidationResult();
        r.valid = false;
        r.errors = errors;
        return r;
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
}
