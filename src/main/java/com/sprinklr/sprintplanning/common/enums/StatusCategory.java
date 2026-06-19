package com.sprinklr.sprintplanning.common.enums;

public enum StatusCategory {
    TODO,
    IN_PROGRESS,
    DONE,
    UNKNOWN;

    public static StatusCategory fromJiraKey(String key) {
        if (key == null) {
            return UNKNOWN;
        }
        return switch (key.toLowerCase()) {
            case "new", "undefined" -> TODO;
            case "indeterminate" -> IN_PROGRESS;
            case "done" -> DONE;
            default -> UNKNOWN;
        };
    }
}
