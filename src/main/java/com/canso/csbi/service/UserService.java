package com.canso.csbi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.canso.csbi.common.BaseResponse;
import com.canso.csbi.model.dto.sms.UserLoginBySmsRequest;
import com.canso.csbi.model.dto.user.UserQueryRequest;
import com.canso.csbi.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.canso.csbi.model.vo.LoginUserVO;
import com.canso.csbi.model.vo.UserVO;
//import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
* @author zcs
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2023-06-03 09:47:42
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户登录（微信开放平台）
     *
     * @param wxOAuth2UserInfo 从微信获取的用户信息
     * @param request
     * @return 脱敏后的用户信息
     */
//    LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户签到
     * @return
     */
    Boolean sign(HttpServletRequest request);

    /**
     * 统计签到数
     *
     * @return
     */
    int signcount(HttpServletRequest request);

    /**
     * 获取当前用户签到当日是否已经签到了
     *
     */
    Boolean signstate(HttpServletRequest request);

    /**
     * 发送短信验证码
     * @param phoneNum
     */
    BaseResponse sendSmsCaptcha(String phoneNum);

    /**
     * 生成图形验证码
     * @param request
     * @param response
     */
    void getCaptcha(HttpServletRequest request, HttpServletResponse response);

    /**
     * 用户通过手机号进行登录
     * @param loginBySms
     * @param response
     * @return
     */
    BaseResponse loginBySms(UserLoginBySmsRequest loginBySms, HttpServletResponse response);
}
