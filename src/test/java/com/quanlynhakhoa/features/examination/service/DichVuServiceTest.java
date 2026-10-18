package com.quanlynhakhoa.features.examination.service;

import com.quanlynhakhoa.features.examination.dto.request.DichVuCreateRequest;
import com.quanlynhakhoa.features.examination.dto.request.DichVuUpdateRequest;
import com.quanlynhakhoa.features.examination.dto.response.DichVuResponse;
import com.quanlynhakhoa.features.examination.entity.DichVu;
import com.quanlynhakhoa.features.examination.repository.DichVuRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DichVuServiceTest {

    private final DichVuRepository repository = mock(DichVuRepository.class);
    private final DichVuAccessPolicy accessPolicy = new DichVuAccessPolicy();
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC
    );
    private final DichVuService service = new DichVuService(repository, accessPolicy, clock);

    @BeforeEach
    void allowCatalogManagement() {
        setAuthorities(DichVuAccessPolicy.SERVICE_READ, DichVuAccessPolicy.SERVICE_MANAGE);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsCatalogEntryAndNormalizesTextAndPrice() {
        when(repository.existsById("DV0001")).thenReturn(false);
        when(repository.existsByTenDichVuIgnoreCase("Khám tổng quát")).thenReturn(false);
        when(repository.save(any(DichVu.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DichVuResponse response = service.taoMoi(new DichVuCreateRequest(
                " DV0001 ", " Khám tổng quát ", " Mô tả ",
                new BigDecimal("250000"), false
        ));

        assertEquals("DV0001", response.maDichVu());
        assertEquals("Khám tổng quát", response.tenDichVu());
        assertEquals("Mô tả", response.moTa());
        assertEquals(new BigDecimal("250000.00"), response.donGia());
        assertTrue(response.dangApDung());
        assertEquals(clock.instant(), response.taoLuc().toInstant(ZoneOffset.UTC));
        verify(repository).save(any(DichVu.class));
    }

    @Test
    void doesNotAllowDuplicateServiceName() {
        when(repository.existsById("DV0002")).thenReturn(false);
        when(repository.existsByTenDichVuIgnoreCase("Khám tổng quát")).thenReturn(true);

        DichVuException exception = assertThrows(DichVuException.class, () -> service.taoMoi(
                new DichVuCreateRequest("DV0002", "Khám tổng quát", null,
                        new BigDecimal("100000"), false)
        ));

        assertEquals(DichVuException.Code.NAME_ALREADY_EXISTS, exception.getCode());
        verify(repository, never()).save(any(DichVu.class));
    }

    @Test
    void rejectsNegativePrice() {
        when(repository.existsById("DV0003")).thenReturn(false);
        when(repository.existsByTenDichVuIgnoreCase("Tẩy trắng")).thenReturn(false);

        assertEquals(DichVuException.Code.INVALID_DATA,
                assertThrows(DichVuException.class, () -> service.taoMoi(
                        new DichVuCreateRequest("DV0003", "Tẩy trắng", null,
                                new BigDecimal("-1"), false)
                )).getCode());
        verify(repository, never()).save(any(DichVu.class));
    }

    @Test
    void filtersByKeywordAndActiveStatus() {
        DichVu active = new DichVu("DV0001", "Khám tổng quát", null,
                new BigDecimal("100000.00"), false, clock.instant().atOffset(ZoneOffset.UTC).toLocalDateTime());
        DichVu inactive = new DichVu("DV0002", "Khám chuyên sâu", null,
                new BigDecimal("200000.00"), true, clock.instant().atOffset(ZoneOffset.UTC).toLocalDateTime());
        inactive.capNhatTrangThai(false, clock.instant().atOffset(ZoneOffset.UTC).toLocalDateTime());
        when(repository.findAllByOrderByTenDichVuAsc()).thenReturn(List.of(active, inactive));

        List<DichVuResponse> result = service.timKiem("KHÁM", true);

        assertEquals(1, result.size());
        assertEquals("DV0001", result.getFirst().maDichVu());
    }

    @Test
    void updateChecksDuplicateNameAndCanChangeStatus() {
        DichVu entity = new DichVu("DV0001", "Khám tổng quát", null,
                new BigDecimal("100000.00"), false, clock.instant().atOffset(ZoneOffset.UTC).toLocalDateTime());
        when(repository.findById("DV0001")).thenReturn(Optional.of(entity));
        when(repository.existsByTenDichVuIgnoreCaseAndMaDichVuNot("Khám chuyên sâu", "DV0001"))
                .thenReturn(false);
        when(repository.save(any(DichVu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DichVuResponse updated = service.capNhat("DV0001", new DichVuUpdateRequest(
                "Khám chuyên sâu", null, new BigDecimal("120000.00"), true
        ));
        DichVuResponse inactive = service.doiTrangThai("DV0001", false);

        assertEquals("Khám chuyên sâu", updated.tenDichVu());
        assertEquals(new BigDecimal("120000.00"), updated.donGia());
        assertFalse(inactive.dangApDung());
    }

    @Test
    void readOnlyAuthorityCannotCreateOrModifyServices() {
        setAuthorities(DichVuAccessPolicy.SERVICE_READ);

        assertThrows(AccessDeniedException.class, () -> service.taoMoi(
                new DichVuCreateRequest("DV0001", "Khám tổng quát", null,
                        new BigDecimal("100000"), false)
        ));
        verifyNoInteractions(repository);
    }

    @Test
    void anonymousUserCannotReadCatalog() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.timKiem("", null));
        verifyNoInteractions(repository);
    }

    private static void setAuthorities(String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "test-account", "N/A", granted
        ));
        SecurityContextHolder.setContext(context);
    }
}
