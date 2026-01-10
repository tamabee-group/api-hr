package com.tamabee.api_hr.service.core.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface IUploadService {
    /**
     * Upload file và trả về đường dẫn tương đối
     * @param file File cần upload
     * @param folder Thư mục đích (vd: "avatar", "documents")
     * @param subFolder Thư mục con (vd: employeeCode)
     * @return Đường dẫn tương đối của file (vd: /uploads/avatar/ABC123/file.webp)
     */
    String uploadFile(MultipartFile file, String folder, String subFolder);
    
    /**
     * Xóa file theo đường dẫn
     * @param filePath Đường dẫn tương đối của file
     * @return true nếu xóa thành công
     */
    boolean deleteFile(String filePath);
}
