package com.mikle.zerologic.service;

import com.mybatisflex.core.service.IService;
import com.mikle.zerologic.model.entity.ToolCallRecord;
import com.mikle.zerologic.model.vo.ToolCallRecordVO;

import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
public interface ToolCallRecordService extends IService<ToolCallRecord> {

    List<ToolCallRecordVO> listByTaskId(Long taskId);
}
