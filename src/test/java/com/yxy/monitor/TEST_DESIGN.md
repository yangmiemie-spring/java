# 认证与权限管理模块 — 白盒测试用例设计文档

> 项目：monitor (com.yxy.monitor)  
> 架构：Spring Boot 3.3 + Spring Security + JWT (jjwt 0.11.5) + MyBatis  
> 测试框架：JUnit 5 + MockMvc + Mockito + spring-security-test

---

## 一、登录功能 (Login) — 3 种角色

### 被测方法：`AuthServiceImpl.login(username, password)`
### 分支覆盖：

| 分支 | 条件 | 路径 |
|------|------|------|
| B1 | `user == null` | 用户不存在 |
| B2 | `!passwordEncoder.matches(...)` | 密码不匹配 |
| B3 | user存在 且 密码匹配 | 登录成功 |
| B4 | 非RuntimeException异常 | 系统异常 |

### 测试用例：

| 用例编号 | 测试路径 | 角色/场景 | 输入数据 | Mock 行为 | 预期结果 |
|----------|----------|-----------|----------|-----------|----------|
| **TC-LOGIN-001** | 正向-管理员登录 | admin 角色 | username="admin", password="Admin123" | `selectByUsername` 返回 admin 用户; `passwordEncoder.matches` = true; `getUserRoleKeys` 返回 ["admin"] | 返回 Map 含 token/userId/username/roles; roles 包含 "admin" |
| **TC-LOGIN-002** | 正向-普通用户登录 | user 角色 | username="user001", password="User123" | `selectByUsername` 返回 user 用户; `passwordEncoder.matches` = true; `getUserRoleKeys` 返回 ["user"] | 返回 Map 含 token; roles 包含 "user" |
| **TC-LOGIN-003** | 正向-审计员登录 | auditor 角色(自定义) | username="auditor", password="Audit123" | `selectByUsername` 返回 auditor 用户; `getUserRoleKeys` 返回 ["auditor"] | 返回 Map 含 token; roles 包含 "auditor" |
| **TC-LOGIN-004** | 负向-用户不存在 | B1 分支 | username="ghost", password="xxx" | `selectByUsername` 返回 null | 抛出 RuntimeException("用户名或密码错误！") |
| **TC-LOGIN-005** | 负向-密码错误 | B2 分支 | username="admin", password="WrongPwd" | `selectByUsername` 返回 user; `passwordEncoder.matches` = false | 抛出 RuntimeException("用户名或密码错误！") |
| **TC-LOGIN-006** | 边界-用户名为空 | @Valid 校验 | username="" / null, password="Admin123" | 不经过 Service | HTTP 400 + 校验错误信息 |
| **TC-LOGIN-007** | 边界-密码为空 | @Valid 校验 | username="admin", password="" / null | 不经过 Service | HTTP 400 + 校验错误信息 |
| **TC-LOGIN-008** | 异常-数据库异常 | B4 分支 | username="admin", password="Admin123" | `selectByUsername` 抛出 SQLException | 抛出 RuntimeException("登录失败，系统异常") |
| **TC-LOGIN-009** | 边界-特殊字符用户名 | 防注入 | username="'; DROP TABLE--", password="Admin123" | `selectByUsername` 返回 null | 抛出 RuntimeException("用户名或密码错误！") |
| **TC-LOGIN-010** | 边界-JWT Token 格式 | 返回验证 | 正常登录成功 | 所有 Mock 正常 | 返回的 token 格式为有效 JWT (3段 Base64) |

---

## 二、密码修改功能 (Update Password)

### 被测方法：`AuthServiceImpl.updatePassword(oldPassword, newPassword)`
### 校验链：

```
UpdatePwdDTO (@Valid)
  ├── oldPassword: @NotBlank
  ├── newPassword: @NotBlank
  ├── newPassword: @Size(min=6, max=20)
  └── newPassword: @Pattern("^[A-Za-z0-9_](?=.*[A-Z])(?=.*[a-z]).{5,19}$")
         └── 首字符字母/数字/下划线，必须含大小写字母
```

### 测试用例：

