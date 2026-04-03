package com.adaptivebp.modules.process.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.adaptivebp.modules.process.model.enums.NodeType;
import com.adaptivebp.modules.process.model.ProcessEdge;
import com.adaptivebp.modules.process.model.ProcessNode;
import com.adaptivebp.modules.process.model.ProcessTemplate;
import com.adaptivebp.modules.process.repository.ProcessTemplateRepository;

/**
 * Seeds the database with common workflow templates on application startup.
 */
@Component
public class ProcessTemplateSeeder implements CommandLineRunner {

    @Autowired
    private ProcessTemplateRepository templateRepository;

    @Override
    public void run(String... args) throws Exception {
        // Always upsert all templates so changes (like requiredModels) are applied on restart
        List<ProcessTemplate> templates = List.of(
            createLeaveRequestTemplate(),
            createEmployeeOnboardingTemplate(),
            createPurchaseOrderTemplate(),
            createEquipmentRequestTemplate(),
            createExpenseReimbursementTemplate()
        );
        templateRepository.saveAll(templates);
        System.out.println("✓ Upserted " + templates.size() + " process templates");
    }

    private ProcessTemplate createLeaveRequestTemplate() {
        ProcessTemplate template = new ProcessTemplate();
        template.setId("leave-request");
        template.setName("Leave Request");
        template.setDescription("Employee submits leave, manager + HR approval flow");
        template.setCategory("HR");

        List<ProcessNode> nodes = List.of(
            createNode("start", NodeType.START, "Start"),
            createNode("form_request", NodeType.FORM_PAGE, "Submit Leave Request", createLeaveFormConfig()),
            createNode("approval_manager", NodeType.APPROVAL, "Manager Review"),
            createNode("condition_type", NodeType.CONDITION, "Check Leave Type", createLeaveConditionConfig()),
            createNode("form_medical", NodeType.FORM_PAGE, "Medical Certificate", createMedicalFormConfig()),
            createNode("action_save", NodeType.DATA_ACTION, "Save Leave Record", createSaveLeaveConfig()),
            createNode("view_hr", NodeType.DATA_VIEW, "HR Review All", createHrViewConfig()),
            createNode("approval_hr", NodeType.APPROVAL, "HR Final Approval"),
            createNode("action_update", NodeType.DATA_ACTION, "Mark as Approved", createUpdateStatusConfig()),
            createNode("end", NodeType.END, "End")
        );

        List<ProcessEdge> edges = List.of(
            createEdge("e1", "start", "form_request"),
            createEdge("e2", "form_request", "approval_manager"),
            createEdge("e3", "approval_manager", "form_request", "Rejected", "reject"),
            createEdge("e4", "approval_manager", "condition_type", "Approved", "approve"),
            createEdge("e5", "condition_type", "form_medical", "Sick Leave", null),
            createEdge("e6", "condition_type", "action_save", "Other Types", null),
            createEdge("e7", "form_medical", "action_save"),
            createEdge("e8", "action_save", "view_hr"),
            createEdge("e9", "view_hr", "approval_hr"),
            createEdge("e10", "approval_hr", "form_request", "HR Rejected", "reject"),
            createEdge("e11", "approval_hr", "action_update", "HR Approved", "approve"),
            createEdge("e12", "action_update", "end")
        );

        template.setNodes(nodes);
        template.setEdges(edges);
        template.setRequiredModels(List.of("leave-request")); // Requires LeaveRequest model
        return template;
    }

