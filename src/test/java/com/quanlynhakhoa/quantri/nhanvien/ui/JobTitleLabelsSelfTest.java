package com.quanlynhakhoa.quantri.nhanvien.ui;

import com.quanlynhakhoa.model.nhanvien.YeuCauTaoNhanVien;
import com.quanlynhakhoa.model.nhanvien.DongDanhSachNhanVien;
import com.quanlynhakhoa.quantri.nhanvien.*;
import com.quanlynhakhoa.model.taikhoan.NhanVienDuDieuKien;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import com.quanlynhakhoa.quantri.nhanvien.entity.EmployeeStatus;
import javax.swing.*;

/** Kiểm trình bày và giữ nguyên mã, không mở database hoặc dialog tương tác. */
public final class JobTitleLabelsSelfTest {
    public static void main(String[] args) throws Exception {
        Map<String,String> expected=Map.of("AD","Quản trị hệ thống","LT","Lễ tân",
                "BS","Bác sĩ","YT","Y tá","QL","Quản lý phòng khám","KHO","Quản lý kho");
        SwingUtilities.invokeAndWait(() -> {
            expected.forEach((code,label) -> require(label.equals(JobTitleLabels.label(code))));
            verifyTable(expected);
            JComboBox<String> picker=EmployeePanel.createJobPicker();
            require(picker.getItemCount()==5);
            for(int i=0;i<picker.getItemCount();i++) {
                String code=picker.getItemAt(i);
                picker.setSelectedIndex(i);
                JLabel rendered=(JLabel)picker.getRenderer().getListCellRendererComponent(new JList<>(),code,i,false,false);
                require(expected.get(code).equals(rendered.getText()));
                require(code.equals(picker.getSelectedItem()));
                var request=EmployeeCreateService.validate(new YeuCauTaoNhanVien(
                        "NVTEST","Kiểm thử","0000000001",(String)picker.getSelectedItem(),
                        null,null,null,LocalDate.of(2020,1,1),null));
                require(code.equals(request.jobTitle()));
                verifyJdbcBinding(request);
                var employee=new NhanVienDuDieuKien("NVTEST","Kiểm thử",code,NhanVienDuDieuKien.roleForJob(code));
                require(employee.displayLabel().endsWith("("+expected.get(code)+")"));
                require(employee.jobTitle().equals(code));
            }
            try {
                EmployeeCreateService.validate(new YeuCauTaoNhanVien("NVTEST","Kiểm thử","0000000001",
                        "AD",null,null,null,LocalDate.of(2020,1,1),null));
                throw new AssertionError("Không được mở thêm quyền tạo AD");
            } catch(IllegalArgumentException expectedRejection) { }
        });
        System.out.println("PASS: 6 nhãn trong bảng, 5 lựa chọn giữ mã qua UI/validation/picker; AD vẫn bị chặn.");
    }
    private static void verifyTable(Map<String, String> expected) {
        try {
            Class<?> type = Class.forName(EmployeePanel.class.getName() + "$EmployeeTableModel");
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            var model = (javax.swing.table.TableModel) constructor.newInstance();
            var setRows = type.getDeclaredMethod("setRows", List.class);
            setRows.setAccessible(true);
            for (var entry : expected.entrySet()) {
                var row = new DongDanhSachNhanVien("NVTEST", "Kiểm thử", entry.getKey(),
                        EmployeeStatus.DANG_LAM_VIEC, null, null, null);
                setRows.invoke(model, List.of(row));
                require(entry.getValue().equals(model.getValueAt(0, 2)));
                require(entry.getKey().equals(row.jobTitle()));
            }
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Không kiểm tra được cột chức danh", failure);
        }
    }
    /** Chỉ kiểm hợp đồng bind PreparedStatement; không phải tích hợp SQL Server. */
    private static void verifyJdbcBinding(YeuCauTaoNhanVien request) {
        var boundCode = new java.util.concurrent.atomic.AtomicReference<String>();
        var statement = (java.sql.PreparedStatement) java.lang.reflect.Proxy.newProxyInstance(
                JobTitleLabelsSelfTest.class.getClassLoader(), new Class<?>[] {java.sql.PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setString") && Integer.valueOf(4).equals(args[0])) {
                        boundCode.set((String) args[1]);
                    }
                    if (method.getName().equals("executeUpdate")) return 1;
                    return null;
                });
        var connection = (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
                JobTitleLabelsSelfTest.class.getClassLoader(), new Class<?>[] {java.sql.Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) return statement;
                    throw new AssertionError("Unexpected JDBC call: " + method.getName());
                });
        try {
            new EmployeeCreateDao().insert(connection, request);
            require(request.jobTitle().equals(boundCode.get()));
        } catch (java.sql.SQLException failure) {
            throw new AssertionError(failure);
        }
    }
    private static void require(boolean condition) { if(!condition) throw new AssertionError("Job title contract"); }
}
