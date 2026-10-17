package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.Supplier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface SupplierRepository extends Repository<Supplier, String> {

    Optional<Supplier> findById(String maNhaCungCap);

    List<Supplier> findAllByOrderByTenNhaCungCapAsc();

    Supplier saveAndFlush(Supplier supplier);

    /** Số lớn nhất sau tiền tố "NCC", để sinh mã kế tiếp. */
    @Query("select max(cast(substring(s.maNhaCungCap, 4) as Integer)) from NhaCungCapJpaEntity s")
    Integer findMaxCodeNumber();
}
