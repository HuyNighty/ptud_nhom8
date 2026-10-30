package com.quanlynhakhoa.benhnhan.controller;

/** Pure Java regression for stale detail requests and duplicate submissions. */
public final class BenhNhanUiStateSelfTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        BenhNhanUiState state = new BenhNhanUiState();
        long first = state.nextDetailRequest();
        check(state.isLatestDetail(first), "first detail current");
        long second = state.nextDetailRequest();
        check(!state.isLatestDetail(first), "old detail discarded");
        check(state.isLatestDetail(second), "new detail current");
        check(!state.isLatestDetail(0), "missing token discarded");
        check(state.beginSave(), "first save accepted");
        check(!state.beginSave(), "double submit prevented");
        state.nextDetailRequest();
        check(!state.isLatestDetail(second), "refresh invalidates old detail");
        check(!state.beginSave(), "save still guarded during refresh");
        state.finishSave();
        check(state.beginSave(), "retry allowed after completion");
        state.finishSave();
        check(state.beginSave(), "further valid save allowed");
        System.out.println("PASS: BENH NHAN UI STATE (" + checks + " checks; no DB, no GUI)");
    }
}
