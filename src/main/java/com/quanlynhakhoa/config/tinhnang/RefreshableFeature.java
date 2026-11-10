package com.quanlynhakhoa.config.tinhnang;

/** Called on authorized navigation, so cached pages read current committed data. */
public interface RefreshableFeature {
    void refreshData();
}
