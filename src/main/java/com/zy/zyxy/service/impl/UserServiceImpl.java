package com.zy.zyxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.service.UserService;
import com.zy.zyxy.mapper.UserMapper;
import com.zy.zyxy.util.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zy.zyxy.contant.UserConstant.ADMIN_ROLE;
import static com.zy.zyxy.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author zy
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;


    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "zy";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode,String phone,String email,Integer gender,String avatarUrl,String activeIds,String userName) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if(userName.length() == 0 || userName.length() > 50){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名不合法");
        }
        if (planetCode.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户分组过长");
        }
        // todo 校验传入的标签必须在 星球编号内
        if(activeIds.length() > 200){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户标签不合法");
        }
        if(avatarUrl.length() > 200){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户头像地址过长");
        }
        // 校验手机号
        //校验手机号（正则表达式）
        if (!phone.matches("1[3-9]\\d{9}")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"手机号格式有误");
        }
        // 校验邮箱
        if (!email.matches("\\w{1,30}@[a-zA-Z0-9]{2,20}(\\.[a-zA-Z0-9]{2,20}){1,2}")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邮箱不合法");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号不能包含特殊字符");
        }
        if(gender < 0 || gender > 1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"性别不合法");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号必须合法
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无相同圈子的伙伴,请重新填写圈子");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        user.setEmail(email);
        user.setGender(gender);
        user.setTags(activeIds);
        user.setPhone(phone);
        user.setAvatarUrl(avatarUrl);
        user.setUsername(userName);

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return user.getId();
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户中不能包含特殊字符");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());

        return safetyUser;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        User user = getLoginUser(request);
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param
     * @return
     */
    public boolean isAdmin(User user) {
        // 仅管理员可查询
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user;
    }

    @Override
    public Page<User> matchUser(User loginUser, Long num) {
        // 检验是否有标签
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        if (tagList == null || tagList.size() == 0) {
            return matchUserNoTag(loginUser,num);
        }
        // 1.筛选标签不为空的
        // todo 给用户添加一个字段区分 planetCode
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("planetCode",loginUser.getPlanetCode());
        userQueryWrapper.isNotNull("tags");

        List<User> userList = this.list(userQueryWrapper);
        // 查不出东西直接报错
        if(userList == null || userList.size() == 0){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        // 用户列表的下表 => 相似度
        // 2.遍历用户计算相似度 键为用户,值为编辑距离
        List<Pair<User, Long>> list = new ArrayList<>();
        for (User user : userList) {
            String currentUsertags = user.getTags();
            // 用户不能为自己或者无标签  (Long类型请使用equals())
            if (StringUtils.isBlank(currentUsertags) || user.getId().equals(loginUser.getId())) {
                continue;
            }
            List<String> currentTagList = gson.fromJson(currentUsertags, new TypeToken<List<String>>() {
            }.getType());
            long minDistance = AlgorithmUtils.minDistance(tagList, currentTagList);
            list.add(new Pair(user, minDistance));
        }
        // 3.获取前 num 小的编辑距离的用户 分页返回
        // 3.1 按照编辑距离排序
        List<Pair<User, Long>> topNumUserList = list.stream()
                .sorted((o1, o2) -> (int) (o1.getValue() - o2.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 3.2 获取对应的用户id列表
        List<Long> idList = topNumUserList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());

        // 3.3 再次查询补充完整信息并分页
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id",idList);
        Page<User> userPage = this.page(new Page<>(1, num), queryWrapper);
        Map<Long, List<User>> userListMap = userPage.getRecords().stream()
                .map(user -> this.getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for (Long id : idList) {
            finalUserList.add(userListMap.get(id).get(0));
        }
        return userPage.setRecords(finalUserList);
    }

    //todo 无标签随机推荐
    private Page<User> matchUserNoTag(User loginUser,Long num) {
//        throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前用户无标签");
      // 直接查找前 num 条 相同圈子的
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("planetCode",loginUser.getPlanetCode());
        Page<User> userPage = this.page(new Page<>(1, num), userQueryWrapper);
        List<User> safeUserList = userPage.getRecords().stream().map(user -> getSafetyUser(user)).collect(Collectors.toList());
        return userPage.setRecords(safeUserList);
    }


    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户 数据库 (暂且不用)
     *
     * @param tagNameList 标签列表
     * @return 脱敏后的用户列表
     */
    @Deprecated
    private List<User> searchUsersByTagsSQL(List<String> tagNameList) {
        // 1.参数校验 非空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.拼接参数 要求全部都有
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        // 循环拼接
        for (String tagName : tagNameList) {
            userQueryWrapper = userQueryWrapper.like("tags", tagName);
        }
        // 3.查询
        List<User> userList = userMapper.selectList(userQueryWrapper);
        // 4.脱敏
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 根据标签搜索用户(内存)
     *
     * @param tagNameList 标签列表
     * @param loginUser 登录用户
     * @return 脱敏后的用户列表
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList,User loginUser) {
        // 1.参数校验 非空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // MySQL方式：
//        // 2.拼接参数 要求全部都有
//        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
//        // 循环拼接
//        for (String tagName : tagNameList) {
//            userQueryWrapper = userQueryWrapper.like("tags", tagName);
//        }
        // 1.查询当前星球(模块)部分用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        String planetCode = loginUser.getPlanetCode();
        if(StringUtils.isAnyBlank(planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        userQueryWrapper.eq("planetCode", planetCode);
        List<User> userList = userMapper.selectList(userQueryWrapper);
        Gson gson = new Gson();
        // 2.判断内存中的用户列表是否有包含要求的标签
        // 用户量级大 可以使用 parallelStream()
        return userList.stream().filter(user -> {
            // 2.1 获取标签列表
            String tagsStr = user.getTags();
            // 2.2 反序列化成对象
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            // 2.3 判断是否为空(最好每一个列表都需要)
            // java8 Optional特性
            Set<String> tagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            // 2.4 判断是否包含每一个标签
            for (String tagName : tagNameList) {
                if (!tagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());


    }

    @Override
    public boolean updateUser(User user, HttpServletRequest request) {
        // 参数校验
        if (user == null || user.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 权限校验
        // 1.必须登录
        // 2.必须是管理员或者自己才能修改
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (!isAdmin(loginUser) && !loginUser.getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 3.修改的星球编号必须存在  管理员可以无视这条规则修改
        String planetCode = user.getPlanetCode();
        if(!isAdmin(loginUser) && StringUtils.isNotEmpty(planetCode)){
            // 星球编号必须合法
            QueryWrapper queryWrapper = new QueryWrapper<User>();
            queryWrapper.eq("planetCode", planetCode);
            long count = userMapper.selectCount(queryWrapper);
            if (count <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无相同圈子的伙伴,请重新填写圈子");
            }
        }

        //校验手机号（正则表达式）
        if (user.getPhone() != null && !user.getPhone().matches("1[3-9]\\d{9}")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"手机号格式有误");
        }
        // 校验邮箱
        if (user.getEmail() != null && !user.getEmail().matches("\\w{1,30}@[a-zA-Z0-9]{2,20}(\\.[a-zA-Z0-9]{2,20}){1,2}")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邮箱不合法");
        }
        // 头像
        if(user.getAvatarUrl() != null && user.getAvatarUrl().length() > 200){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户头像地址过长");
        }

        // 校验性别
        if(user.getGender() != null && (user.getGender() < 0 || user.getGender() > 1)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"性别不合法");
        }
        // 数据库中是否存在
        Long updatedUserId = user.getId();
        User oldUser = userMapper.selectById(user.getId());
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        // 修改
        return this.updateById(user);

    }

}
