package com.tamabee.api_hr.controller.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller serve static files từ thư mục uploads
 */
@Slf4j
@RestController
@RequestMapping("/uploads")
public class FileController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @GetMapping("/{tenant}/{folder}/{filename}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String tenant,
            @PathVariable String folder,
            @PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadPath, tenant, folder, filename).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Xác định content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(resource);
        } catch (IOException e) {
            log.error("Error serving file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
