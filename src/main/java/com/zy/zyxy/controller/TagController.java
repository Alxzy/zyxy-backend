package com.zy.zyxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zy.zyxy.annotation.AuthCheck;
import com.zy.zyxy.common.BaseResponse;
import com.zy.zyxy.common.DeleteRequest;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.common.ResultUtils;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.domain.request.TagQueryRequest;
import com.zy.zyxy.model.domain.request.TeamQueryRequest;
import com.zy.zyxy.model.dto.Tag;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.vo.TagVO;
import com.zy.zyxy.service.TagService;
import com.zy.zyxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zy.zyxy.contant.TagConstant.TAG_CATEGORY_KEY;
import static com.zy.zyxy.contant.TagConstant.TAG_LIST_KEY;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-04-02 20:47
 * 标签接口
 */
@RestController
@RequestMapping("/tag")
@CrossOrigin(origins = {"http://127.0.0.1:5173/","https://zyxy.ai-haitham-gsim.icu","https://zyxy-back.ai-haitham-gsim.icu"},allowCredentials = "true")
@Slf4j
public class TagController {
    @Resource
    private UserService userService;

    @Resource
    private TagService tagService;

    @Resource
    private RedisTemplate redisTemplate;

    @PostMapping("/add")
    @AuthCheck(mustRole = "admin")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Integer> addTag(@RequestBody Tag tag, HttpServletRequest request) {
        // 校验参数
        if (tag == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String tagName = tag.getTagName();
        String category = tag.getCategory();
        Long userId = userService.getLoginUser(request).getId();
        Integer parentId = tag.getParentId();
        Integer isParent = tag.getIsParent();

        Integer tagId = tagService.addTag(userId, tagName, category, isParent, parentId, tag);
        return ResultUtils.success(tagId);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteTagById(@RequestBody DeleteRequest deleteRequest) {
        // 校验参数
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isSuccess = tagService.removeTag(id);
        return ResultUtils.success(isSuccess);
    }

    /**
     * 根据id 获取标签
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(anyRole = {"admin", "user", "vip"})
    public BaseResponse<Tag> getTagById(long id) {
        // todo 权限校验
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Tag tag = tagService.getById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(tag);
    }


    /**
     * 获取所有标签
     *
     * @param tagQueryRequest
     * @return
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<Tag>> listTags(TagQueryRequest tagQueryRequest) {
        if (tagQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Tag tag = new Tag();
        BeanUtils.copyProperties(tagQueryRequest, tag);

        QueryWrapper<Tag> teamQueryWrapper = new QueryWrapper<>(tag);
        List<Tag> tagList = tagService.list(teamQueryWrapper);

        return ResultUtils.success(tagList);
    }

    /**
     * 根据组获取对应的标签
     *
     * @param tagQueryRequest
     * @return
     */
    @GetMapping("/category/list")
    public BaseResponse<List<TagVO>> listTagsByCategory(TagQueryRequest tagQueryRequest, HttpServletRequest request) {
        // todo Redis缓存 和 定时任务更新缓存
        if (tagQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Tag tag = new Tag();
        BeanUtils.copyProperties(tagQueryRequest, tag);

        User loginUser = userService.getLoginUser(request);
        // 未登录
        if (loginUser != null) {
            String planetCode = loginUser.getPlanetCode();
            tag.setCategory(planetCode);
        }
        String category = tag.getCategory();
        // 普通用户只能按照标签分组查找
        if(StringUtils.isBlank(category) && !userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 1.查找缓存
        String tagDataKey = String.format(TAG_LIST_KEY, category);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        List<TagVO> tagVOList = (List<TagVO>) valueOperations.get(tagDataKey);
        if(tagVOList != null){
            return ResultUtils.success(tagVOList);
        }
        // 2.查找数据库

        // 只能查询子标签
        tag.setIsParent(0);
        QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>(tag);
        List<Tag> tagList = tagService.list(tagQueryWrapper);
        tagVOList = tagList.stream().map(tag1 -> {
            TagVO tagVO = new TagVO();
            BeanUtils.copyProperties(tag1, tagVO);
            return tagVO;
        }).collect(Collectors.toList());
        // 3.重建缓存
        // 加载到Redis中
        // 设置过期时间为24h
        try {
            valueOperations.set(tagDataKey,tagVOList,24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("redis set key error : " + tagDataKey,e);
        }
        return ResultUtils.success(tagVOList);
    }

    /**
     * 获取所有组
     *
     * @return
     */
    @GetMapping("/get/category")
    public BaseResponse<List<String>> getTagCategory() {
        // 1.获取标签列表
        // 1.1 缓存中获取
        String tagCategoryKey = TAG_CATEGORY_KEY;
        ValueOperations valueOperations = redisTemplate.opsForValue();
        List<String> resultList = (List<String>) valueOperations.get(tagCategoryKey);
        if(resultList != null){
            return ResultUtils.success(resultList);
        }
        // 1.2 数据库中获取
        List<Tag> tagList = tagService.list();
        if (tagList == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        resultList = tagList.stream()
                .map(tag -> tag.getCategory()).distinct().collect(Collectors.toList());
        // 1.3 重建缓存
        // 加载到Redis中
        // 设置过期时间为24h
        try {
            valueOperations.set(tagCategoryKey,resultList,24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("redis set key error : " + tagCategoryKey,e);
        }
        return ResultUtils.success(resultList);
    }
}
