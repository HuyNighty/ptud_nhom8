package com.quanlynhakhoa.benhnhan.hopdong;

import java.sql.SQLException;
import java.util.ArrayList;

public interface TraCuuBenhNhan {
    ArrayList<BenhNhanTomTat> timBenhNhan(String tuKhoa) throws SQLException;

    BenhNhanTomTat layTheoMa(String maBenhNhan) throws SQLException;
}
