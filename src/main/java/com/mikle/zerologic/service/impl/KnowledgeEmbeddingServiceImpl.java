package com.mikle.zerologic.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.model.entity.KnowledgeEmbedding;
import com.mikle.zerologic.mapper.KnowledgeEmbeddingMapper;
import com.mikle.zerologic.service.KnowledgeEmbeddingService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Service
public class KnowledgeEmbeddingServiceImpl extends ServiceImpl<KnowledgeEmbeddingMapper, KnowledgeEmbedding>  implements KnowledgeEmbeddingService{

}
