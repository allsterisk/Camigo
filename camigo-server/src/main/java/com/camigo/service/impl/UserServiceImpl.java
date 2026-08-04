package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.dto.UserLoginDTO;
import com.camigo.dto.UserRegisterDTO;
import com.camigo.dto.UserUpdateDTO;
import com.camigo.entity.Role;
import com.camigo.entity.User;
import com.camigo.entity.UserRole;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.RoleMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.mapper.UserRoleMapper;
import com.camigo.service.UserService;
import com.camigo.utils.JwtUtil;
import com.camigo.utils.PasswordUtil;
import com.camigo.vo.LoginVO;
import com.camigo.vo.UserInfoVO;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper,
                           PasswordUtil passwordUtil,
                           JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordUtil = passwordUtil;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public UserInfoVO register(UserRegisterDTO request) {
        String requestedRole = request.role().trim().toUpperCase();
        if (!USER_ROLE.equals(requestedRole)) {
            throw new ForbiddenException("公开注册仅支持普通用户角色");
        }
        String studentNo = StringUtils.hasText(request.studentNo()) ? request.studentNo().trim() : null;
        ensureRegistrationUnique(request.username().trim(), request.phone().trim(), studentNo);

        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, USER_ROLE)
                .eq(Role::getStatus, "ENABLED"));
        if (userRole == null) {
            throw new BusinessException(ErrorCodes.ROLE_NOT_CONFIGURED,
                    "系统未配置普通用户角色", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordUtil.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setPhone(request.phone().trim());
        user.setStudentNo(studentNo);
        user.setStatus("ACTIVE");

        try {
            userMapper.insert(user);
            UserRole binding = new UserRole();
            binding.setUserId(user.getId());
            binding.setRoleId(userRole.getId());
            binding.setGrantedTime(LocalDateTime.now());
            userRoleMapper.insert(binding);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.USER_CONFLICT, "用户名、手机号或学号已存在");
        }

        logger.info("Registered user id={}", user.getId());
        return toUserInfo(user, List.of(USER_ROLE));
    }

    @Override
    @Transactional
    public LoginVO login(UserLoginDTO request) {
        User user = userMapper.selectByAccount(request.account().trim());
        if (user == null || !passwordUtil.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.USER_INVALID_CREDENTIALS,
                    "账号或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCodes.USER_DISABLED,
                    "账号当前不可用", HttpStatus.FORBIDDEN);
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginTime, LocalDateTime.now()));
        List<String> roles = userMapper.selectRoleCodes(user.getId());
        String token = jwtUtil.createToken(user.getId());
        logger.info("User login succeeded, userId={}", user.getId());
        return new LoginVO("Bearer", token, toUserInfo(user, roles));
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return toUserInfo(user, userMapper.selectRoleCodes(userId));
    }

    @Override
    @Transactional
    public UserInfoVO updateUser(Long currentUserId, Long targetUserId, UserUpdateDTO request) {
        boolean hasRealName = request.realName() != null;
        boolean hasPhone = request.phone() != null;
        boolean hasStudentNo = request.studentNo() != null;
        boolean hasStatus = request.status() != null;
        if (!hasRealName && !hasPhone && !hasStudentNo && !hasStatus) {
            throw new BusinessException(ErrorCodes.USER_PROFILE_INVALID,
                    "至少提供一个需要修改的字段", HttpStatus.BAD_REQUEST);
        }

        List<String> currentRoles = normalizeRoles(userMapper.selectRoleCodes(currentUserId));
        boolean isAdmin = currentRoles.contains(ADMIN_ROLE);
        if (!currentUserId.equals(targetUserId) && !isAdmin) {
            throw new ForbiddenException("不能修改其他用户的信息");
        }
        if (hasStatus && !isAdmin) {
            throw new ForbiddenException("只有管理员可以修改账号状态");
        }

        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        if (hasRealName) {
            String realName = request.realName().trim();
            if (!StringUtils.hasText(realName)) {
                throw new BusinessException(ErrorCodes.USER_PROFILE_INVALID,
                        "真实姓名不能为空", HttpStatus.BAD_REQUEST);
            }
            target.setRealName(realName);
        }
        if (hasPhone) {
            target.setPhone(request.phone().trim());
        }
        if (hasStudentNo) {
            target.setStudentNo(StringUtils.hasText(request.studentNo())
                    ? request.studentNo().trim() : null);
        }
        if (hasStatus) {
            target.setStatus(request.status().trim().toUpperCase());
        }

        ensureProfileUnique(target.getId(), target.getPhone(), target.getStudentNo());
        try {
            userMapper.updateById(target);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.USER_CONFLICT, "手机号或学号已存在");
        }
        logger.info("Updated user profile, operatorUserId={}, targetUserId={}",
                currentUserId, targetUserId);
        return toUserInfo(target, userMapper.selectRoleCodes(targetUserId));
    }

    private void ensureRegistrationUnique(String username, String phone, String studentNo) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .and(wrapper -> {
                    wrapper.eq(User::getUsername, username).or().eq(User::getPhone, phone);
                    if (studentNo != null) {
                        wrapper.or().eq(User::getStudentNo, studentNo);
                    }
                }));
        if (count > 0) {
            throw new BusinessException(ErrorCodes.USER_CONFLICT, "用户名、手机号或学号已存在");
        }
    }

    private void ensureProfileUnique(Long userId, String phone, String studentNo) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ne(User::getId, userId)
                .and(wrapper -> {
                    wrapper.eq(User::getPhone, phone);
                    if (studentNo != null) {
                        wrapper.or().eq(User::getStudentNo, studentNo);
                    }
                }));
        if (count > 0) {
            throw new BusinessException(ErrorCodes.USER_CONFLICT, "手机号或学号已存在");
        }
    }

    private UserInfoVO toUserInfo(User user, List<String> roles) {
        return new UserInfoVO(user.getId(), user.getUsername(), user.getRealName(),
                user.getPhone(), user.getStudentNo(), user.getStatus(), normalizeRoles(roles));
    }

    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .distinct()
                .sorted()
                .toList();
    }
}
