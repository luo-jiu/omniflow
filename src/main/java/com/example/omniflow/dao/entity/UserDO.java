package com.example.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.omniflow.common.database.BaseDO;
import lombok.Data;

@Data
@TableName("user")
public class UserDO extends BaseDO {
    /**
     * ID - 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名 - 用户登录名称
     */
    private String username;

    /**
     * 密码 - 用户登录密码
     */
    private String password;

    /**
     * 手机号 - 用户的手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 账号状态：1=启用，2=禁用，3=待审核
     */
    private Integer status;
}
