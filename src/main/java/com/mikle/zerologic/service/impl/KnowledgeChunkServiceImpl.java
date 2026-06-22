package com.mikle.zerologic.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.model.entity.KnowledgeChunk;
import com.mikle.zerologic.mapper.KnowledgeChunkMapper;
import com.mikle.zerologic.service.KnowledgeChunkService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Service
public class KnowledgeChunkServiceImpl extends ServiceImpl<KnowledgeChunkMapper, KnowledgeChunk>  implements KnowledgeChunkService{

}
