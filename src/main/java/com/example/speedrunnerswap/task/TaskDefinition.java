package com.example.speedrunnerswap.task;

public record TaskDefinition(
        String id,
        String description,
        TaskType type,
        String... params
) {}
