package com.quanlynhakhoa.enums;




import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public enum PhuongThucNhapThanhToan {

        CASH("Tiền mặt"), BANK("Chuyển khoản");
        private final String label;
        PhuongThucNhapThanhToan(String label){this.label=label;}
        @Override public String toString(){return label;}

}
