package com.lambda;

import java.time.ZoneId;

/**
 * Common constants used throughout the application.
 */
public final class AppConstants {
    
    // Time zones
    public static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");
    public static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    
    // Custom field names
    public static final String CUSTOM_FIELD_STARTED_AT = "Started at";
    
    // Business logic constants
    public static final float MAX_ACTUAL_HOURS = 999;
    public static final String DATE_FORMAT_MILESTONE = "yyyy-MMM";
    public static final String TIME_SEPARATOR = ";";
    
    // API configuration
    public static final String BACKLOG_SPACE = "faber-wi";
    
    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}