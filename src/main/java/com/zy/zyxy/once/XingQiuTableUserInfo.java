package com.zy.zyxy.once;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-12 19:45
 * 星球表格用户信息
 */
@Data
public class XingQiuTableUserInfo {
    /**
     * id
     */
    @ExcelProperty("成员编号")
    private String planetCode;

    /**
     * 用户昵称
     */
    @ExcelProperty("成员昵称")
    private String username;
}
