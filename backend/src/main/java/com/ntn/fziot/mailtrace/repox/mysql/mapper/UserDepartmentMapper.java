package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDepartmentMapper extends BaseMapper<UserDepartmentEntity> {

    @Delete("DELETE FROM mt_user_department WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);
}