    private ProcessTemplate createEmployeeOnboardingTemplate() {
        ProcessTemplate template = new ProcessTemplate();
        template.setId("employee-onboarding");
        template.setName("Employee Onboarding");
        template.setDescription("Complete onboarding process for new hires");
        template.setCategory("HR");

        List<ProcessNode> nodes = List.of(
            createNode("start", NodeType.START, "Start"),
            createNode("form_details", NodeType.FORM_PAGE, "Employee Details", createEmployeeDetailsConfig()),
            createNode("action_create_employee", NodeType.DATA_ACTION, "Create Employee Record", createEmployeeActionConfig()),
            createNode("form_equipment", NodeType.FORM_PAGE, "Equipment Request", createEquipmentFormConfig()),
            createNode("approval_it", NodeType.APPROVAL, "IT Approval"),
            createNode("view_summary", NodeType.DATA_VIEW, "Onboarding Summary", createOnboardingSummaryConfig()),
            createNode("end", NodeType.END, "End")
        );

        List<ProcessEdge> edges = List.of(
            createEdge("e1", "start", "form_details"),
            createEdge("e2", "form_details", "action_create_employee"),
            createEdge("e3", "action_create_employee", "form_equipment"),
            createEdge("e4", "form_equipment", "approval_it"),
            createEdge("e5", "approval_it", "form_equipment", "Rejected", "reject"),
            createEdge("e6", "approval_it", "view_summary", "Approved", "approve"),
            createEdge("e7", "view_summary", "end")
        );

        template.setNodes(nodes);
        template.setEdges(edges);
        template.setRequiredModels(List.of("equipment")); // Employee model is auto-created, needs Equipment model
        return template;
    }

    private ProcessTemplate createPurchaseOrderTemplate() {
        ProcessTemplate template = new ProcessTemplate();
        template.setId("purchase-order");
        template.setName("Purchase Order");
        template.setDescription("Procurement request and approval workflow");
        template.setCategory("Finance");

        List<ProcessNode> nodes = List.of(
            createNode("start", NodeType.START, "Start"),
            createNode("form_request", NodeType.FORM_PAGE, "Purchase Request", createPurchaseFormConfig()),
            createNode("condition_amount", NodeType.CONDITION, "Check Amount", createAmountConditionConfig()),
            createNode("approval_manager", NodeType.APPROVAL, "Manager Approval"),
            createNode("approval_finance", NodeType.APPROVAL, "Finance Approval"),
            createNode("action_save_po", NodeType.DATA_ACTION, "Create PO", createPurchaseActionConfig()),
            createNode("end", NodeType.END, "End")
        );

        List<ProcessEdge> edges = List.of(
            createEdge("e1", "start", "form_request"),
            createEdge("e2", "form_request", "condition_amount"),
            createEdge("e3", "condition_amount", "approval_manager", "Under $1000", null),
            createEdge("e4", "condition_amount", "approval_finance", "Over $1000", null),
            createEdge("e5", "approval_manager", "form_request", "Rejected", "reject"),
            createEdge("e6", "approval_manager", "action_save_po", "Approved", "approve"),
            createEdge("e7", "approval_finance", "form_request", "Rejected", "reject"),
            createEdge("e8", "approval_finance", "action_save_po", "Approved", "approve"),
            createEdge("e9", "action_save_po", "end")
        );

        template.setNodes(nodes);
        template.setEdges(edges);
        template.setRequiredModels(List.of("purchase-order", "vendor")); // Requires PurchaseOrder and Vendor models
        return template;
    }

    private ProcessTemplate createEquipmentRequestTemplate() {
        ProcessTemplate template = new ProcessTemplate();
        template.setId("equipment-request");
        template.setName("Equipment Request");
        template.setDescription("Request and approval for office equipment");
        template.setCategory("Operations");

        List<ProcessNode> nodes = List.of(
            createNode("start", NodeType.START, "Start"),
            createNode("form_equipment", NodeType.FORM_PAGE, "Equipment Request", createEquipmentRequestConfig()),
            createNode("approval_manager", NodeType.APPROVAL, "Manager Review"),
            createNode("approval_it", NodeType.APPROVAL, "IT Review"),
            createNode("action_order", NodeType.DATA_ACTION, "Create Order", createEquipmentOrderConfig()),
            createNode("end", NodeType.END, "End")
        );

        List<ProcessEdge> edges = List.of(
            createEdge("e1", "start", "form_equipment"),
            createEdge("e2", "form_equipment", "approval_manager"),
            createEdge("e3", "approval_manager", "form_equipment", "Rejected", "reject"),
            createEdge("e4", "approval_manager", "approval_it", "Approved", "approve"),
            createEdge("e5", "approval_it", "form_equipment", "Rejected", "reject"),
            createEdge("e6", "approval_it", "action_order", "Approved", "approve"),
            createEdge("e7", "action_order", "end")
        );

        template.setNodes(nodes);
        template.setEdges(edges);
        template.setRequiredModels(List.of("equipment-request")); // Employee model exists, needs EquipmentRequest model
        return template;
    }

