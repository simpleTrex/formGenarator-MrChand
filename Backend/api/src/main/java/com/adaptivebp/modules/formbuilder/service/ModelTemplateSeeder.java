package com.adaptivebp.modules.formbuilder.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.adaptivebp.modules.formbuilder.model.DomainFieldType;
import com.adaptivebp.modules.formbuilder.model.DomainModelField;
import com.adaptivebp.modules.formbuilder.model.ModelTemplate;
import com.adaptivebp.modules.formbuilder.repository.ModelTemplateRepository;

/**
 * Seeds the database with common data model templates on application startup.
 */
@Component
public class ModelTemplateSeeder implements CommandLineRunner {

    @Autowired
    private ModelTemplateRepository templateRepository;

    @Override
    public void run(String... args) throws Exception {
        // Define all templates that should exist
        List<ModelTemplate> requiredTemplates = List.of(
            createEmployeeTemplate(),
            createProjectTemplate(),
            createAssetTemplate(),
            createVendorTemplate(),
            createCustomerTemplate(),
            createInventoryTemplate(),
            // Workflow-specific templates
            createLeaveRequestTemplate(),
            createEquipmentTemplate(),
            createPurchaseOrderTemplate(),
            createEquipmentRequestTemplate(),
            createExpenseReportTemplate()
        );

        // Add any missing templates (instead of only seeding when empty)
        int added = 0;
        for (ModelTemplate template : requiredTemplates) {
            if (!templateRepository.existsById(template.getId())) {
                templateRepository.save(template);
                added++;
            }
        }

        if (added > 0) {
            System.out.println("✓ Seeded " + added + " new model templates (total: " + templateRepository.count() + ")");
        }
    }

    private ModelTemplate createEmployeeTemplate() {
        List<DomainModelField> fields = List.of(
            createField("employeeId", "Employee ID", DomainFieldType.STRING, true),
            createField("firstName", "First Name", DomainFieldType.STRING, true),
            createField("lastName", "Last Name", DomainFieldType.STRING, true),
            createField("email", "Email Address", DomainFieldType.STRING, true),
            createField("department", "Department", DomainFieldType.STRING, false),
            createField("position", "Job Title", DomainFieldType.STRING, false),
            createField("hireDate", "Hire Date", DomainFieldType.DATE, false),
            createField("salary", "Annual Salary", DomainFieldType.NUMBER, false),
            createField("status", "Employment Status", DomainFieldType.STRING, true)
        );

        return new ModelTemplate(
            "employee",
            "Employee Records",
            "Comprehensive employee information and HR data",
            "HR",
            fields
        );
    }

    private ModelTemplate createProjectTemplate() {
        List<DomainModelField> fields = List.of(
            createField("projectCode", "Project Code", DomainFieldType.STRING, true),
            createField("projectName", "Project Name", DomainFieldType.STRING, true),
            createField("description", "Description", DomainFieldType.STRING, false),
            createField("projectManager", "Project Manager", DomainFieldType.EMPLOYEE_REFERENCE, true),
            createField("startDate", "Start Date", DomainFieldType.DATE, true),
            createField("endDate", "Target End Date", DomainFieldType.DATE, false),
            createField("budget", "Budget", DomainFieldType.NUMBER, false),
            createField("status", "Status", DomainFieldType.STRING, true),
            createField("priority", "Priority", DomainFieldType.STRING, false)
        );

        return new ModelTemplate(
            "project",
            "Project Tracking",
            "Project management and tracking information",
            "Operations",
            fields
        );
    }

    private ModelTemplate createAssetTemplate() {
        List<DomainModelField> fields = List.of(
            createField("assetId", "Asset ID", DomainFieldType.STRING, true),
            createField("assetName", "Asset Name", DomainFieldType.STRING, true),
            createField("category", "Category", DomainFieldType.STRING, true),
            createField("serialNumber", "Serial Number", DomainFieldType.STRING, false),
            createField("purchaseDate", "Purchase Date", DomainFieldType.DATE, false),
            createField("purchasePrice", "Purchase Price", DomainFieldType.NUMBER, false),
            createField("assignedTo", "Assigned To", DomainFieldType.EMPLOYEE_REFERENCE, false),
            createField("location", "Location", DomainFieldType.STRING, false),
            createField("condition", "Condition", DomainFieldType.STRING, true),
            createField("warrantyExpiry", "Warranty Expiry", DomainFieldType.DATE, false)
        );

        return new ModelTemplate(
            "asset",
            "Asset Management",
            "Track company assets, equipment, and resources",
            "Operations",
            fields
        );
    }

