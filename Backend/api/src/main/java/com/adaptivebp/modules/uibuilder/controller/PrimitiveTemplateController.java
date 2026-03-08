package com.adaptivebp.modules.uibuilder.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adaptivebp.modules.uibuilder.model.PrimitiveType;

/**
 * Provides hardcoded Business Primitive template schemas.
 * Each schema describes what config fields the Client needs to render the
 * configuration form.
 */
@RestController
@RequestMapping("/adaptive/primitives")
public class PrimitiveTemplateController {

    @GetMapping
    public ResponseEntity<?> listPrimitives() {
        List<Map<String, Object>> primitives = new ArrayList<>();
        primitives.add(buildMeta(PrimitiveType.ITEM_CARD, "Item Card", "🃏",
                "A visual card displaying a single record with image, title, price, and a badge."));
        primitives.add(buildMeta(PrimitiveType.DATA_TABLE, "Data Table", "📋",
                "A sortable and filterable table showing all records of a model."));
        primitives.add(buildMeta(PrimitiveType.ENTRY_FORM, "Entry Form", "📝",
                "A form for creating or editing a record in a model."));
        primitives.add(buildMeta(PrimitiveType.STAT_CARD, "Stat Card", "📊",
                "Displays an aggregated number (count, sum, average) from a model field."));
        primitives.add(buildMeta(PrimitiveType.NAVBAR, "Navbar", "🧭",
                "An auto-generated navigation bar linking to your app pages."));
        primitives.add(buildMeta(PrimitiveType.SIGN_IN_FORM, "Sign-In Form", "🔐",
                "A login form for domain users of this application."));
        primitives.add(buildMeta(PrimitiveType.GALLERY_GRID, "Gallery Grid", "🖼️",
                "A responsive grid of item cards for all records."));
        primitives.add(buildMeta(PrimitiveType.DETAIL_VIEW, "Detail View", "🔍",
                "A full detail panel for a single selected record."));
        return ResponseEntity.ok(primitives);
    }

    @GetMapping("/{type}")
    public ResponseEntity<?> getPrimitiveSchema(@PathVariable String type) {
        try {
            PrimitiveType primitiveType = PrimitiveType.valueOf(type.toUpperCase());
            Map<String, Object> schema = buildSchema(primitiveType);
            return ResponseEntity.ok(schema);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Unknown primitive type: " + type);
        }
    }

    // ───── private helpers ─────

    private Map<String, Object> buildMeta(PrimitiveType type, String label, String icon, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type.name());
        m.put("label", label);
        m.put("icon", icon);
        m.put("description", description);
        return m;
    }

    private Map<String, Object> buildSchema(PrimitiveType type) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type.name());

        List<Map<String, Object>> fields = new ArrayList<>();

        // Common fields for all model-based primitives
        boolean needsModel = type != PrimitiveType.NAVBAR && type != PrimitiveType.SIGN_IN_FORM;

        fields.add(field("name", "Component Name", "TEXT", true, null));

        if (needsModel) {
            fields.add(field("modelId", "Model", "MODEL_SELECT", true, null));
        }

        switch (type) {
            case ITEM_CARD -> {
                fields.add(field("titleField", "Title Field", "FIELD_SELECT", true,
                        "Model field to use as the card title"));
                fields.add(field("imageField", "Image / URL Field", "FIELD_SELECT", false,
                        "Model field containing an image URL"));
                fields.add(field("subtitleField", "Subtitle Field", "FIELD_SELECT", false,
                        "Secondary label under the title"));
                fields.add(field("priceField", "Price Field", "FIELD_SELECT", false, "Numeric field to show as price"));
                fields.add(field("badgeField", "Badge Field", "FIELD_SELECT", false,
                        "Field to display as a status badge"));
                fields.add(field("showBadge", "Show Badge", "BOOLEAN", false, null));
                fields.add(field("onClickAction", "On Click Action", "ACTION_SELECT", false,
                        "What happens when user clicks the card"));
            }
            case DATA_TABLE -> {
                fields.add(field("columns", "Columns to Show", "FIELD_MULTISELECT", true,
                        "Pick which model fields appear as columns"));
                fields.add(field("allowSort", "Allow Sorting", "BOOLEAN", false, null));
                fields.add(field("allowFilter", "Allow Filtering", "BOOLEAN", false, null));
                fields.add(field("rowClickAction", "Row Click Action", "ACTION_SELECT", false,
                        "What happens when user clicks a row"));
            }
            case ENTRY_FORM -> {
                fields.add(field("formFields", "Fields to Include", "FIELD_MULTISELECT", true,
                        "Pick which model fields appear on the form"));
                fields.add(field("submitLabel", "Submit Button Label", "TEXT", false, "Default: Save"));
                fields.add(field("onSubmitAction", "On Submit Action", "ACTION_SELECT", false,
                        "What happens after form is submitted"));
            }
            case STAT_CARD -> {
                fields.add(field("aggregation", "Aggregation", "ENUM_SELECT", true,
                        Map.of("options", List.of("COUNT", "SUM", "AVERAGE", "MIN", "MAX"))));
                fields.add(
                        field("aggregateField", "Field to Aggregate", "FIELD_SELECT", false, "Leave empty for COUNT"));
                fields.add(field("cardLabel", "Card Label", "TEXT", true, "e.g. Total Products"));
            }
            case NAVBAR -> {
                fields.add(field("showAppName", "Show App Name", "BOOLEAN", false, null));
                fields.add(field("position", "Position", "ENUM_SELECT", false,
                        Map.of("options", List.of("TOP", "LEFT", "BOTTOM"))));
            }
            case SIGN_IN_FORM -> {
                fields.add(field("heading", "Form Heading", "TEXT", false, "e.g. Welcome Back"));
                fields.add(field("buttonLabel", "Button Label", "TEXT", false, "Default: Sign In"));
                fields.add(field("redirectPageId", "Redirect Page After Login", "PAGE_SELECT", false, null));
            }
            case GALLERY_GRID -> {
                fields.add(field("titleField", "Title Field", "FIELD_SELECT", true, null));
                fields.add(field("imageField", "Image Field", "FIELD_SELECT", false, null));
                fields.add(field("columns", "Grid Columns", "ENUM_SELECT", false,
                        Map.of("options", List.of("2", "3", "4"))));
                fields.add(field("onClickAction", "On Click Action", "ACTION_SELECT", false, null));
            }
            case DETAIL_VIEW -> {
                fields.add(field("displayFields", "Fields to Show", "FIELD_MULTISELECT", true,
                        "Pick which fields to display"));
                fields.add(field("titleField", "Title Field", "FIELD_SELECT", true, null));
                fields.add(field("showEditButton", "Show Edit Button", "BOOLEAN", false, null));
            }
            default -> {
                /* no extra fields */ }
        }

        schema.put("fields", fields);
        return schema;
    }

    private Map<String, Object> field(String key, String label, String fieldType, boolean required, Object hint) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("key", key);
        f.put("label", label);
        f.put("fieldType", fieldType);
        f.put("required", required);
        if (hint != null)
            f.put("hint", hint);
        return f;
    }
}
