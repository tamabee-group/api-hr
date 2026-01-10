package com.tamabee.api_hr.service.admin.interfaces;

import com.tamabee.api_hr.dto.request.user.CreateTamabeeUserRequest;
import com.tamabee.api_hr.dto.request.user.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IEmployeeManagerService {
    Page<UserResponse> getTamabeeUsers(Pageable pageable);
    UserResponse getTamabeeUser(Long id);
    UserResponse createTamabeeUser(CreateTamabeeUserRequest request);
    UserResponse updateUserProfile(Long id, UpdateUserProfileRequest request);
    String uploadAvatar(Long id, MultipartFile file);
}
