package com.yxy.monitor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须6-20位")
    @Pattern(
            regexp = "^[A-Za-z0-9_](?=.*[A-Z])(?=.*[a-z]).{5,19}$",
            message = "密码规则：首字符只能是字母/数字/下划线，必须同时包含大小写字母"
    )
    private String password;

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;
}