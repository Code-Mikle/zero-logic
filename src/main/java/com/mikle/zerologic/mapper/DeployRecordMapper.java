package com.mikle.zerologic.mapper;

import com.mikle.zerologic.model.entity.DeployRecord;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployRecordMapper extends BaseMapper<DeployRecord> {

    @Delete("""
            <script>
            delete from deploy_record
            where appId = #{appId}
              and userId = #{userId}
              and versionId in
              <foreach collection="versionIds" item="versionId" open="(" separator="," close=")">
                #{versionId}
              </foreach>
            </script>
            """)
    int physicalDeleteByVersionIds(@Param("appId") Long appId,
                                   @Param("userId") Long userId,
                                   @Param("versionIds") List<Long> versionIds);
}
