package com.tairui.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 本地目录
    @Value("${spring.application.file.upload.borrow-photo-path}")
    private String uploadPath;
    // 网络路径
    @Value("${spring.application.file.upload.borrow-photo-access}")
    private String accessPath;

    WebConfig(){
        System.out.println(uploadPath);
        System.out.println(accessPath);
    }
    /**
     * 保存上传的图片文件，返回可访问的 URL
     */
    public String savePhotoFile(MultipartFile photoFile) {
        try {
            String originalFilename = photoFile.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + ext;
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new RuntimeException("创建上传目录失败");
                }
            }
            File dest = new File(dir, newFileName);
            photoFile.transferTo(dest);
            return accessPath + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("保存照片失败", e);
        }
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadPath.startsWith("file:") ? uploadPath : "file:" + uploadPath;
        if (!location.endsWith("/")) {
            location += "/";
        }

        String handlerPattern = accessPath.endsWith("/") ? accessPath + "**" : accessPath + "/**";

        registry.addResourceHandler(handlerPattern)
                .addResourceLocations(location);
    }
    /**
     * 跨域配置
     *
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600);
    }
}
