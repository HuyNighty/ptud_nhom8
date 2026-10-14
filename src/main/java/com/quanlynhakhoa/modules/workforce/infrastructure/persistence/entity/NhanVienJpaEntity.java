
package com.quanlynhakhoa.modules.workforce.infrastructure.persistence.entity;

import com.quanlynhakhoa.modules.workforce.enums.EmployeePosition;
import com.quanlynhakhoa.modules.workforce.enums.EmployeeStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

@Entity
@Table(name = "NhanVien", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NhanVienJpaEntity {

    @Id
    @Column(name = "maNhanVien", length = 10, nullable = false)
    private String maNhanVien;

    @Nationalized
    @Column(name = "hoTen", length = 100, nullable = false)
    private String hoTen;

    @Column(name = "soDienThoai", length = 15, nullable = false)
    private String soDienThoai;

    @Enumerated(EnumType.STRING)
    @Column(name = "chucDanh", length = 10, nullable = false)
    private EmployeePosition chucDanh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", length = 20, nullable = false)
    private EmployeeStatus trangThai;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Column(name = "gioiTinh", length = 5)
    private String gioiTinh;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "ngayVaoLam")
    private LocalDate ngayVaoLam;

    @Nationalized
    @Column(name = "chuyenMon", length = 200)
    private String chuyenMon;

    @Column(name = "ngayNghiViec")
    private LocalDate ngayNghiViec;
}
