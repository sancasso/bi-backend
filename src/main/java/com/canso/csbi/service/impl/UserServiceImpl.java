package com.canso.csbi.service.impl;

import static com.canso.csbi.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.canso.csbi.common.*;
import com.canso.csbi.constant.CommonConstant;
import com.canso.csbi.constant.CookieConstant;
import com.canso.csbi.exception.BusinessException;
import com.canso.csbi.manager.SmsLimiter;
import com.canso.csbi.mapper.UserMapper;
import com.canso.csbi.model.dto.sms.SmsDTO;
import com.canso.csbi.model.dto.sms.UserLoginBySmsRequest;
import com.canso.csbi.model.dto.user.UserQueryRequest;
import com.canso.csbi.model.entity.User;
import com.canso.csbi.model.enums.UserRoleEnum;
import com.canso.csbi.model.vo.LoginUserVO;
import com.canso.csbi.model.vo.UserVO;
import com.canso.csbi.service.UserService;
import com.canso.csbi.utils.CookieUtils;
import com.canso.csbi.utils.SqlUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.canso.csbi.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
//import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private RabbitUtils rabbitUtils;

    @Autowired
    private SmsLimiter smsLimiter;

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenUtils tokenUtils;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "canso";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

//    @Override
//    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
//        String unionId = wxOAuth2UserInfo.getUnionId();
//        String mpOpenId = wxOAuth2UserInfo.getOpenid();
//        // 单机锁
//        synchronized (unionId.intern()) {
//            // 查询用户是否已存在
//            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//            queryWrapper.eq("unionId", unionId);
//            User user = this.getOne(queryWrapper);
//            // 用户不存在则创建
//            if (user == null) {
//                user = new User();
//                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
//                user.setUserName(wxOAuth2UserInfo.getNickname());
//                boolean result = this.save(user);
//                if (!result) {
//                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
//                }
//            }
//            // 记录用户的登录态
//            request.getSession().setAttribute(USER_LOGIN_STATE, user);
//            return getLoginUserVO(user);
//        }
//    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
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
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser)  {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUserName(originUser.getUserName());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setUserAvatar(originUser.getUserAvatar());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUpdateTime(originUser.getUpdateTime());
        safetyUser.setCreateTime(originUser.getCreateTime());
        return safetyUser;
    }

    @Override
    public Boolean sign(HttpServletRequest request) {
        //获取当前用户
        Long userid = getLoginUser(request).getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String keySuffiX = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = "sign:" + userid + keySuffiX;
        //获取今天是本月第几天
        int day = now.getDayOfMonth();
        //存入redis
        stringRedisTemplate.opsForValue().setBit(key, day - 1, true);
        return true;
    }

    @Override
    public int signcount(HttpServletRequest request) {
        //获取当前用户
        Long userid = getLoginUser(request).getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String keySuffiX = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = "sign:" + userid + keySuffiX;
        //获取今天是本月第几天
        int day = now.getDayOfMonth();
        //获取本月前 day 天
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().
                        get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        //循环遍历，与运算，遇0则停
        if (result == null || result.isEmpty()) {
            return 0;
        }
        Long num = result.get(0);
        if (num == 0) {
            return 0;
        }
        int count = 0;
        while(true) {
            if ((num & 1) == 0) {
                break;
            } else {
                count++;
            }
            num >>>= 1;
        }
        return count;
    }

    @Override
    public Boolean signstate(HttpServletRequest request) {
        //获取当前用户
        Long userid = getLoginUser(request).getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String keySuffiX = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = "sign:" + userid + keySuffiX;
        //获取今天是本月第几天
        int day = now.getDayOfMonth();
        //获取当天的情况
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().
                        get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(day - 1));
        if (result == null || result.isEmpty()) {
            return false;
        }
        Long num = result.get(0);
        if (num == 0) {
            return false;
        }
        return true;
    }

    @Override
    public BaseResponse sendSmsCaptcha(String phoneNum) {
        if (phoneNum == null ){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        AuthPhoneNumber authPhoneNumber = new AuthPhoneNumber();
        //验证手机号的合法性
        if(!authPhoneNumber.isPhoneNum(phoneNum.toString())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手机号非法");
        }
        int code = (int)((Math.random() * 9 + 1) * 10000);
        // 使用redis来存储手机号和验证码 ，同时使用令牌桶算法来实现流量控制
        boolean sendSmsAuth = smsLimiter.sendSmsAuth(phoneNum, String.valueOf(code));
        if(!sendSmsAuth){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送频率过高，请稍后再试");
        }
        SmsDTO smsDTO = new SmsDTO(phoneNum, String.valueOf(code));
        try {
            //实际发送短信的功能交给第三方服务去实现
            rabbitUtils.sendSms(smsDTO);
        }catch (Exception e){
            //发送失败，删除令牌桶
            redisTemplate.delete("sms:" + phoneNum + "_last_refill_time");
            redisTemplate.delete("sms:" + phoneNum + "_tokens");
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"发送验证码失败，请稍后再试");
        }
        log.info("发送验证码成功---->手机号为{}，验证码为{}", phoneNum, code);
        return ResultUtils.success("发送成功");
    }

    @Override
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        // 随机生成 4 位验证码
        RandomGenerator randomGenerator = new RandomGenerator("0123456789", 4);
        // 定义图片的显示大小
        LineCaptcha lineCaptcha = null;
        lineCaptcha = CaptchaUtil.createLineCaptcha(100, 30);
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "No-cache");
        // 在前端发送请求时携带captchaId，用于标识不同的用户。
        String signature = request.getHeader("signature");
        if (null == signature){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        try {
            // 调用父类的 setGenerator() 方法，设置验证码的类型
            lineCaptcha.setGenerator(randomGenerator);
            // 输出到页面
            lineCaptcha.write(response.getOutputStream());
            // 打印日志
            log.info("captchaId：{} ----生成的验证码:{}", signature,lineCaptcha.getCode());
            // 关闭流
            response.getOutputStream().close();
            //将对应的验证码存入redis中去，2分钟后过期
            redisTemplate.opsForValue().set("api:captchaId:"+signature,lineCaptcha.getCode(),4, TimeUnit.MINUTES);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseResponse loginBySms(UserLoginBySmsRequest loginBySms, HttpServletResponse response) {
        String phoneNum = loginBySms.getPhoneNum();
        String code = loginBySms.getCode();
        if ( null == phoneNum || null == code){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //验证手机号的合法性
        AuthPhoneNumber authPhoneNumber = new AuthPhoneNumber();
        if(!authPhoneNumber.isPhoneNum(phoneNum.toString())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手机号非法");
        }
        //验证用户输入的手机号和验证码是否匹配
        boolean verify = smsLimiter.verifyCode(phoneNum, code);
        if (!verify){
            throw new BusinessException(ErrorCode.SMS_CODE_ERROR);
        }
        //验证该手机号是否完成注册
        UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNum);
        User user=  (User)userDetails;
        user.setUserPassword(null);
        LoginUserVO loginUserVo = initUserLogin((UserDetails) user,response);
        return ResultUtils.success(loginUserVo);
    }
    /**
     * 初始化用户登录状态
     * @param user
     */
    private LoginUserVO initUserLogin(UserDetails user,HttpServletResponse response){
        //设置到Security 全局对象中去
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        LoginUserVO loginUserVo = new LoginUserVO();
        BeanUtils.copyProperties(user,loginUserVo);
        //生成token并存入redis
        String token = tokenUtils.generateToken(String.valueOf(loginUserVo.getId()),loginUserVo.getUserAccount());
        loginUserVo.setToken(token);
        Cookie cookie = new Cookie(CookieConstant.headAuthorization,token);
        cookie.setPath("/");
        cookie.setMaxAge(CookieConstant.expireTime);
        response.addCookie(cookie);
        CookieUtils cookieUtils = new CookieUtils();
        String autoLoginContent = cookieUtils.generateAutoLoginContent(loginUserVo.getId().toString(), loginUserVo.getUserAccount());
        Cookie cookie1 = new Cookie(CookieConstant.autoLoginAuthCheck, autoLoginContent);
        cookie1.setPath("/");
        cookie.setMaxAge(CookieConstant.expireTime);
        response.addCookie(cookie1);
        return loginUserVo;
    }
}
