package com.quanlynhakhoa.service.benhnhan;

import com.quanlynhakhoa.model.benhnhan.BenhNhanTomTat;
import java.sql.SQLException;
import java.util.ArrayList;

public interface TraCuuBenhNhan {
    ArrayList<BenhNhanTomTat> timBenhNhan(String tuKhoa) throws SQLException;

    BenhNhanTomTat layTheoMa(String maBenhNhan) throws SQLException;
}
