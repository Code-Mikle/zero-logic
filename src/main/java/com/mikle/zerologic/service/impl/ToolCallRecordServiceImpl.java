package com.mikle.zerologic.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.model.entity.ToolCallRecord;
import com.mikle.zerologic.mapper.ToolCallRecordMapper;
import com.mikle.zerologic.model.vo.ToolCallRecordVO;
import com.mikle.zerologic.service.ToolCallRecordService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Service
public class ToolCallRecordServiceImpl extends ServiceImpl<ToolCallRecordMapper, ToolCallRecord>  implements ToolCallRecordService{

    @Override
    public List<ToolCallRecordVO> listByTaskId(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        return list(QueryWrapper.create()
                .eq("taskId", taskId)
                .orderBy("id", true))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private ToolCallRecordVO toVO(ToolCallRecord record) {
        ToolCallRecordVO vo = new ToolCallRecordVO();
        BeanUtil.copyProperties(record, vo);
        return vo;
    }
}
