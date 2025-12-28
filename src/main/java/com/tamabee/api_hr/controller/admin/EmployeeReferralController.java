package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.response.ReferredCompanyResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.admin.IEmployeeReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho Employee Tamabee xem và theo dõi company đã giới thiệu
 * Chỉ EMPLOYEE_TAMABEE có quyền truy cập
 */
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_EMPLOYEE_TAMABEE)
public class EmployeeReferralController {

    private final IEmployeeReferralService employeeReferralService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách companies đã giới thiệu (phân trang)
     * GET /api/employee/referrals
     *
     * @param page    số trang (mặc định 0)
     * @param size    số lượng mỗi trang (mặc định 20)
     * @param sortBy  sắp xếp theo field (mặc định createdAt)
     * @param sortDir hướng sắp xếp (asc/desc, mặc định desc)
     */
    @GetMapping("/referrals")
    public ResponseEntity<BaseResponse<Page<ReferredCompanyResponse>>> getReferredCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String employeeCode = getCurrentUserEmployeeCode();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReferredCompanyResponse> companies = employeeReferralService.getReferredCompanies(employeeCode, pageable);
        return ResponseEntity.ok(BaseResponse.success(companies));
    }

    /**
     * Lấy employeeCode của user hiện tại từ JWT token
     */
    private String getCurrentUserEmployeeCode() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));

        return user.getEmployeeCode();
    }
}
