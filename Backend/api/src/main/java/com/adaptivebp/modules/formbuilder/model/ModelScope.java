package com.adaptivebp.modules.formbuilder.model;

/**
 * Defines the visibility scope of a domain model.
 */
public enum ModelScope {
    /**
     * Model is scoped to a single domain.
     * Normal models created by users.
     */
    DOMAIN_SCOPED,

    /**
     * Model is visible across all domains within the system.
     * Currently only used for the built-in Employee model.
     */
    SYSTEM_WIDE
}
