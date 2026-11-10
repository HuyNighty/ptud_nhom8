package com.quanlynhakhoa.enums;




import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public enum PhuongThucNhapThanhToan {

        CASH("Tiền mặt"), BANK("Chuyển khoản");
        private final String nhan;
        PhuongThucNhapThanhToan(String nhan){this.nhan=nhan;}
        @Override public String toString(){return nhan;}

}
