package com.tamabee.api_hr.service.core.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý upload và xóa file
 * Cấu trúc: uploads/{tenant-domain}/{folder}/{filename}
 */
@Slf4j
@Service
public class UploadServiceImpl implements IUploadService {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    /**
     * Upload file lên server
     * Cấu trúc: uploads/{tenant-domain}/{folder}/{filename}
     * 
     * @param file      file cần upload
     * @param folder    thư mục (vd: avatar, logo)
     * @param subFolder không sử dụng nữa, giữ lại để tương thích
     * @return đường dẫn tương đối của file đã upload
     */
    @Override
    public String uploadFile(MultipartFile file, String folder, String subFolder) {
        try {
            // Lấy tenant domain từ context
            String tenantDomain = TenantContext.getCurrentTenant();
            if (tenantDomain == null || tenantDomain.isEmpty()) {
                tenantDomain = "default";
            }

            // Tạo thư mục: uploads/{tenant-domain}/{folder}
            String dirPath = uploadPath + "/" + tenantDomain + "/" + folder;
            Path directory = Paths.get(dirPath);
            Files.createDirectories(directory);

            // Lấy extension từ tên file gốc
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Tạo tên file mới với UUID
            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = directory.resolve(fileName);
            Files.write(filePath, file.getBytes());

            // Trả về đường dẫn tương đối: /uploads/{tenant-domain}/{folder}/{filename}
            String relativePath = "/uploads/" + tenantDomain + "/" + folder + "/" + fileName;
            log.info("File uploaded successfully: {}", relativePath);

            return relativePath;
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw InternalServerException.fileUploadFailed(e);
        }
    }

    /**
     * Xóa file trên server
     * 
     * @param filePath đường dẫn tương đối của file
     * @return true nếu xóa thành công, false nếu file không tồn tại hoặc lỗi
     */
    @Override
    public boolean deleteFile(String filePath) {
        try {
            // Chuyển đường dẫn tương đối thành đường dẫn tuyệt đối
            String absolutePath = filePath.replace("/uploads/", uploadPath + "/");
            Path path = Paths.get(absolutePath);

            if (Files.exists(path)) {
                Files.delete(path);
                log.info("File deleted successfully: {}", filePath);
                return true;
            }

            log.warn("File not found: {}", filePath);
            return false;
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            return false;
        }
    }
}
