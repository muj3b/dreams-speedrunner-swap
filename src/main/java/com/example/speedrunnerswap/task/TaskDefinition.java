package com.example.speedrunnerswap.task;

import java.util.Arrays;
import java.util.List;

public record TaskDefinition(
        String id,
        String description,
        TaskType type,
        List<String> params,
        TaskDifficulty difficulty,
        List<String> categories,
        boolean enabled
) {
    // Backward-compat: existing call sites
    public TaskDefinition(String id, String description, TaskType type, String... params) {
        this(id, description, type, Arrays.asList(params), TaskDifficulty.MEDIUM, List.of(), true);
    }

    // Backward-compat: no params
    public TaskDefinition(String id, String description, TaskType type) {
        this(id, description, type, List.of(), TaskDifficulty.MEDIUM, List.of(), true);
    }
}
