package com.mikle.zerologic.generation.task.service;

public interface GenerationTaskProgressService {
    void updateStep(Long taskId, String currentStep);
}