| 用例编号 | 测试路径 | 场景 | 输入数据 | 前置条件 | 预期结果 |
|----------|----------|------|----------|----------|----------|
| **TC-PWD-001** | 正向-修改成功 | 旧密码正确 + 新密码合法 | oldPwd="OldPass1", newPwd="NewPass2" | SecurityContext 有已认证用户; `passwordEncoder.matches(oldPwd, dbPwd)` = true | 返回 "密码修改成功，请重新登录"; `updatePwd` 被调用 |
| **TC-PWD-002** | 负向-旧密码错误 | 旧密码校验失败 | oldPwd="WrongOld1", newPwd="NewPass2" | SecurityContext 有已认证用户; `passwordEncoder.matches` = false | 抛出 RuntimeException("旧密码输入错误") |
| **TC-PWD-003** | 边界-新密码长度=6 | @Size 下边界 | oldPwd="OldPass1", newPwd="Abc123" | 已认证 | 通过校验，密码修改成功 |
| **TC-PWD-004** | 边界-新密码长度=20 | @Size 上边界 | oldPwd="OldPass1", newPwd="Abcdef1234567890ABCD" (20位) | 已认证 | 通过校验，密码修改成功 |
| **TC-PWD-005** | 边界-新密码长度=5 | @Size 低于下限 | oldPwd="OldPass1", newPwd="Abc12" | 不经过 Service | HTTP 400 + "密码长度6-20位" |
| **TC-PWD-006** | 边界-新密码长度=21 | @Size 超过上限 | oldPwd="OldPass1", newPwd="Abcdef1234567890ABCDX" (21位) | 不经过 Service | HTTP 400 + "密码长度6-20位" |
| **TC-PWD-007** | 负向-新密码缺少大写 | @Pattern 校验 | oldPwd="OldPass1", newPwd="abcdef1" | 不经过 Service | HTTP 400 + 正则校验错误 |
| **TC-PWD-008** | 负向-新密码缺少小写 | @Pattern 校验 | oldPwd="OldPass1", newPwd="ABCDEF1" | 不经过 Service | HTTP 400 + 正则校验错误 |
| **TC-PWD-009** | 负向-新密码首字符非法 | @Pattern 首字符 | oldPwd="OldPass1", newPwd="!Abc123" | 不经过 Service | HTTP 400 + 正则校验错误 |
| **TC-PWD-010** | 负向-新密码为空 | @NotBlank | oldPwd="OldPass1", newPwd="" | 不经过 Service | HTTP 400 + "新密码不能为空" |
| **TC-PWD-011** | 负向-旧密码为空 | @NotBlank | oldPwd="", newPwd="NewPass2" | 不经过 Service | HTTP 400 + "旧密码不能为空" |
| **TC-PWD-012** | 负向-未登录修改密码 | 匿名访问 | oldPwd="OldPass1", newPwd="NewPass2" | SecurityContext 为空 | HTTP 403 (Spring Security 拦截) |
| **TC-PWD-013** | 边界-新密码包含下划线 | @Pattern 允许 | oldPwd="OldPass1", newPwd="_Abcdef1" | 已认证 | 通过校验，密码修改成功 |

---

## 三、JWT Token 认证拦截 (Session 拦截)

### 被测类：`JwtAuthFilter.doFilterInternal()`
### 分支覆盖：

| 分支 | 条件 | 行为 |
|------|------|------|
| F1 | URI 为 /api/auth/login\|/api/auth/register\|/api/auth/updatePwd | 直接放行 |
| F2 | Authorization header 为空 或 不以 "Bearer " 开头 | 放行（交给 SecurityConfig 判断） |
| F3 | Token 校验失败 | 放行（交给 SecurityConfig 判断） |
| F4 | Token 有效, 用户存在 | 设置 SecurityContext |

### 测试用例：

