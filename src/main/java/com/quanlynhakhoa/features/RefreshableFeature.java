package com.quanlynhakhoa.features;

/** Called on authorized navigation, so cached pages read current committed data. */
public interface RefreshableFeature {
    void refreshData();
}
