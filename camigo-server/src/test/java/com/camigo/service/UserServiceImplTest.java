package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.dto.UserLoginDTO;
import com.camigo.dto.UserRegisterDTO;
import com.camigo.dto.UserUpdateDTO;
import com.camigo.entity.Role;
import com.camigo.entity.User;
import com.camigo.entity.UserRole;
import com.camigo.exception.ForbiddenException;
import com.camigo.mapper.RoleMapper;
import com.camigo.mapper.UserMapper;
import com.camigo.mapper.UserRoleMapper;
import com.camigo.service.impl.UserServiceImpl;
import com.camigo.utils.JwtUtil;
import com.camigo.utils.PasswordUtil;
import com.camigo.vo.LoginVO;
import com.camigo.vo.UserInfoVO;
import com.camigo.vo.AdminUserVO;
import java.util.List;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private PasswordUtil passwordUtil;
    @Mock
    private JwtUtil jwtUtil;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "user-test");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Role.class);
        service = new UserServiceImpl(userMapper, roleMapper, userRoleMapper, passwordUtil, jwtUtil);
    }

    @Test
    void shouldRegisterUserInOneServiceOperation() {
        Role role = new Role();
        role.setId(2L);
        role.setRoleCode("USER");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(passwordUtil.encode("Camigo@123")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

        UserInfoVO result = service.register(new UserRegisterDTO(
                "camigo_user", "Camigo@123", "13800138000", "USER", "测试用户", "20260001"));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.roles()).containsExactly("USER");
        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void shouldRejectPrivilegedRoleDuringPublicRegistration() {
        UserRegisterDTO request = new UserRegisterDTO(
                "camigo_admin", "Camigo@123", "13800138000", "ADMIN", "测试管理员", null);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("公开注册仅支持普通用户角色");
    }

    @Test
    void shouldReturnJwtAfterSuccessfulLogin() {
        User user = new User();
        user.setId(11L);
        user.setUsername("camigo_user");
        user.setPasswordHash("bcrypt-hash");
        user.setRealName("测试用户");
        user.setPhone("13800138000");
        user.setStatus("ACTIVE");
        when(userMapper.selectByAccount("camigo_user")).thenReturn(user);
        when(passwordUtil.matches("Camigo@123", "bcrypt-hash")).thenReturn(true);
        when(userMapper.selectRoleCodes(11L)).thenReturn(List.of("USER"));
        when(jwtUtil.createToken(11L)).thenReturn("signed.jwt.token");

        LoginVO result = service.login(new UserLoginDTO("camigo_user", "Camigo@123"));

        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(result.user().id()).isEqualTo(11L);
    }

    @Test
    void shouldUpdateOwnProfile() {
        User user = activeUser(11L);
        when(userMapper.selectRoleCodes(11L)).thenReturn(List.of("USER"));
        when(userMapper.selectById(11L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserInfoVO result = service.updateUser(11L, 11L,
                new UserUpdateDTO("新姓名", "13900139000", "20260002", null));

        assertThat(result.realName()).isEqualTo("新姓名");
        assertThat(result.phone()).isEqualTo("13900139000");
        assertThat(result.roles()).containsExactly("USER");
        verify(userMapper).updateById(user);
    }

    @Test
    void shouldPreventUserFromUpdatingAnotherAccount() {
        when(userMapper.selectRoleCodes(11L)).thenReturn(List.of("USER"));

        assertThatThrownBy(() -> service.updateUser(11L, 12L,
                new UserUpdateDTO("其他用户", null, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("不能修改其他用户的信息");
    }

    @Test
    void shouldAllowAdminToDisableAnotherAccount() {
        User user = activeUser(12L);
        when(userMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        when(userMapper.selectById(12L)).thenReturn(user);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(userMapper.selectRoleCodes(12L)).thenReturn(List.of("USER"));

        UserInfoVO result = service.updateUser(1L, 12L,
                new UserUpdateDTO(null, null, null, "DISABLED"));

        assertThat(result.status()).isEqualTo("DISABLED");
    }

    @Test
    void shouldListUsersForAdmin() {
        when(userMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        Page<AdminUserVO> page = new Page<>(1, 10);
        page.setRecords(List.of(new AdminUserVO(12L, "student", "学生", "13800138000",
                "20260001", "ACTIVE", "USER", null, LocalDateTime.now(), LocalDateTime.now())));
        page.setTotal(1);
        when(userMapper.selectAdminPage(any(), any(), any(), any())).thenReturn(page);

        var result = service.listUsers(1L, 1, 10, null, "active", "user");

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().get(0).roles()).containsExactly("USER");
    }

    @Test
    void shouldRejectDeletingOwnAdminAccount() {
        when(userMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        assertThatThrownBy(() -> service.deleteUser(1L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("管理员不能删除自己的账号");
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("camigo_user");
        user.setPasswordHash("bcrypt-hash");
        user.setRealName("测试用户");
        user.setPhone("13800138000");
        user.setStudentNo("20260001");
        user.setStatus("ACTIVE");
        return user;
    }
}
