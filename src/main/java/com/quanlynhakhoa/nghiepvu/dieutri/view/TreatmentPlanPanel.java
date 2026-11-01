package com.quanlynhakhoa.nghiepvu.dieutri.view;

import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.nghiepvu.dieutri.service.TreatmentPlanService;
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

public final class TreatmentPlanPanel extends JPanel {
    final JTextField planId=new JTextField(), chartId=new JTextField(), visitId=new JTextField(), planDate=new JTextField(LocalDate.now().toString()), goal=new JTextField();
    final JTextField serviceId=new JTextField(), tooth=new JTextField(), dueDate=new JTextField(LocalDate.now().toString()), stepNote=new JTextField();
    final JButton createPlan=new JButton("Tạo kế hoạch"), addStep=new JButton("Thêm bước điều trị"), completeStep=new JButton("Hoàn thành bước"), cancelStep=new JButton("Hủy bước"), completePlan=new JButton("Hoàn tất kế hoạch"), cancelPlan=new JButton("Hủy kế hoạch"), refresh=new JButton("Tải lại");
    final DefaultTableModel planModel=readonly("Mã KH","Mã hồ sơ","Mã BN","Bệnh nhân","Mã phiếu khám","Ngày lập","Mục tiêu","Trạng thái","Bác sĩ");
    final DefaultTableModel detailModel=readonly("Mã CT","Mã DV","Dịch vụ","Vị trí răng","Ngày dự kiến","Ngày thực hiện","Kết quả","Trạng thái","Nhân viên","Ghi chú");
    final JTable plans=new JTable(planModel), details=new JTable(detailModel);

    public TreatmentPlanPanel(DesktopSession session){
        setLayout(new BorderLayout(8,8)); JPanel create=new JPanel(new GridLayout(0,4,7,7));
        field(create,"Mã kế hoạch (≤10)",planId);
        field(create,"Mã hồ sơ bệnh án",chartId);
        field(create,"Mã phiếu khám (tùy chọn)",visitId);
        field(create,"Ngày lập yyyy-MM-dd",planDate);
        field(create,"Mục tiêu",goal);create.add(createPlan);create.add(refresh);
        JPanel step=new JPanel(new GridLayout(0,4,7,7));
        field(step,"Mã kế hoạch đang chọn",new JLabel("Chọn ở bảng trên"));
        field(step,"Mã dịch vụ",serviceId);
        field(step,"Vị trí răng",tooth);
        field(step,"Ngày dự kiến yyyy-MM-dd",dueDate);
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
        bottom.add(new JLabel("Chọn kế hoạch, sau đó thêm/hoàn thành/hủy từng bước. Mã hồ sơ và dịch vụ phải tồn tại."),BorderLayout.NORTH);
        bottom.add(new JScrollPane(details),BorderLayout.CENTER);
        bottom.add(step,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,top,bottom);
        split.setResizeWeight(.48);
        add(split,BorderLayout.CENTER);
        new TreatmentPlanController(this,new TreatmentPlanService(session));
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
    String selectedPlan(){
        int r=plans.getSelectedRow();
        return r<0?null:String.valueOf(plans.getValueAt(plans.convertRowIndexToModel(r),0));
    }
    Integer selectedDetail(){
        int r=details.getSelectedRow();
        if(r<0)return null;
        return Integer.valueOf(String.valueOf(details.getValueAt(details.convertRowIndexToModel(r),0)));
    }
}
