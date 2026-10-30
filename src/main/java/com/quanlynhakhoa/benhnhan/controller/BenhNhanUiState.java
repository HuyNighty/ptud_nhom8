package com.quanlynhakhoa.benhnhan.controller;

/** EDT-confined state: latest selection wins; only one save may run. */
final class BenhNhanUiState {
    private long detailRequest;
    private boolean saving;

    long nextDetailRequest() { return ++detailRequest; }
    boolean isLatestDetail(long request) { return request == detailRequest; }
    boolean beginSave() {
        if (saving) return false;
        saving = true;
        return true;
    }
    void finishSave() { saving = false; }
}
