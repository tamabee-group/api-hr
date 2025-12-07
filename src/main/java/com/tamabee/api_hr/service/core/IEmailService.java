package com.tamabee.api_hr.service.core;

public interface IEmailService {
    void sendTemporaryPassword(String email, String employeeCode, String temporaryPassword, String language);
}
