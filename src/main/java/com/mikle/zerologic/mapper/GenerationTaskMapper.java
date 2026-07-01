package com.mikle.zerologic.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mikle.zerologic.model.entity.GenerationTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 *  映射层。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
public interface GenerationTaskMapper extends BaseMapper<GenerationTask> {

    @Update("""
            update generation_task
            set tokenUsage = coalesce(tokenUsage, 0) + #{tokenUsage}
            where id = #{taskId}
            """)
    int addTokenUsage(@Param("taskId") Long taskId, @Param("tokenUsage") Long tokenUsage);
}
