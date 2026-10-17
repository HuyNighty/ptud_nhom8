package com.quanlynhakhoa.features.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;
import org.springframework.data.domain.Persistable;

/** Danh mục vật tư — bảng dbo.VatTu. */
@Entity(name = "VatTuJpaEntity")
@Table(name = "VatTu", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalSupply implements Persistable<String> {

    @Id
    @Column(name = "maVatTu", length = 10, nullable = false)
    private String maVatTu;

    @Nationalized
    @Column(name = "tenVatTu", length = 150, nullable = false)
    private String tenVatTu;

    @Nationalized
    @Column(name = "donViTinh", length = 30, nullable = false)
    private String donViTinh;

    @Nationalized
    @Column(name = "moTa", length = 300)
    private String moTa;

    @Column(name = "dinhMucToiThieu", nullable = false)
    private int dinhMucToiThieu;

    @Column(name = "daDatHang", nullable = false)
    private boolean daDatHang;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static MedicalSupply create(String maVatTu, String tenVatTu, String donViTinh,
                                       String moTa, int dinhMucToiThieu) {
        MedicalSupply supply = new MedicalSupply();
        supply.maVatTu = maVatTu;
        supply.updateInfo(tenVatTu, donViTinh, moTa, dinhMucToiThieu);
        return supply;
    }

    public void updateInfo(String tenVatTu, String donViTinh, String moTa, int dinhMucToiThieu) {
        this.tenVatTu = tenVatTu;
        this.donViTinh = donViTinh;
        this.moTa = moTa;
        this.dinhMucToiThieu = dinhMucToiThieu;
    }

    @Override
    public String getId() {
        return maVatTu;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }
}
