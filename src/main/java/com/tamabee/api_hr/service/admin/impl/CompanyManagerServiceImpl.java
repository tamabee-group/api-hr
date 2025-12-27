package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.UpdateCompanyRequest;
import com.tamabee.api_hr.dto.response.CompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.core.CompanyMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.service.admin.ICompanyManagerService;
import com.tamabee.api_hr.service.core.IUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service quản lý công ty cho admin Tamabee
 */
@Service
@RequiredArgsConstructor
public class CompanyManagerServiceImpl implements ICompanyManagerService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final IUploadService uploadService;

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAllCompanies(Pageable pageable) {
        Page<CompanyEntity> companies = companyRepository.findAll(pageable);
        return companies.map(companyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));
        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, UpdateCompanyRequest request) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));

        // Cập nhật các trường nếu có giá trị
        if (request.getName() != null) {
            company.setName(request.getName());
        }
        if (request.getOwnerName() != null) {
            company.setOwnerName(request.getOwnerName());
        }
        if (request.getEmail() != null) {
            company.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            company.setPhone(request.getPhone());
        }
        if (request.getIndustry() != null) {
            company.setIndustry(request.getIndustry());
        }
        if (request.getLocale() != null) {
            company.setLocale(request.getLocale());
        }
        if (request.getLanguage() != null) {
            company.setLanguage(request.getLanguage());
        }
        if (request.getZipcode() != null) {
            company.setZipcode(request.getZipcode());
        }
        if (request.getAddress() != null) {
            company.setAddress(request.getAddress());
        }

        CompanyEntity savedCompany = companyRepository.save(company);
        return companyMapper.toResponse(savedCompany);
    }

    @Override
    @Transactional
    public String uploadLogo(Long id, MultipartFile file) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));

        // Xóa logo cũ nếu có
        if (company.getLogo() != null) {
            uploadService.deleteFile(company.getLogo());
        }

        // Upload file mới
        String logoUrl = uploadService.uploadFile(file, "logo", "company-" + id);

        // Cập nhật vào database
        company.setLogo(logoUrl);
        companyRepository.save(company);

        return logoUrl;
    }
}
