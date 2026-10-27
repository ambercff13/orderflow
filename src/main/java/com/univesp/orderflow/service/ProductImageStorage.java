package com.univesp.orderflow.service;

import com.univesp.orderflow.exceptions.ApiErrorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Component
public class ProductImageStorage {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path uploadDir;

    public ProductImageStorage(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new ApiErrorException(HttpStatus.BAD_REQUEST, "Formato inválido. Use JPG, PNG ou WEBP.");
        }

        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar a imagem.");
        }
        return fileName;
    }

    public void delete(String fileName) {
        if (fileName == null) return;
        try {
            Files.deleteIfExists(uploadDir.resolve(fileName));
        } catch (IOException ignored) {
            // Se não conseguir apagar o arquivo antigo, não impede a atualização
        }
    }

    public Resource load(String fileName) {
        try {
            Path path = uploadDir.resolve(fileName).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new ApiErrorException(HttpStatus.NOT_FOUND, "Imagem não encontrada.");
            }
            return resource;
        } catch (IOException e) {
            throw new ApiErrorException(HttpStatus.NOT_FOUND, "Imagem não encontrada.");
        }
    }
}
