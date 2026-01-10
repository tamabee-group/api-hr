package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.dto.request.attendance.EmployeeSwapRequest;
import com.tamabee.api_hr.dto.response.schedule.EmployeeScheduleDataResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftSwapRequestResponse;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftSwapRequestEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.SwapRequestStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.ShiftMapper;
import com.tamabee.api_hr.repository.attendance.ShiftAssignmentRepository;
import com.tamabee.api_hr.repository.attendance.ShiftSwapRequestRepository;
import com.tamabee.api_hr.repository.attendance.ShiftTemplateRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.core.interfaces.IEmployeeScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation cho employee xem lịch làm việc
 */
@Service
@RequiredArgsConstructor
public class EmployeeScheduleServiceImpl implements IEmployeeScheduleService {

        private final UserRepository userRepository;
        private final ShiftAssignmentRepository shiftAssignmentRepository;
        private final ShiftSwapRequestRepository shiftSwapRequestRepository;
        private final ShiftTemplateRepository shiftTemplateRepository;
        private final ShiftMapper shiftMapper;

        @Override
        @Transactional(readOnly = true)
        public List<ShiftAssignmentResponse> getMySchedule(Long employeeId, LocalDate startDate, LocalDate endDate) {
                UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId).orElse(null);
                List<ShiftAssignmentResponse> result = new ArrayList<>();

                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository
                                        .findByEmployeeIdAndWorkDate(employeeId, date);

