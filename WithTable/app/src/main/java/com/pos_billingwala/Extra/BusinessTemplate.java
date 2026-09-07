package com.pos_billingwala.Extra;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Declarative capability map for one business type.
 * Does not change billing by itself — {@link FeatureEngine} resolves flags at runtime.
 */
public final class BusinessTemplate {

    private final String id;
    private final String businessType;
    private final String displayName;
    private final Set<String> enabledFeatures;

    public BusinessTemplate(String id, String businessType, String displayName, Set<String> enabledFeatures) {
        this.id = id;
        this.businessType = businessType;
        this.displayName = displayName;
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        if (enabledFeatures != null) {
            copy.addAll(enabledFeatures);
        }
        this.enabledFeatures = Collections.unmodifiableSet(copy);
    }

    public String getId() {
        return id;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getEnabledFeatures() {
        return enabledFeatures;
    }

    public boolean supports(String featureFlag) {
        return featureFlag != null && enabledFeatures.contains(featureFlag);
    }
}
