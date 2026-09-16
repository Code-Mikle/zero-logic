package com.mikle.zerologic.knowledge.document.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.knowledge.document.model.entity.KnowledgeDocument;
import com.mikle.zerologic.knowledge.document.mapper.KnowledgeDocumentMapper;
import com.mikle.zerologic.knowledge.document.service.KnowledgeDocumentService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>  implements KnowledgeDocumentService{

}
