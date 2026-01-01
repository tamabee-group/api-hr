package com.tamabee.api_hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa thông tin người có quyền duyệt (admin/manager)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverResponse {
    private Long id;
    private String name;
    private String role;
}
