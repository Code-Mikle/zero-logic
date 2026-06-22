package com.mikle.zerologic.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.model.entity.RagRetrievalLog;
import com.mikle.zerologic.mapper.RagRetrievalLogMapper;
import com.mikle.zerologic.service.RagRetrievalLogService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Service
public class RagRetrievalLogServiceImpl extends ServiceImpl<RagRetrievalLogMapper, RagRetrievalLog>  implements RagRetrievalLogService{

}
