package com.pos_billingwala.Extra;

/**
 * One row in the Universal POS business-type catalog.
 */
public final class BusinessTypeInfo {

    private final String typeId;
    private final String displayName;
    private final String category;
    private final BusinessTypeReadiness readiness;
    private final int priority;
    private final String defaultTemplateId;
    private final String engineNotes;

    public BusinessTypeInfo(String typeId, String displayName, String category,
                            BusinessTypeReadiness readiness, int priority,
                            String defaultTemplateId, String engineNotes) {
        this.typeId = typeId;
        this.displayName = displayName;
        this.category = category;
        this.readiness = readiness;
        this.priority = priority;
        this.defaultTemplateId = defaultTemplateId;
        this.engineNotes = engineNotes;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public BusinessTypeReadiness getReadiness() {
        return readiness;
    }

    public int getPriority() {
        return priority;
    }

    public String getDefaultTemplateId() {
        return defaultTemplateId;
    }

    public String getEngineNotes() {
        return engineNotes;
    }

    public String readinessLabel() {
        switch (readiness) {
            case LIVE:
                return "Live";
            case PARTIAL:
                return "Partial";
            case PLANNED:
            default:
                return "Planned";
        }
    }

    public String pickerLabel() {
        return displayName + "  [" + readinessLabel() + "]";
    }
}
