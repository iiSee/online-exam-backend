package edu.sandau.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户权限枚举
 */
@Getter
@AllArgsConstructor
public enum RoleTypeEnum {
    /***
     * 普通用户
     */
    NORMAL_USER(0, "普通用户"),
    /***
     * 试题管理员
     */
    TOPIC_MANAGER(1, "试题管理员"),
    /***
     * 系统管理员
     */
    SYSTEM_MANAGER(2,"系统管理员");

    private final Integer value;
    private final String name;
}