    private ModelTemplate createVendorTemplate() {
        List<DomainModelField> fields = List.of(
            createField("vendorCode", "Vendor Code", DomainFieldType.STRING, true),
            createField("companyName", "Company Name", DomainFieldType.STRING, true),
            createField("contactPerson", "Contact Person", DomainFieldType.STRING, false),
            createField("email", "Email Address", DomainFieldType.STRING, false),
            createField("phone", "Phone Number", DomainFieldType.STRING, false),
            createField("address", "Address", DomainFieldType.STRING, false),
            createField("taxId", "Tax ID", DomainFieldType.STRING, false),
            createField("paymentTerms", "Payment Terms", DomainFieldType.STRING, false),
            createField("status", "Status", DomainFieldType.STRING, true),
            createField("rating", "Vendor Rating", DomainFieldType.NUMBER, false)
        );

        return new ModelTemplate(
            "vendor",
            "Vendor Directory",
            "Supplier and vendor contact and contract information",
            "Finance",
            fields
        );
    }

    private ModelTemplate createCustomerTemplate() {
        List<DomainModelField> fields = List.of(
            createField("customerId", "Customer ID", DomainFieldType.STRING, true),
            createField("customerName", "Customer Name", DomainFieldType.STRING, true),
            createField("contactPerson", "Contact Person", DomainFieldType.STRING, false),
            createField("email", "Email Address", DomainFieldType.STRING, false),
            createField("phone", "Phone Number", DomainFieldType.STRING, false),
            createField("address", "Billing Address", DomainFieldType.STRING, false),
            createField("accountManager", "Account Manager", DomainFieldType.EMPLOYEE_REFERENCE, false),
            createField("customerType", "Customer Type", DomainFieldType.STRING, false),
            createField("contractValue", "Annual Contract Value", DomainFieldType.NUMBER, false),
            createField("status", "Status", DomainFieldType.STRING, true)
        );

        return new ModelTemplate(
            "customer",
            "Customer Database",
            "Customer relationship and account management data",
            "Sales",
            fields
        );
    }

    private ModelTemplate createInventoryTemplate() {
        List<DomainModelField> fields = List.of(
            createField("itemCode", "Item Code", DomainFieldType.STRING, true),
            createField("itemName", "Item Name", DomainFieldType.STRING, true),
            createField("description", "Description", DomainFieldType.STRING, false),
            createField("category", "Category", DomainFieldType.STRING, true),
            createField("unitPrice", "Unit Price", DomainFieldType.NUMBER, false),
            createField("quantityOnHand", "Quantity On Hand", DomainFieldType.NUMBER, true),
            createField("reorderLevel", "Reorder Level", DomainFieldType.NUMBER, false),
            createField("supplier", "Primary Supplier", DomainFieldType.STRING, false),
            createField("location", "Storage Location", DomainFieldType.STRING, false),
            createField("lastUpdated", "Last Stock Update", DomainFieldType.DATETIME, false)
        );

        return new ModelTemplate(
            "inventory",
            "Inventory Management",
            "Stock levels, product catalog, and warehouse data",
            "Operations",
            fields
        );
    }

    // Workflow-specific model templates

    private ModelTemplate createLeaveRequestTemplate() {
        List<DomainModelField> fields = List.of(
            createField("requestId", "Request ID", DomainFieldType.STRING, true),
            createField("employeeId", "Employee", DomainFieldType.EMPLOYEE_REFERENCE, true),
            createField("leaveType", "Leave Type", DomainFieldType.STRING, true),
            createField("startDate", "Start Date", DomainFieldType.DATE, true),
            createField("endDate", "End Date", DomainFieldType.DATE, true),
            createField("totalDays", "Total Days", DomainFieldType.NUMBER, true),
            createField("reason", "Reason", DomainFieldType.STRING, true),
            createField("status", "Status", DomainFieldType.STRING, true),
            createField("approvedBy", "Approved By", DomainFieldType.EMPLOYEE_REFERENCE, false),
            createField("submittedDate", "Submitted Date", DomainFieldType.DATETIME, true)
        );

        return new ModelTemplate(
            "leave-request",
            "Leave Request",
            "Employee leave application and approval tracking",
            "HR",
            fields
        );
    }

