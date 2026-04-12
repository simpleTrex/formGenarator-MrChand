package com.adaptivebp.modules.process.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Validates form submissions against a FORM_PAGE node's element config.
 * Returns field-level errors: [{elementId, message}].
 */
@Service
public class FormValidationService {

    public record FieldError(String elementId, String message) {}

    @SuppressWarnings("unchecked")
    public List<FieldError> validate(Map<String, Object> nodeConfig, Map<String, Object> formData) {
        List<FieldError> errors = new ArrayList<>();
        if (nodeConfig == null || formData == null) return errors;

        List<Map<String, Object>> elements = (List<Map<String, Object>>) nodeConfig.get("elements");
        if (elements == null) return errors;

        for (Map<String, Object> element : elements) {
            String id = (String) element.get("id");
            String type = (String) element.get("type");
            if (id == null || "LABEL".equals(type) || "HIDDEN".equals(type)) continue;

            // Evaluate visibility rule — if hidden, skip validation
            if (isHidden(element, formData)) continue;

            Object rawValue = formData.get(id);
            Map<String, Object> validation = (Map<String, Object>) element.get("validation");
            Map<String, Object> config = (Map<String, Object>) element.get("config");

            if (validation == null) continue;

            // 1 — required check
            Boolean required = (Boolean) validation.get("required");
            if (Boolean.TRUE.equals(required) && isEmpty(rawValue)) {
                errors.add(new FieldError(id, "This field is required"));
                continue; // Skip further checks if value is empty
            }

            if (isEmpty(rawValue)) continue; // Not required and empty — skip

            String stringValue = rawValue.toString();

            // 2 — minLength / maxLength for STRING types
            if (isStringType(type)) {
                Integer minLength = toInt(validation.get("minLength"));
                Integer maxLength = toInt(validation.get("maxLength"));
                if (minLength != null && stringValue.length() < minLength) {
                    errors.add(new FieldError(id, "Minimum length is " + minLength));
                }
                if (maxLength != null && stringValue.length() > maxLength) {
                    errors.add(new FieldError(id, "Maximum length is " + maxLength));
                }
                // 4 — pattern (regex)
                String pattern = (String) validation.get("pattern");
                if (pattern != null && !pattern.isBlank() && !stringValue.matches(pattern)) {
                    errors.add(new FieldError(id, "Value does not match the required pattern"));
                }
            }

            // 3 — min / max for NUMBER types
            if ("NUMBER_INPUT".equals(type)) {
                try {
                    double numValue = Double.parseDouble(stringValue);
                    Number min = (Number) validation.get("min");
                    Number max = (Number) validation.get("max");
                    if (min != null && numValue < min.doubleValue()) {
                        errors.add(new FieldError(id, "Minimum value is " + min));
                    }
                    if (max != null && numValue > max.doubleValue()) {
                        errors.add(new FieldError(id, "Maximum value is " + max));
                    }
                } catch (NumberFormatException e) {
                    errors.add(new FieldError(id, "Must be a valid number"));
                }
            }

            // 6 — SELECT / RADIO / CHECKBOX must match defined options
            if ("SELECT".equals(type) || "RADIO".equals(type) || "CHECKBOX".equals(type)) {
                if (config != null) {
                    List<Map<String, Object>> options = (List<Map<String, Object>>) config.get("options");
                    if (options != null && !options.isEmpty()) {
                        Set<String> validValues = new java.util.HashSet<>();
                        for (Map<String, Object> opt : options) {
                            Object val = opt.get("value");
                            if (val != null) validValues.add(val.toString());
                        }
                        if (rawValue instanceof Collection<?> multi) {
                            for (Object v : multi) {
                                if (!validValues.contains(v.toString())) {
                                    errors.add(new FieldError(id, "Invalid option: " + v));
                                }
                            }
                        } else if (!validValues.contains(stringValue)) {
                            errors.add(new FieldError(id, "Invalid option: " + stringValue));
                        }
                    }
                }
            }
        }

        return errors;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private boolean isHidden(Map<String, Object> element, Map<String, Object> formData) {
        Map<String, Object> rule = (Map<String, Object>) element.get("visibilityRule");
        if (rule == null) return false;
        String dependsOn = (String) rule.get("dependsOn");
        String operator = (String) rule.get("operator");
        Object ruleValue = rule.get("value");
        if (dependsOn == null || operator == null) return false;

        Object actualValue = formData.get(dependsOn);
        // visibilityRule defines SHOW condition — if show condition fails, element is hidden
        return !evaluateOperator(actualValue, operator, ruleValue);
    }

    private boolean evaluateOperator(Object actual, String operator, Object expected) {
        String actualStr = actual == null ? "" : actual.toString();
        String expectedStr = expected == null ? "" : expected.toString();
        return switch (operator) {
            case "EQUALS" -> actualStr.equals(expectedStr);
            case "NOT_EQUALS" -> !actualStr.equals(expectedStr);
            case "IS_EMPTY" -> actualStr.isBlank();
            case "IS_NOT_EMPTY" -> !actualStr.isBlank();
            case "CONTAINS" -> actualStr.contains(expectedStr);
            case "GREATER_THAN" -> {
                try { yield Double.parseDouble(actualStr) > Double.parseDouble(expectedStr); }
                catch (NumberFormatException e) { yield false; }
            }
            case "LESS_THAN" -> {
                try { yield Double.parseDouble(actualStr) < Double.parseDouble(expectedStr); }
                catch (NumberFormatException e) { yield false; }
            }
            default -> false;
        };
    }

    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String s) return s.isBlank();
        if (value instanceof Collection<?> c) return c.isEmpty();
        return false;
    }

    private boolean isStringType(String type) {
        return "TEXT_INPUT".equals(type) || "TEXT_AREA".equals(type) || "DATE_PICKER".equals(type)
                || "DATETIME_PICKER".equals(type);
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
