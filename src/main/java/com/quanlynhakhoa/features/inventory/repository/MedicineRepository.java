package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.Medicine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository extends Repository<Medicine, String> {

    Optional<Medicine> findById(String maThuoc);

    boolean existsById(String maThuoc);

    Medicine saveAndFlush(Medicine medicine);

    long count();

    long countByKiemSoatDacBietTrue();

    /** {@code keyword} đã ở dạng chữ thường, rỗng nghĩa là lấy tất cả. */
    @Query("""
            select m from ThuocJpaEntity m
            where :keyword = ''
               or lower(m.maThuoc) like concat('%', :keyword, '%')
               or lower(m.tenThuoc) like concat('%', :keyword, '%')
               or lower(coalesce(m.hoatChat, '')) like concat('%', :keyword, '%')
            order by m.maThuoc
            """)
    List<Medicine> search(@Param("keyword") String keyword);

    /** Số lớn nhất sau tiền tố "TH", để gợi ý mã kế tiếp. */
    @Query("select max(cast(substring(m.maThuoc, 3) as Integer)) from ThuocJpaEntity m")
    Integer findMaxCodeNumber();
}