                        for (ShiftAssignmentEntity assignment : assignments) {
                                ShiftTemplateEntity template = shiftTemplateRepository
                                                .findByIdAndDeletedFalse(assignment.getShiftTemplateId())
                                                .orElse(null);

                                UserEntity swappedWith = assignment.getSwappedWithEmployeeId() != null
                                                ? userRepository.findByIdAndDeletedFalse(
                                                                assignment.getSwappedWithEmployeeId()).orElse(null)
                                                : null;

                                result.add(shiftMapper.toResponse(assignment, user, template, swappedWith));
                        }
                }

                return result;
        }

        @Override
        @Transactional(readOnly = true)
        public List<ShiftSwapRequestResponse> getSwapHistory(Long employeeId) {
                List<ShiftSwapRequestEntity> swapRequests = shiftSwapRequestRepository.findByEmployeeId(employeeId);
                List<ShiftSwapRequestResponse> result = new ArrayList<>();

                for (ShiftSwapRequestEntity entity : swapRequests) {
                        result.add(buildSwapRequestResponse(entity));
                }

                return result;
        }

        @Override
        @Transactional(readOnly = true)
        public EmployeeScheduleDataResponse getAllScheduleData(Long employeeId, LocalDate startDate,
                        LocalDate endDate) {
                List<ShiftAssignmentResponse> shifts = getMySchedule(employeeId, startDate, endDate);
                List<ShiftSwapRequestResponse> swapRequests = getSwapHistory(employeeId);

                return EmployeeScheduleDataResponse.builder()
                                .shifts(shifts)
                                .swapRequests(swapRequests)
                                .build();
        }

        private ShiftSwapRequestResponse buildSwapRequestResponse(ShiftSwapRequestEntity entity) {
                UserEntity requester = userRepository.findByIdAndDeletedFalse(entity.getRequesterId()).orElse(null);
                UserEntity targetEmployee = userRepository.findByIdAndDeletedFalse(entity.getTargetEmployeeId())
                                .orElse(null);
                UserEntity approver = entity.getApprovedBy() != null
                                ? userRepository.findByIdAndDeletedFalse(entity.getApprovedBy()).orElse(null)
                                : null;

                ShiftAssignmentEntity requesterAssignment = shiftAssignmentRepository
                                .findById(entity.getRequesterAssignmentId()).orElse(null);
                ShiftAssignmentEntity targetAssignment = shiftAssignmentRepository
                                .findById(entity.getTargetAssignmentId()).orElse(null);

                ShiftAssignmentResponse requesterAssignmentResponse = null;
                ShiftAssignmentResponse targetAssignmentResponse = null;

                if (requesterAssignment != null) {
                        ShiftTemplateEntity shiftTemplate = shiftTemplateRepository
                                        .findByIdAndDeletedFalse(requesterAssignment.getShiftTemplateId()).orElse(null);
                        requesterAssignmentResponse = shiftMapper.toResponse(requesterAssignment, requester,
                                        shiftTemplate, null);
                }

                if (targetAssignment != null) {
                        ShiftTemplateEntity shiftTemplate = shiftTemplateRepository
                                        .findByIdAndDeletedFalse(targetAssignment.getShiftTemplateId()).orElse(null);
                        targetAssignmentResponse = shiftMapper.toResponse(targetAssignment, targetEmployee,
                                        shiftTemplate, null);
                }

                return shiftMapper.toResponse(entity, requester, targetEmployee,
                                requesterAssignmentResponse, targetAssignmentResponse, approver);
        }

        @Override
        @Transactional(readOnly = true)
        public List<ShiftAssignmentResponse> getAvailableShiftsForSwap(Long employeeId, Long myShiftId) {
                // Lấy thông tin ca của mình
                ShiftAssignmentEntity myShift = shiftAssignmentRepository
                                .findById(myShiftId)
                                .orElse(null);

                if (myShift == null) {
                        return new ArrayList<>();
                }

                // Lấy tất cả ca của nhân viên khác trong cùng ngày, status SCHEDULED
                // Không cần companyId trong tenant DB
                List<ShiftAssignmentEntity> availableAssignments = shiftAssignmentRepository
                                .findAvailableForSwap(employeeId, myShift.getWorkDate());

                List<ShiftAssignmentResponse> result = new ArrayList<>();
                for (ShiftAssignmentEntity assignment : availableAssignments) {
                        UserEntity employee = userRepository.findByIdAndDeletedFalse(assignment.getEmployeeId())
                                        .orElse(null);
                        ShiftTemplateEntity template = shiftTemplateRepository
                                        .findByIdAndDeletedFalse(assignment.getShiftTemplateId())
                                        .orElse(null);
                        result.add(shiftMapper.toResponse(assignment, employee, template, null));
                }

                return result;
        }

        @Override
        @Transactional
        public ShiftSwapRequestResponse createSwapRequest(Long employeeId, EmployeeSwapRequest request) {
                // Lấy thông tin user
                UserEntity requester = userRepository.findByIdAndDeletedFalse(employeeId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng",
                                                ErrorCode.USER_NOT_FOUND));

                // Lấy ca của người yêu cầu
                ShiftAssignmentEntity requesterAssignment = shiftAssignmentRepository
                                .findById(request.getRequesterShiftId())
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca làm việc của bạn",
                                                ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

                // Kiểm tra ca thuộc về người yêu cầu
                if (!requesterAssignment.getEmployeeId().equals(employeeId)) {
                        throw new BadRequestException("Ca làm việc không thuộc về bạn", ErrorCode.INVALID_REQUEST);
                }

                // Lấy ca của người được yêu cầu đổi
                ShiftAssignmentEntity targetAssignment = shiftAssignmentRepository
                                .findById(request.getTargetShiftId())
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca làm việc muốn đổi",
                                                ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

                // Lấy thông tin người được yêu cầu đổi
                UserEntity targetEmployee = userRepository.findByIdAndDeletedFalse(targetAssignment.getEmployeeId())
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên",
                                                ErrorCode.USER_NOT_FOUND));

                // Tạo yêu cầu đổi ca
                ShiftSwapRequestEntity swapRequest = new ShiftSwapRequestEntity();
                swapRequest.setRequesterId(employeeId);
                swapRequest.setTargetEmployeeId(targetAssignment.getEmployeeId());
                swapRequest.setRequesterAssignmentId(request.getRequesterShiftId());
                swapRequest.setTargetAssignmentId(request.getTargetShiftId());
                swapRequest.setReason(request.getReason());
                swapRequest.setStatus(SwapRequestStatus.PENDING);

                ShiftSwapRequestEntity saved = shiftSwapRequestRepository.save(swapRequest);

                return buildSwapRequestResponse(saved);
        }
}
