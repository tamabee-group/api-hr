package com.tamabee.api_hr.service.core.impl;

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

@Slf4j
@Service
public class UploadServiceImpl implements IUploadService {
    
    @Value("${file.upload.path:uploads}")
    private String uploadPath;
    
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
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi lưu file: " + e.getMessage());
        }
    }
    
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
            log.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }
}
