package com.camigo.service;

import com.camigo.common.PageResult;
import com.camigo.dto.UserLoginDTO;
import com.camigo.dto.UserRegisterDTO;
import com.camigo.dto.UserUpdateDTO;
import com.camigo.vo.LoginVO;
import com.camigo.vo.AdminUserVO;
import com.camigo.vo.UserInfoVO;

/**
 * 用户注册、登录与资料查询服务。
 */
public interface UserService {

    UserInfoVO register(UserRegisterDTO request);

    LoginVO login(UserLoginDTO request);

    UserInfoVO getUserInfo(Long userId);

    UserInfoVO updateUser(Long currentUserId, Long targetUserId, UserUpdateDTO request);

    PageResult<AdminUserVO> listUsers(Long currentUserId, long page, long pageSize,
                                      String keyword, String status, String role);

    void deleteUser(Long currentUserId, Long targetUserId);
}
