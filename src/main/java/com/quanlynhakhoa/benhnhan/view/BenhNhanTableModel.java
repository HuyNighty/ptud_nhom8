package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Dữ liệu cho bảng tblBenhNhan (chỉ đọc). */
public final class BenhNhanTableModel extends AbstractTableModel {
    static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] TEN_COT = {"Mã BN", "Họ tên", "Ngày sinh", "Giới tính", "SĐT"};

    private List<BenhNhan> danhSach = new ArrayList<>();

    public void datDuLieu(List<BenhNhan> moi) {
        danhSach = new ArrayList<>(moi);
        fireTableDataChanged();
    }

    public BenhNhan layDong(int dong) {
        return dong < 0 || dong >= danhSach.size() ? null : danhSach.get(dong);
    }

    /** Tìm vị trí dòng theo mã, -1 nếu không có. */
    public int timDong(String maBenhNhan) {
        for (int i = 0; i < danhSach.size(); i++) {
            if (danhSach.get(i).maBenhNhan().equals(maBenhNhan)) return i;
        }
        return -1;
    }

    @Override public int getRowCount() { return danhSach.size(); }
    @Override public int getColumnCount() { return TEN_COT.length; }
    @Override public String getColumnName(int cot) { return TEN_COT[cot]; }
    @Override public boolean isCellEditable(int dong, int cot) { return false; }

    @Override
    public Object getValueAt(int dong, int cot) {
        BenhNhan bn = danhSach.get(dong);
        return switch (cot) {
            case 0 -> bn.maBenhNhan();
            case 1 -> bn.hoTen();
            case 2 -> bn.ngaySinh() == null ? "" : NGAY.format(bn.ngaySinh());
            case 3 -> bn.gioiTinh().tenHienThi();
            case 4 -> dinhDangSoDienThoai(bn.soDienThoai());
            default -> "";
        };
    }

    /** 0909123456 → 0909 123 456 (chỉ để hiển thị). */
    static String dinhDangSoDienThoai(String so) {
        if (so == null || so.length() != 10) return so == null ? "" : so;
        return so.substring(0, 4) + " " + so.substring(4, 7) + " " + so.substring(7);
    }
}
