package com.zy.zyxy.service;

import com.zy.zyxy.model.domain.request.InviteRequest;
import com.zy.zyxy.model.dto.Invitation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【invitation(邀请关系)】的数据库操作Service
* @createDate 2024-04-07 21:16:42
*/
public interface InvitationService extends IService<Invitation> {

    /**
     * 获取我的邀请队伍列表
     * @param request
     * @return
     */
    List<TeamUserVO> getMyInvitation(HttpServletRequest request);

    /**
     * 邀请
     * @param inviteRequest
     * @param loginUser
     * @return
     */
    Boolean inviteRequest(InviteRequest inviteRequest, User loginUser);

    /**
     * 接收邀请 加入队伍
     * @param loginUserId
     * @param teamId
     * @return
     */
    boolean agree(Long loginUserId, Long teamId);
}
