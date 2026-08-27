package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDataGrantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface UserDataGrantMapper extends BaseMapper<UserDataGrantEntity> {

    @Delete("DELETE FROM mt_user_data_grant WHERE user_id = #{userId}")
    int physicalDeleteByUserId(Long userId);
}
