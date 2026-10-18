package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface MedicalSupplyRepository extends Repository<MedicalSupply, String> {

    Optional<MedicalSupply> findById(String maVatTu);

    boolean existsById(String maVatTu);

    MedicalSupply saveAndFlush(MedicalSupply supply);

    long count();

    /** {@code keyword} đã ở dạng chữ thường, rỗng nghĩa là lấy tất cả. */
    @Query("""
            select v from VatTuJpaEntity v
            where :keyword = ''
               or lower(v.maVatTu) like concat('%', :keyword, '%')
               or lower(v.tenVatTu) like concat('%', :keyword, '%')
               or lower(coalesce(v.moTa, '')) like concat('%', :keyword, '%')
            order by v.maVatTu
            """)
    List<MedicalSupply> search(@Param("keyword") String keyword);

    /** Số lớn nhất sau tiền tố "VT", để gợi ý mã kế tiếp. */
    @Query("select max(cast(substring(v.maVatTu, 3) as Integer)) from VatTuJpaEntity v")
    Integer findMaxCodeNumber();
}
