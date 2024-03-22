package com.zy.zyxy.model.enums;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-22 21:56
 * 用户权限枚举类
 */
public enum UserAuthEnum {
    USER(0,"user","普通用户"),
    ADMIN(1,"admin","管理员"),
    VIP(2,"vip","VIP用户");

    private int value;

    private String text;

    private String description;

    public static UserAuthEnum getValues(Integer value){
        if(value == null){// 非空校验
            return null;
        }
        UserAuthEnum[] values = UserAuthEnum.values();
        for (UserAuthEnum userAuthEnum : values) {
            if(userAuthEnum.getValue() == value){
                return userAuthEnum;
            }
        }
        // 不存在
        return null;
    }
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    UserAuthEnum(int value, String text, String description) {
        this.value = value;
        this.text = text;
        this.description = description;
    }
}
