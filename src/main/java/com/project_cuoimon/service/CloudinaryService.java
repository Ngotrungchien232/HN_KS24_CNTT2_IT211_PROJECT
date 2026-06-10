package com.project_cuoimon.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    // Hàm nhận file ảnh từ Client gửi lên và đẩy lên Cloudinary
    public String uploadFile(MultipartFile file) throws IOException {
        // Đẩy file lên đám mây Cloudinary và lưu vào thư mục "rikkei_bank_ekyc" trên mây cho gọn gàng
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "rikkei_bank_ekyc"));

        // Trả về URL bảo mật (HTTPS secure_url) của bức ảnh đã upload thành công
        return uploadResult.get("secure_url").toString();
    }
}
