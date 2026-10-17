package com.quanlynhakhoa.features.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

/** Một lô trong đợt kiểm kê: tồn sổ sách lúc tạo đợt và số thực tế đếm được — bảng dbo.ChiTietKiemKe. */
@Entity(name = "ChiTietKiemKeJpaEntity")
@Table(name = "ChiTietKiemKe", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StocktakeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maKiemKe", nullable = false)
    @Getter(AccessLevel.NONE)
    private Stocktake stocktake;

    @Column(name = "maLo", nullable = false)
    private Integer maLo;

    @Column(name = "soLuongSoSach", nullable = false)
    private int soLuongSoSach;

    @Column(name = "soLuongThucTe")
    private Integer soLuongThucTe;

    @Nationalized
    @Column(name = "ghiChu", length = 200)
    private String ghiChu;

    public static StocktakeLine snapshot(Integer maLo, int soSach) {
        StocktakeLine line = new StocktakeLine();
        line.maLo = maLo;
        line.soLuongSoSach = soSach;
        return line;
    }

    void attachTo(Stocktake owner) {
        this.stocktake = owner;
    }

    void record(Integer thucTe, String note) {
        this.soLuongThucTe = thucTe;
        this.ghiChu = note;
    }

    /** Thực tế − sổ sách; null khi chưa đếm. */
    public Integer chenhLech() {
        return soLuongThucTe == null ? null : soLuongThucTe - soLuongSoSach;
    }
}
