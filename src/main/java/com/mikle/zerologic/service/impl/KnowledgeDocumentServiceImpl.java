package com.mikle.zerologic.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.model.entity.KnowledgeDocument;
import com.mikle.zerologic.mapper.KnowledgeDocumentMapper;
import com.mikle.zerologic.service.KnowledgeDocumentService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>  implements KnowledgeDocumentService{

}