    private ProcessTemplate createExpenseReimbursementTemplate() {
        ProcessTemplate template = new ProcessTemplate();
        template.setId("expense-reimbursement");
        template.setName("Expense Reimbursement");
        template.setDescription("Employee expense claim and reimbursement process");
        template.setCategory("Finance");

        List<ProcessNode> nodes = List.of(
            createNode("start", NodeType.START, "Start"),
            createNode("form_expense", NodeType.FORM_PAGE, "Submit Expenses", createExpenseFormConfig()),
            createNode("approval_manager", NodeType.APPROVAL, "Manager Review"),
            createNode("approval_finance", NodeType.APPROVAL, "Finance Review"),
            createNode("action_reimburse", NodeType.DATA_ACTION, "Process Reimbursement", createReimbursementConfig()),
            createNode("end", NodeType.END, "End")
        );

        List<ProcessEdge> edges = List.of(
            createEdge("e1", "start", "form_expense"),
            createEdge("e2", "form_expense", "approval_manager"),
            createEdge("e3", "approval_manager", "form_expense", "Rejected", "reject"),
            createEdge("e4", "approval_manager", "approval_finance", "Approved", "approve"),
            createEdge("e5", "approval_finance", "form_expense", "Rejected", "reject"),
            createEdge("e6", "approval_finance", "action_reimburse", "Approved", "approve"),
            createEdge("e7", "action_reimburse", "end")
        );

        template.setNodes(nodes);
        template.setEdges(edges);
        template.setRequiredModels(List.of("expense-report")); // Employee model exists, needs ExpenseReport model
        return template;
    }

    // Helper methods for creating nodes and edges
    private ProcessNode createNode(String id, NodeType type, String name) {
        return createNode(id, type, name, new HashMap<>());
    }

    private ProcessNode createNode(String id, NodeType type, String name, Map<String, Object> config) {
        ProcessNode node = new ProcessNode();
        node.setId(id);
        node.setType(type);
        node.setName(name);
        node.setConfig(config);
        return node;
    }

    private ProcessEdge createEdge(String id, String fromNodeId, String toNodeId) {
        return createEdge(id, fromNodeId, toNodeId, "", "");
    }

    private ProcessEdge createEdge(String id, String fromNodeId, String toNodeId, String label, String conditionRef) {
        ProcessEdge edge = new ProcessEdge();
        edge.setId(id);
        edge.setFromNodeId(fromNodeId);
        edge.setToNodeId(toNodeId);
        edge.setLabel(label);
        edge.setConditionRef(conditionRef);
        return edge;
    }

    // Configuration builders for different node types
    private Map<String, Object> createLeaveFormConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        elements.add(createElement("employeeName", "TEXT_INPUT", "Your Name", true));
        elements.add(createSelectElement("leaveType", "Leave Type", true, List.of(
            Map.of("value", "sick", "label", "Sick Leave"),
            Map.of("value", "annual", "label", "Annual Leave"),
            Map.of("value", "personal", "label", "Personal Leave")
        )));
        elements.add(createElement("startDate", "DATE_PICKER", "Start Date", true));
        elements.add(createElement("endDate", "DATE_PICKER", "End Date", true));
        elements.add(createElement("reason", "TEXT_AREA", "Reason", true));

