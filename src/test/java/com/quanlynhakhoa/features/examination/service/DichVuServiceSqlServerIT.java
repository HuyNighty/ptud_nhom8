package com.quanlynhakhoa.features.examination.service;

import com.quanlynhakhoa.features.examination.dto.request.DichVuCreateRequest;
import com.quanlynhakhoa.features.examination.dto.request.DichVuUpdateRequest;
import com.quanlynhakhoa.features.examination.dto.response.DichVuResponse;
import com.quanlynhakhoa.features.examination.repository.DichVuRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Explicit SQL Server integration test. It is named *IT so it is not run by
 * Surefire's default test-name patterns; invoke it explicitly after SQL Server
 * and the application's required environment variables are configured.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "n08.desktop.enabled=false",
                "n08.security.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        }
)
class DichVuServiceSqlServerIT {

    @Autowired
    private DichVuService service;

    @Autowired
    private DichVuRepository repository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void servicePersistsUpdateAndSoftDisableOnSqlServer() {
        setManageAuthority();
        String code = "IT" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
        String name = "Kiểm thử dịch vụ " + code;

        DichVuResponse created = service.taoMoi(new DichVuCreateRequest(
                code, name, "Tạo bởi integration test", new BigDecimal("123456.50"), true
        ));
        entityManager.flush();
        assertTrue(repository.findById(code).isPresent());
        assertEquals(new BigDecimal("123456.50"), created.donGia());

        DichVuResponse updated = service.capNhat(code, new DichVuUpdateRequest(
                name + " đã sửa", "Mô tả đã sửa", new BigDecimal("200000"), false
        ));
        entityManager.flush();
        assertEquals("Mô tả đã sửa", updated.moTa());
        assertFalse(updated.laCanLamSang());

        DichVuResponse disabled = service.doiTrangThai(code, false);
        entityManager.flush();
        assertFalse(disabled.dangApDung());
        assertTrue(repository.findById(code).isPresent(), "Ngừng áp dụng không được xóa dữ liệu");
    }

    private static void setManageAuthority() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "integration-test-account",
                "N/A",
                List.of(
                        new SimpleGrantedAuthority(DichVuAccessPolicy.SERVICE_READ),
                        new SimpleGrantedAuthority(DichVuAccessPolicy.SERVICE_MANAGE)
                )
        ));
        SecurityContextHolder.setContext(context);
    }
}
