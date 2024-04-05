package com.zy.zyxy.service.impl;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.dto.Tag;
import com.zy.zyxy.service.TagService;
import com.zy.zyxy.mapper.TagMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author Administrator
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2024-04-02 20:45:37
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

    @Override
    public boolean removeTag(Long id) {
        // 1.标签是否存在
        Tag deleteTag = getById(id);
        if(deleteTag == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标签不存在");
        }
        // 2.如果是父标签 必须先删除其他子标签
        if(deleteTag.getIsParent() == 1){
            QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
            tagQueryWrapper.eq("parentId",id);
            this.remove(tagQueryWrapper);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer addTag(Long userId, String tagName, String category, Integer isParent, Integer parentId,Tag tag) {
        // 校验
        if (StringUtils.isAnyBlank(tagName,category)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        tag.setUserId(userId);
        // 校验父标签是否存在

        Tag parentTag = this.getById(parentId);
        if(isParent == 0 && parentTag == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"父标签不存在");
        }else if(isParent == 0 && !parentTag.getCategory().equals(category)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"与父标签分组不一致");
        }

        this.save(tag);
        Integer tagId = tag.getId();
        if(isParent == 1){
            tag.setParentId(tagId);
            this.updateById(tag);
        }
        return tagId;
    }
}




