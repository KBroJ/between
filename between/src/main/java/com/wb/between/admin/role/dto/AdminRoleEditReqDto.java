package com.wb.between.admin.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdminRoleEditReqDto {

    @NotBlank(message = "역할 코드는 필수입니다.")
    @Size(max = 50, message = "역할 코드는 50자를 초과할 수 없습니다.")
    private String roleCode;

    @NotBlank(message = "역할 이름은 필수입니다.")
    @Size(max = 100, message = "역할 이름은 100자를 초과할 수 없습니다.")
    private String roleName;

    @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다.")
    private String description;

    @NotEmpty(message = "하나 이상의 권한을 선택해야 합니다.") // 초기 권한 목록
    private List<Long> permissionIds; // UI에서 선택된 권한들의 ID 목록
}
