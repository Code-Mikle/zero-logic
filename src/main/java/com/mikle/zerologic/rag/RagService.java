package com.mikle.zerologic.rag;

import com.mikle.zerologic.rag.model.RagResult;

public interface RagService {

    RagResult retrieve(Long taskId, Long appId, Long userId, Long attachmentId, String query);
}