        config.put("elements", elements);
        config.put("submitLabel", "Submit Request");
        return config;
    }

    private Map<String, Object> createMedicalFormConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        elements.add(createElement("certificateNumber", "TEXT_INPUT", "Medical Certificate Number", true));
        elements.add(createElement("doctorName", "TEXT_INPUT", "Doctor Name", false));

        config.put("elements", elements);
        config.put("submitLabel", "Submit Certificate");
        return config;
    }

    private Map<String, Object> createEmployeeDetailsConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        elements.add(createElement("firstName", "TEXT_INPUT", "First Name", true));
        elements.add(createElement("lastName", "TEXT_INPUT", "Last Name", true));
        elements.add(createElement("email", "TEXT_INPUT", "Email Address", true));
        elements.add(createElement("startDate", "DATE_PICKER", "Start Date", true));
        elements.add(createElement("department", "TEXT_INPUT", "Department", true));
        elements.add(createElement("position", "TEXT_INPUT", "Job Title", true));

        config.put("elements", elements);
        return config;
    }

    private Map<String, Object> createEquipmentFormConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        elements.add(createSelectElement("equipmentType", "Equipment Type", true, List.of(
            Map.of("value", "laptop", "label", "Laptop"),
            Map.of("value", "monitor", "label", "Monitor"),
            Map.of("value", "phone", "label", "Phone"),
            Map.of("value", "other", "label", "Other")
        )));
        elements.add(createElement("justification", "TEXT_AREA", "Business Justification", true));

        config.put("elements", elements);
        return config;
    }

    private Map<String, Object> createPurchaseFormConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        elements.add(createElement("itemDescription", "TEXT_INPUT", "Item Description", true));
        elements.add(createElement("vendor", "TEXT_INPUT", "Vendor", true));
        elements.add(createElement("amount", "NUMBER_INPUT", "Total Amount", true));
        elements.add(createElement("justification", "TEXT_AREA", "Business Justification", true));

        config.put("elements", elements);
        return config;
    }

    private Map<String, Object> createEquipmentRequestConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        elements.add(createElement("equipmentType", "TEXT_INPUT", "Equipment Type", true));
        elements.add(createElement("model", "TEXT_INPUT", "Model/Specifications", false));
        elements.add(createElement("urgency", "SELECT", "Urgency Level", true));
        elements.add(createElement("justification", "TEXT_AREA", "Justification", true));

        config.put("elements", elements);
        return config;
    }

    private Map<String, Object> createExpenseFormConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        elements.add(createElement("expenseDate", "DATE_PICKER", "Expense Date", true));
        elements.add(createElement("amount", "NUMBER_INPUT", "Amount", true));
        elements.add(createElement("category", "TEXT_INPUT", "Category", true));
        elements.add(createElement("description", "TEXT_AREA", "Description", true));

        config.put("elements", elements);
        return config;
    }

    // Helper methods for creating form elements
    private Map<String, Object> createElement(String id, String type, String label, boolean required) {
        Map<String, Object> element = new HashMap<>();
        element.put("id", id);
        element.put("type", type);
        element.put("label", label);
        if (required) {
            element.put("validation", Map.of("required", true));
        }
        return element;
    }

    private Map<String, Object> createSelectElement(String id, String label, boolean required, List<Map<String, String>> options) {
        Map<String, Object> element = createElement(id, "SELECT", label, required);
        element.put("config", Map.of("options", options));
        return element;
    }

    // Configuration for other node types (DATA_ACTION, DATA_VIEW, CONDITION)
    private Map<String, Object> createLeaveConditionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("defaultEdgeId", "e6"); // Default to "Other Types" path
        config.put("rules", new ArrayList<>());
        return config;
    }

    private Map<String, Object> createSaveLeaveConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("operation", "CREATE");
        config.put("fieldMappings", new ArrayList<>());
        // modelId will be set by user after linking models
        return config;
    }

    private Map<String, Object> createHrViewConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("displayFields", new ArrayList<>());
        // modelId will be set by user after linking models
        return config;
    }

   private Map<String, Object> createUpdateStatusConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("operation", "UPDATE");
        config.put("fieldMappings", new ArrayList<>());
        // modelId will be set by user after linking models
        return config;
    }

    private Map<String, Object> createEmployeeActionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("operation", "CREATE");
        config.put("fieldMappings", new ArrayList<>());
        return config;
    }

    private Map<String, Object> createOnboardingSummaryConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("displayFields", new ArrayList<>());
        return config;
    }

    private Map<String, Object> createAmountConditionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("defaultEdgeId", "e3"); // Default to "Under $1000" path
        config.put("rules", new ArrayList<>());
        return config;
    }

    private Map<String, Object> createPurchaseActionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("operation", "CREATE");
        config.put("fieldMappings", new ArrayList<>());
        return config;
    }

    private Map<String, Object> createEquipmentOrderConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("operation", "CREATE");
        config.put("fieldMappings", new ArrayList<>());
        return config;
    }

    private Map<String, Object> createReimbursementConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("operation", "CREATE");
        config.put("fieldMappings", new ArrayList<>());
        return config;
    }
}