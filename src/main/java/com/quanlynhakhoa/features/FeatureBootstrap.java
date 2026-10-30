package com.quanlynhakhoa.features;

import com.quanlynhakhoa.account.ui.AccountPanel;
import com.quanlynhakhoa.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.navigation.Module;

public final class FeatureBootstrap {
    private FeatureBootstrap() { }

    public static FeatureRegistry createRegistry() {
        return FeatureRegistry.builder()
                .register(Module.ACCOUNTS, AccountPanel::new)
                .register(Module.PATIENTS, BenhNhanPanel::new)
                .build();
    }
}
