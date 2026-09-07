package com.pos_billingwala.Extra;

/**
 * Readiness of a business type against the live WithTable POS.
 */
public enum BusinessTypeReadiness {
    /** Engines + billing path shipped for this type. */
    LIVE,
    /** Template selectable; specialised extras may still map to a nearest engine. */
    PARTIAL,
    /** Catalogued for later modules; maps to nearest template today. */
    PLANNED
}
