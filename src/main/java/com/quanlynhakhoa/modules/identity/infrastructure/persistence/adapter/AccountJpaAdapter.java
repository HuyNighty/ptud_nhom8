
package com.quanlynhakhoa.modules.identity.infrastructure.persistence.adapter;

import com.quanlynhakhoa.modules.identity.application.model.AccountSnapshot;
import com.quanlynhakhoa.modules.identity.application.port.out.AccountRepositoryPort;
import com.quanlynhakhoa.modules.identity.infrastructure.persistence.entity.TaiKhoanJpaEntity;
import com.quanlynhakhoa.modules.identity.infrastructure.persistence.repository.TaiKhoanJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class AccountJpaAdapter implements AccountRepositoryPort {

    private final TaiKhoanJpaRepository repository;

    public AccountJpaAdapter(TaiKhoanJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountSnapshot> findByUsername(String username) {
        return repository.findByTenDangNhap(username)
                .map(this::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountSnapshot> findById(String accountId) {
        return repository.findById(accountId)
                .map(this::toSnapshot);
    }

    private AccountSnapshot toSnapshot(TaiKhoanJpaEntity entity) {
        return new AccountSnapshot(
                entity.getMaTaiKhoan(),
                entity.getMaNhanVien(),
                entity.getTenDangNhap(),
                entity.getMatKhauHash(),
                entity.getVaiTro(),
                entity.getTrangThai(),
                entity.getSoLanSai(),
                entity.getLanSaiDau(),
                entity.getKhoaDen(),
                entity.getNguonKhoa(),
                entity.isPhaiDoiMatKhau()
        );
    }
}
