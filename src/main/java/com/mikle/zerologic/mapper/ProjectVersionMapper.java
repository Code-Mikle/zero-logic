package com.mikle.zerologic.mapper;

import com.mikle.zerologic.model.entity.ProjectVersion;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface ProjectVersionMapper extends BaseMapper<ProjectVersion> {

    @Delete("""
            delete from project_version
            where appId = #{appId}
              and userId = #{userId}
              and versionNo > #{versionNo}
            """)
    int physicalDeleteAfterVersionNo(@Param("appId") Long appId,
                                     @Param("userId") Long userId,
                                     @Param("versionNo") Integer versionNo);
}
