package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.service.core.IUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service xử lý upload và xóa file
 */
@Slf4j
@Service
public class UploadServiceImpl implements IUploadService {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    /**
     * Upload file lên server
     * 
     * @param file      file cần upload
     * @param folder    thư mục chính (vd: avatars, documents)
     * @param subFolder thư mục con (vd: user-id, company-id)
     * @return đường dẫn tương đối của file đã upload
     */
    @Override
    public String uploadFile(MultipartFile file, String folder, String subFolder) {
        try {
            // Tạo thư mục: uploads/{folder}/{subFolder}
            String dirPath = uploadPath + "/" + folder + "/" + subFolder;
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

            // Trả về đường dẫn tương đối
            String relativePath = "/uploads/" + folder + "/" + subFolder + "/" + fileName;
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
