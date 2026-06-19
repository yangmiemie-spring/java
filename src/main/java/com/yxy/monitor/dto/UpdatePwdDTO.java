package com.yxy.monitor.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePwdDTO {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    @Pattern(regexp = "^[A-Za-z0-9_](?=.*[A-Z])(?=.*[a-z]).{5,19}$",
            message = "密码首字符为字母/数字/下划线，必须包含大小写字母")
    private String newPassword;
}