    private ModelTemplate createEquipmentTemplate() {
        List<DomainModelField> fields = List.of(
            createField("equipmentId", "Equipment ID", DomainFieldType.STRING, true),
            createField("equipmentName", "Equipment Name", DomainFieldType.STRING, true),
            createField("equipmentType", "Equipment Type", DomainFieldType.STRING, true),
            createField("serialNumber", "Serial Number", DomainFieldType.STRING, false),
            createField("manufacturer", "Manufacturer", DomainFieldType.STRING, false),
            createField("model", "Model", DomainFieldType.STRING, false),
            createField("purchaseDate", "Purchase Date", DomainFieldType.DATE, false),
            createField("warranty", "Warranty Info", DomainFieldType.STRING, false),
            createField("assignedTo", "Assigned To", DomainFieldType.EMPLOYEE_REFERENCE, false),
            createField("status", "Status", DomainFieldType.STRING, true)
        );

        return new ModelTemplate(
            "equipment",
            "Equipment",
            "Office equipment and hardware inventory",
            "IT",
            fields
        );
    }

    private ModelTemplate createPurchaseOrderTemplate() {
        List<DomainModelField> fields = List.of(
            createField("poNumber", "PO Number", DomainFieldType.STRING, true),
            createField("vendorId", "Vendor", DomainFieldType.REFERENCE, true),
            createField("requestedBy", "Requested By", DomainFieldType.EMPLOYEE_REFERENCE, true),
            createField("itemDescription", "Item Description", DomainFieldType.STRING, true),
            createField("quantity", "Quantity", DomainFieldType.NUMBER, true),
            createField("unitPrice", "Unit Price", DomainFieldType.NUMBER, true),
            createField("totalAmount", "Total Amount", DomainFieldType.NUMBER, true),
            createField("requestDate", "Request Date", DomainFieldType.DATE, true),
            createField("approvedBy", "Approved By", DomainFieldType.EMPLOYEE_REFERENCE, false),
            createField("status", "Status", DomainFieldType.STRING, true)
        );

        return new ModelTemplate(
            "purchase-order",
            "Purchase Order",
            "Purchase requests and procurement tracking",
            "Finance",
            fields
        );
    }

    private ModelTemplate createEquipmentRequestTemplate() {
        List<DomainModelField> fields = List.of(
            createField("requestId", "Request ID", DomainFieldType.STRING, true),
            createField("requestedBy", "Requested By", DomainFieldType.EMPLOYEE_REFERENCE, true),
            createField("equipmentType", "Equipment Type", DomainFieldType.STRING, true),
            createField("justification", "Business Justification", DomainFieldType.STRING, true),
            createField("priority", "Priority", DomainFieldType.STRING, true),
            createField("estimatedCost", "Estimated Cost", DomainFieldType.NUMBER, false),
            createField("requestDate", "Request Date", DomainFieldType.DATE, true),
            createField("approvedBy", "Approved By", DomainFieldType.EMPLOYEE_REFERENCE, false),
            createField("status", "Status", DomainFieldType.STRING, true),
            createField("notes", "Notes", DomainFieldType.STRING, false)
        );

        return new ModelTemplate(
            "equipment-request",
            "Equipment Request",
            "Employee equipment requests and approval workflow",
            "IT",
            fields
        );
    }

    private ModelTemplate createExpenseReportTemplate() {
        List<DomainModelField> fields = List.of(
            createField("expenseId", "Expense ID", DomainFieldType.STRING, true),
            createField("submittedBy", "Submitted By", DomainFieldType.EMPLOYEE_REFERENCE, true),
            createField("expenseDate", "Expense Date", DomainFieldType.DATE, true),
            createField("category", "Category", DomainFieldType.STRING, true),
            createField("description", "Description", DomainFieldType.STRING, true),
            createField("amount", "Amount", DomainFieldType.NUMBER, true),
            createField("currency", "Currency", DomainFieldType.STRING, true),
            createField("receiptAttached", "Receipt Attached", DomainFieldType.BOOLEAN, false),
            createField("approvedBy", "Approved By", DomainFieldType.EMPLOYEE_REFERENCE, false),
            createField("status", "Status", DomainFieldType.STRING, true)
        );

        return new ModelTemplate(
            "expense-report",
            "Expense Report",
            "Employee expense claims and reimbursement tracking",
            "Finance",
            fields
        );
    }

    private DomainModelField createField(String name, String displayName, DomainFieldType type, boolean required) {
        DomainModelField field = new DomainModelField();
        field.setKey(name);
        // Since DomainModelField doesn't have displayName, we can store it in config
        field.getConfig().put("displayName", displayName);
        field.setType(type);
        field.setRequired(required);
        return field;
    }
}