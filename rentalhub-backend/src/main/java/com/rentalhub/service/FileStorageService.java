package com.rentalhub.service;

import com.rentalhub.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            throw new BadRequestException("Cannot store empty file");
        }

        String originalFilename = StringUtils.cleanPath(
            java.util.Objects.requireNonNull(file.getOriginalFilename())
        );

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetDir = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();

            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(newFilename);

            if (!targetPath.getParent().equals(targetDir)) {
                throw new BadRequestException("Invalid file path");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = Paths.get(uploadDir, subDir, newFilename)
                .toString()
                .replace("\\", "/");

            log.info("Stored file: {} -> {}", originalFilename, relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new BadRequestException("Failed to store file: " + originalFilename);
        }
    }

    public Path resolve(String relativePath) {
        return Paths.get(uploadDir).toAbsolutePath().normalize()
            .resolve(Paths.get(relativePath).getFileName().toString().isEmpty()
                ? relativePath
                : relativePath.substring(uploadDir.length()));
    }

    public Path resolveAbsolute(String storedRelativePath) {
        return Paths.get(".").toAbsolutePath().normalize().resolve(storedRelativePath);
    }

    public void delete(String relativePath) {
        try {
            Path path = resolveAbsolute(relativePath);
            Files.deleteIfExists(path);
            log.info("Deleted file: {}", relativePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }
}