| 用例编号 | 测试路径 | 场景 | 请求特征 | 预期结果 |
|----------|----------|------|----------|----------|
| **TC-FILTER-001** | 正向-携带有效 Token 访问受保护接口 | 正常认证流程 | Header: `Authorization: Bearer <valid_token>`; URL: `/api/dashboard/list` | HTTP 200; SecurityContext 包含用户信息和权限 |
| **TC-FILTER-002** | 负向-无 Token 访问受保护接口 | 匿名访问被拒 | 无 Authorization header; URL: `/api/dashboard/list` | HTTP 403 (由 SecurityConfig 拦截) |
| **TC-FILTER-003** | 负向-Token 前缀错误 | "Basic" 前缀 | Header: `Authorization: Basic YWRtaW46MTIz` | HTTP 403 (视为无 Token) |
| **TC-FILTER-004** | 负向-Token 已过期 | 过期 JWT | Header: `Authorization: Bearer <expired_token>` | HTTP 403 (`verifyToken` 返回 false) |
| **TC-FILTER-005** | 负向-Token 被篡改 | 签名无效 | Header: `Authorization: Bearer <tampered_token>` | HTTP 403 (`verifyToken` 返回 false) |
| **TC-FILTER-006** | 负向-Token 格式错误 | 非 JWT 格式 | Header: `Authorization: Bearer not-a-jwt` | HTTP 403 |
| **TC-FILTER-007** | 正向-登录接口跳过 Token 校验 | 放行路径 F1 | 无 Authorization header; URL: `/api/auth/login` | HTTP 200 (或业务错误, 不会被拦截) |
| **TC-FILTER-008** | 正向-注册接口跳过 Token 校验 | 放行路径 F1 | 无 Authorization header; URL: `/api/auth/register` | HTTP 到达 Controller |
| **TC-FILTER-009** | 负向-Token 有效但用户不存在 | 用户被删除 | 有效 Token 包含已删除的用户名 | HTTP 403 或 NullPointerException |
| **TC-FILTER-010** | 边界-Authorization 值为 "Bearer " | 空 Token | Header: `Authorization: Bearer ` | HTTP 403 (`"".length==0` → verifyToken 抛异常 → false) |

---

## 四、越权访问控制 (Privilege Escalation)

### 垂直越权：低权限用户访问高权限接口
### 水平越权：用户访问其他用户的数据

| 用例编号 | 测试路径 | 越权类型 | 场景描述 | 用户/角色 | 请求目标 | 预期结果 |
|----------|----------|----------|----------|-----------|----------|----------|
| **TC-AUTHZ-001** | 正向-管理员访问管理接口 | 正常 | admin 角色访问 `/api/user/list` | admin (ROLE_admin) | GET `/api/user/list` | HTTP 200 |
| **TC-AUTHZ-002** | 负向-普通用户访问管理接口 | **垂直越权** | user 角色尝试访问管理接口 | user (ROLE_user) | GET `/api/user/list` | HTTP 403 (hasRole("admin") 不满足) |
| **TC-AUTHZ-003** | 负向-普通用户访问 @PreAuthorize 接口 | **垂直越权** | user 角色尝试访问 | user (ROLE_user, 无 sys:flow:generate) | GET `/api/auth/admin/test` | HTTP 403 (缺少 required authority) |
| **TC-AUTHZ-004** | 正向-管理员访问 @PreAuthorize 接口 | 正常 | admin 角色有 sys:flow:generate 权限 | admin | GET `/api/auth/admin/test` | HTTP 200 + "只有管理员能访问" |
| **TC-AUTHZ-005** | 负向-匿名用户访问管理接口 | **垂直越权** | 无认证访问管理接口 | 匿名 | GET `/api/user/list` | HTTP 403 |
| **TC-AUTHZ-006** | 负向-普通用户修改其他用户密码 | **水平越权** | userA 尝试以 userB 身份修改密码 | 已认证 userA | POST `/api/auth/updatePwd` (Token=userB) | 由业务逻辑决定; updatePwd 从 SecurityContext 获取当前用户进行修改，无法直接修改他人密码 |
| **TC-AUTHZ-007** | 负向-普通用户访问管理员专属仪表盘 | **垂直越权** | user 角色访问 dashboard | user | GET `/api/dashboard/stats` | HTTP 403 (如果 dashboard 配置了 admin 限制) |
| **TC-AUTHZ-008** | 边界-多角色用户 | 边缘场景 | 用户同时有 user 和 admin 角色 | user+admin | GET `/api/user/list` | HTTP 200 (任一角色匹配) |

---

## 测试策略总结

```
测试层级          工具                       覆盖目标
─────────────────────────────────────────────────────────
Service 层        MockitoExtension          AuthServiceImpl 全部逻辑分支
Controller 层     @WebMvcTest + MockMvc     AuthController 接口 + 参数校验
Filter 层         @WebMvcTest / 手动测试    JwtAuthFilter 分支覆盖
Security 集成     @SpringBootTest           SecurityConfig 权限规则验证
```
