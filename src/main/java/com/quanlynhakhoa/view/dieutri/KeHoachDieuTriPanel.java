package com.quanlynhakhoa.view.dieutri;

import com.quanlynhakhoa.controller.dieutri.KeHoachDieuTriController;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.service.dieutri.KeHoachDieuTriService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public final class KeHoachDieuTriPanel extends JPanel {
    final JTextField planId=new JTextField(), chartId=new JTextField(), visitId=new JTextField(), planDate=new JTextField(LocalDate.now().toString()), goal=new JTextField();
    final JTextField serviceId=new JTextField(), tooth=new JTextField(), dueDate=new JTextField(LocalDate.now().toString()), stepNote=new JTextField();
    final JButton createPlan=new JButton("Tạo kế hoạch"), addStep=new JButton("Thêm bước điều trị"), completeStep=new JButton("Hoàn thành bước"), cancelStep=new JButton("Hủy bước"), completePlan=new JButton("Hoàn tất kế hoạch"), cancelPlan=new JButton("Hủy kế hoạch"), refresh=new JButton("Làm mới");
    final DefaultTableModel planModel=readonly("Mã KH","Mã hồ sơ","Mã BN","Bệnh nhân","Mã phiếu khám","Ngày lập","Mục tiêu","Trạng thái","Bác sĩ");
    final DefaultTableModel detailModel=readonly("Mã CT","Mã DV","Dịch vụ","Vị trí răng","Ngày dự kiến","Ngày thực hiện","Kết quả","Trạng thái","Nhân viên","Ghi chú");
    final JTable plans=new JTable(planModel), details=new JTable(detailModel);

    public KeHoachDieuTriPanel(DesktopSession session){
        setLayout(new BorderLayout(8,8)); JPanel create=new JPanel(new GridLayout(0,4,7,7));
        JLabel title = new JLabel("Kế hoạch điều trị");
        title.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 23));
        add(title, BorderLayout.NORTH);
        field(create,"Mã kế hoạch (≤10)",planId);
        field(create,"Mã hồ sơ bệnh án",chartId);
        field(create,"Mã phiếu khám (tùy chọn)",visitId);
        field(create,"Ngày lập yyyy-MM-dd",com.quanlynhakhoa.hethong.giaodien.DateInput.iso(planDate,"ngày lập kế hoạch"));
        field(create,"Mục tiêu",goal);create.add(createPlan);create.add(refresh);
        JPanel step=new JPanel(new GridLayout(0,4,7,7));
        field(step,"Mã kế hoạch đang chọn",new JLabel("Chọn ở bảng trên"));
        field(step,"Mã dịch vụ",serviceId);
        field(step,"Vị trí răng",tooth);
        field(step,"Ngày dự kiến yyyy-MM-dd",com.quanlynhakhoa.hethong.giaodien.DateInput.iso(dueDate,"ngày dự kiến điều trị"));
        field(step,"Ghi chú",stepNote);
        step.add(addStep);
        step.add(completeStep);
        step.add(cancelStep);
        step.add(completePlan);
        step.add(cancelPlan);
        JPanel top=new JPanel(new BorderLayout(5,5));
        top.add(create,BorderLayout.NORTH);
        top.add(new JScrollPane(plans),BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout(5,5));

        bottom.add(new JScrollPane(details),BorderLayout.CENTER);
        bottom.add(step,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,top,bottom);
        split.setResizeWeight(.48);
        add(split,BorderLayout.CENTER);
        if (session.role() != VaiTroHeThong.DOCTOR && session.role() != VaiTroHeThong.SYSTEM_ADMIN) {
            createPlan.setEnabled(false);
            addStep.setEnabled(false);
            completeStep.setEnabled(false);
            cancelStep.setEnabled(false);
            completePlan.setEnabled(false);
            cancelPlan.setEnabled(false);
        }
        new KeHoachDieuTriController(this,new KeHoachDieuTriService(session));
    }

    private static void field(JPanel panel,String label,java.awt.Component input){
        panel.add(new JLabel(label));
        panel.add(input);
    }

    private static DefaultTableModel readonly(String... names){
        return new DefaultTableModel(names,0){
            @Override public boolean isCellEditable(int r,int c){
                return false;
            }
        };
    }
    public String selectedPlan(){
        int r=plans.getSelectedRow();
        return r<0?null:String.valueOf(plans.getValueAt(plans.convertRowIndexToModel(r),0));
    }
    public Integer selectedDetail(){
        int r=details.getSelectedRow();
        if(r<0)return null;
        return Integer.valueOf(String.valueOf(details.getValueAt(details.convertRowIndexToModel(r),0)));
    }

    // Controller bindings; widgets remain owned by this view.
    public JButton addStep() { return addStep; }
    public JButton cancelPlan() { return cancelPlan; }
    public JButton cancelStep() { return cancelStep; }
    public JTextField chartId() { return chartId; }
    public JButton completePlan() { return completePlan; }
    public JButton completeStep() { return completeStep; }
    public JButton createPlan() { return createPlan; }
    public DefaultTableModel detailModel() { return detailModel; }
    public JTextField dueDate() { return dueDate; }
    public JTextField goal() { return goal; }
    public JTextField planDate() { return planDate; }
    public JTextField planId() { return planId; }
    public DefaultTableModel planModel() { return planModel; }
    public JTable plans() { return plans; }
    public JButton refresh() { return refresh; }
    public JTextField serviceId() { return serviceId; }
    public JTextField stepNote() { return stepNote; }
    public JTextField tooth() { return tooth; }
    public JTextField visitId() { return visitId; }
}
