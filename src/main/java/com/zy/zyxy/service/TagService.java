package com.zy.zyxy.service;

import com.zy.zyxy.model.dto.Tag;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【tag(标签)】的数据库操作Service
* @createDate 2024-04-02 20:45:37
*/
public interface TagService extends IService<Tag> {

    /**
     * 删除标签
     * @param id
     * @return
     */
    boolean removeTag(Long id);

    /**
     * 添加标签
     * @param userId
     * @param tagName
     * @param category
     * @param isParent
     * @param parentId
     * @return
     */
    Integer addTag(Long userId, String tagName, String category, Integer isParent, Integer parentId,Tag tag);
}
