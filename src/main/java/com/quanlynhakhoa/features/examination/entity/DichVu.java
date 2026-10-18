package com.quanlynhakhoa.features.examination.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "DichVuJpaEntity")
@Table(
        name = "DichVu",
        schema = "dbo",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_DichVu_TenDichVu",
                columnNames = "tenDichVu"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DichVu {

    @Id
    @Column(name = "maDichVu", length = 10, nullable = false)
    private String maDichVu;

    @Nationalized
    @Column(name = "tenDichVu", length = 100, nullable = false)
    private String tenDichVu;

    @Nationalized
    @Column(name = "moTa", length = 500)
    private String moTa;

    @Column(name = "donGia", precision = 12, scale = 2, nullable = false)
    private BigDecimal donGia;

    @Column(name = "laCanLamSang", nullable = false)
    private boolean laCanLamSang;

    @Column(name = "dangApDung", nullable = false)
    private boolean dangApDung;

    @Column(name = "taoLuc", nullable = false)
    private LocalDateTime taoLuc;

    @Column(name = "capNhatLuc", nullable = false)
    private LocalDateTime capNhatLuc;

    public DichVu(
            String maDichVu,
            String tenDichVu,
            String moTa,
            BigDecimal donGia,
            boolean laCanLamSang,
            LocalDateTime nowUtc
    ) {
        this.maDichVu = Objects.requireNonNull(maDichVu, "maDichVu");
        this.tenDichVu = Objects.requireNonNull(tenDichVu, "tenDichVu");
        this.moTa = moTa;
        this.donGia = Objects.requireNonNull(donGia, "donGia");
        this.laCanLamSang = laCanLamSang;
        this.dangApDung = true;
        this.taoLuc = Objects.requireNonNull(nowUtc, "nowUtc");
        this.capNhatLuc = nowUtc;
    }

    public void capNhatThongTin(
            String tenDichVu,
            String moTa,
            BigDecimal donGia,
            boolean laCanLamSang,
            LocalDateTime nowUtc
    ) {
        this.tenDichVu = Objects.requireNonNull(tenDichVu, "tenDichVu");
        this.moTa = moTa;
        this.donGia = Objects.requireNonNull(donGia, "donGia");
        this.laCanLamSang = laCanLamSang;
        this.capNhatLuc = Objects.requireNonNull(nowUtc, "nowUtc");
    }

    public void capNhatTrangThai(boolean dangApDung, LocalDateTime nowUtc) {
        this.dangApDung = dangApDung;
        this.capNhatLuc = Objects.requireNonNull(nowUtc, "nowUtc");
    }
}
