package com.camigo.service;

import com.camigo.dto.UserLoginDTO;
import com.camigo.dto.UserRegisterDTO;
import com.camigo.dto.UserUpdateDTO;
import com.camigo.vo.LoginVO;
import com.camigo.vo.UserInfoVO;

/**
 * 用户注册、登录与资料查询服务。
 */
public interface UserService {

    UserInfoVO register(UserRegisterDTO request);

    LoginVO login(UserLoginDTO request);

    UserInfoVO getUserInfo(Long userId);

    UserInfoVO updateUser(Long currentUserId, Long targetUserId, UserUpdateDTO request);
}
