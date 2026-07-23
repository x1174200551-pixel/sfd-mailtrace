package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailSendLogMapper extends BaseMapper<MailSendLogEntity> {
}
