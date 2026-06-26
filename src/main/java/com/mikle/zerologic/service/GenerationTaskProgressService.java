package com.mikle.zerologic.service;

public interface GenerationTaskProgressService {
    void updateStep(Long taskId, String currentStep);
}
