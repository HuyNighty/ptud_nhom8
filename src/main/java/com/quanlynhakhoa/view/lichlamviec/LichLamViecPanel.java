package com.quanlynhakhoa.view.lichlamviec;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.model.lichlamviec.ThongTinPhanCong;
import com.quanlynhakhoa.model.lichlamviec.NhanVienChon;
import com.quanlynhakhoa.model.lichlamviec.DuLieuPhanCong;
import com.quanlynhakhoa.entity.CaLamViec;
import com.quanlynhakhoa.service.lichlamviec.PhanCongService;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.config.tinhnang.RefreshableFeature;
import java.awt.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

/** Two scoped screens share rendering and worker lifecycle, never employee identity resolution. */
public final class LichLamViecPanel extends JPanel implements RefreshableFeature {
    private final PhanCongService service;
    private final boolean quanLy;
    private final JTextField tuNgay=new JTextField(10),denNgay=new JTextField(10),tuKhoa=new JTextField(14);
    private final JComboBox<NhanVienChon> nhanVien=new JComboBox<>();
    private final JButton btnTruoc=new JButton("‹ Tuần trước"),btnTiep=new JButton("Tuần sau ›"),btnHomNay=new JButton("Tuần này"),btnLamMoi=new JButton("Làm mới"),btnTao=new JButton("+ Thêm phân công"),btnHuy=new JButton("Hủy phân công"),chiTiet=new JButton("Xem chi tiết");
    private final JLabel trangThai=new JLabel("Chưa tải dữ liệu");
    private final Rows model=new Rows();
    private final JTable table=new JTable(model);
    private List<CaLamViec> caLamViec=List.of();
    private boolean busy,dangCho;
    private String thongBao="";
    private String maChonSauTai;
    public LichLamViecPanel(DesktopSession session,boolean management){this(session,management,new PhanCongService(session,TeamDevConnection::open),true);}
    public LichLamViecPanel(DesktopSession session,boolean management,PhanCongService service,boolean autoLoad){
        new AuthorizationService(new RolePermissionPolicy()).require(session,management?Quyen.WORKFORCE_READ:Quyen.SCHEDULE_SELF_READ);
        this.quanLy=management;this.service=service;
        setLayout(new BorderLayout(0,14));setBackground(new Color(245,248,252));setBorder(new EmptyBorder(24,26,24,26));
        JPanel heading=new JPanel(new BorderLayout(0,8));heading.setOpaque(false);
        JLabel title=new JLabel(management?"Phân công ca":"Lịch làm việc của tôi");title.setFont(new Font("SansSerif",Font.BOLD,23));title.setForeground(new Color(29,61,102));heading.add(title,BorderLayout.NORTH);
        add(heading,BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(0,10));body.setOpaque(false);
        JPanel tools=new JPanel();tools.setLayout(new BoxLayout(tools,BoxLayout.Y_AXIS));tools.setBackground(Color.WHITE);tools.setBorder(new EmptyBorder(10,12,10,12));
        JPanel dates=new JPanel(new FlowLayout(FlowLayout.LEFT));dates.setOpaque(false);
        dates.add(btnTruoc);dates.add(btnHomNay);dates.add(btnTiep);dates.add(new JLabel("Từ ngày:"));dates.add(com.quanlynhakhoa.view.hethong.DateInput.iso(tuNgay,"ngày bắt đầu"));dates.add(new JLabel("Đến:"));dates.add(com.quanlynhakhoa.view.hethong.DateInput.iso(denNgay,"ngày kết thúc"));dates.add(btnLamMoi);dates.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(dates);
        setWeek(LocalDate.now());
        if(management){JPanel filters=new JPanel(new GridBagLayout());filters.setOpaque(false);nhanVien.setPreferredSize(new Dimension(260,34));nhanVien.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){return super.getListCellRendererComponent(l,v==null?"Tất cả nhân viên":v,i,s,f);}
        });nhanVien.addItem(null);
            GridBagConstraints filterCell=new GridBagConstraints();filterCell.gridy=0;filterCell.fill=GridBagConstraints.HORIZONTAL;filterCell.anchor=GridBagConstraints.SOUTH;filterCell.insets=new Insets(6,4,4,8);
            filterCell.gridx=0;filterCell.weightx=.35;filters.add(labeledField("Tìm nhân viên",tuKhoa),filterCell);
            filterCell.gridx=1;filterCell.weightx=.65;filters.add(labeledField("Nhân viên hiển thị",nhanVien),filterCell);
            filterCell.gridx=2;filterCell.weightx=0;filters.add(btnTao,filterCell);
            filters.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(filters);
            tuKhoa.setToolTipText("Chỉ tìm trong danh sách lựa chọn nhân viên; không tìm nội dung bảng phân công.");
            nhanVien.setToolTipText("Chọn nhân viên rồi nhấn Enter ở ô tìm để lọc bảng phân công.");
            JLabel hint=new JLabel("Tìm lựa chọn NV: thu hẹp danh sách chọn. Lọc bảng: chọn nhân viên rồi bấm Làm mới (người đang lọc luôn được giữ).");
            hint.setBorder(new EmptyBorder(4,4,0,0));hint.setText("Chọn nhân viên rồi nhấn Enter ở ô tìm để xem lịch. Làm mới sẽ bỏ lọc nhân viên.");hint.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(hint);}
        body.add(tools,BorderLayout.NORTH);
        table.setRowHeight(32);table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setFillsViewportHeight(true);table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());body.add(scroll,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.setOpaque(false);actions.add(chiTiet);if(management)actions.add(btnHuy);body.add(actions,BorderLayout.SOUTH);add(body,BorderLayout.CENTER);
        trangThai.setForeground(new Color(62,82,106));add(trangThai,BorderLayout.SOUTH);
        btnTruoc.addActionListener(e->move(-7));btnTiep.addActionListener(e->move(7));btnHomNay.addActionListener(e->{setWeek(LocalDate.now());refreshData();});btnLamMoi.addActionListener(e->{tuKhoa.setText("");if(management&&nhanVien.getItemCount()>0)nhanVien.setSelectedIndex(0);refreshData();});tuKhoa.addActionListener(e->refreshData());
        // Selection is an explicit filter, applied by Tải lại. Avoid recursive reload while replacing choices.
        chiTiet.addActionListener(e->showDetail());btnTao.addActionListener(e->create());btnHuy.addActionListener(e->cancel());
        for(JComponent control:new JComponent[]{tuNgay,denNgay,tuKhoa,nhanVien,btnTruoc,btnTiep,btnHomNay,btnLamMoi,btnTao,btnHuy,chiTiet}){
            Dimension preferred=control.getPreferredSize();control.setPreferredSize(new Dimension(preferred.width,34));
            if(control instanceof JButton button){button.setMargin(new Insets(6,12,6,12));button.setFocusPainted(false);}
        }
        table.getSelectionModel().addListSelectionListener(e->buttons());buttons();
        if(autoLoad)SwingUtilities.invokeLater(this::refreshData);
    }
    private void setWeek(LocalDate day){LocalDate monday=day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));tuNgay.setText(monday.toString());denNgay.setText(monday.plusDays(6).toString());}
    private void move(int days){try{LocalDate f=LocalDate.parse(tuNgay.getText().trim()),t=LocalDate.parse(denNgay.getText().trim());tuNgay.setText(f.plusDays(days).toString());denNgay.setText(t.plusDays(days).toString());refreshData();}catch(DateTimeException e){trangThai.setText("Ngày không hợp lệ. Dùng yyyy-MM-dd.");}}
    private ThongTinPhanCong selected(){int row=table.getSelectedRow();return row<0?null:model.danhSach.get(table.convertRowIndexToModel(row));}
    private void buttons(){
        boolean active=service.isActive(),write=quanLy&&service.canManage();
        for(Component c:new Component[]{tuNgay,denNgay,tuKhoa,nhanVien,btnTruoc,btnTiep,btnHomNay,btnLamMoi,table})c.setEnabled(!busy&&active);
        btnTao.setEnabled(!busy&&active&&write&&nhanVien.getItemCount()>1);
        ThongTinPhanCong p=selected();chiTiet.setEnabled(!busy&&active&&p!=null);btnHuy.setEnabled(!busy&&write&&p!=null&&PhanCongService.canCancelStatus(p.trangThai()));
    }
    private void clear(){model.danhSach=List.of();model.fireTableDataChanged();caLamViec=List.of();nhanVien.removeAllItems();nhanVien.addItem(null);}
    @Override public void refreshData(){
        if(busy){dangCho=true;return;}if(!service.isActive()){clear();trangThai.setText("Phiên đăng nhập đã kết thúc.");buttons();return;}
        try{
            LocalDate f=LocalDate.parse(tuNgay.getText().trim()),t=LocalDate.parse(denNgay.getText().trim());PhanCongService.range(f,t);
            String search=tuKhoa.getText();NhanVienChon chosen=(NhanVienChon)nhanVien.getSelectedItem();String id=chosen==null?null:chosen.id();
            busy=true;buttons();trangThai.setText("Đang tải lịch làm việc…");
            new SwingWorker<DuLieuPhanCong,Void>(){
                @Override protected DuLieuPhanCong doInBackground() throws Exception{return quanLy?service.management(f,t,search,id):service.mine(f,t);}
                @Override protected void done(){
                    busy=false;
                    try{
                        var data=get();if(!service.isActive()){clear();trangThai.setText("Phiên đăng nhập đã kết thúc.");return;}
                        model.danhSach=data.rows();model.fireTableDataChanged();caLamViec=data.shifts();
                        if(quanLy){nhanVien.removeAllItems();nhanVien.addItem(null);for(var employee:data.employees())nhanVien.addItem(employee);if(chosen!=null){if(!data.employees().contains(chosen))nhanVien.addItem(chosen);nhanVien.setSelectedItem(chosen);}}
                        String message=!data.message().isEmpty()?data.message():data.rows().isEmpty()?"Không có phân công trong khoảng ngày đã chọn.":data.rows().size()+" phân công (bao gồm lịch sử hủy).";
                        if(quanLy && data.shifts().isEmpty()) message += " Chưa có ca làm việc để phân công.";
                        if(quanLy && data.employees().isEmpty()) message += " Không tìm thấy nhân viên đang làm việc; xóa từ khóa để tải lại.";
                        trangThai.setText(thongBao+message);thongBao="";
                        if(maChonSauTai!=null){
                            for(int index=0;index<model.danhSach.size();index++)if(maChonSauTai.equals(model.danhSach.get(index).id())){
                                int visible=table.convertRowIndexToView(index);
                                if(visible>=0){table.setRowSelectionInterval(visible,visible);table.scrollRectToVisible(table.getCellRect(visible,0,true));}
                                break;
                            }
                            maChonSauTai=null;
                        }
                    }catch(Exception e){clear();trangThai.setText(error(e));}
                    finally{buttons();if(dangCho){dangCho=false;refreshData();}}
                }
            }.execute();
        }catch(DateTimeException|IllegalArgumentException e){trangThai.setText("Khoảng ngày không hợp lệ. Dùng yyyy-MM-dd.");}
    }
    private void showDetail(){ThongTinPhanCong p=selected();if(p==null)return;JTextArea text=new JTextArea("Mã: "+p.id()+"\nNhân viên: "+p.tenNhanVien()+" ("+p.nhanVienId()+")\nNgày: "+p.ngay()+"\nCa: "+p.ca()+"\nNhiệm vụ: "+p.nhiemVu()+"\nTrạng thái: "+p.trangThaiHienThi()+"\nLý do hủy: "+(p.lyDoHuy()==null?"—":p.lyDoHuy()),10,48);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);JOptionPane.showMessageDialog(this,new JScrollPane(text),"Chi tiết phân công",JOptionPane.INFORMATION_MESSAGE);}
    private void create(){
        if(busy||!service.canManage())return;
        JComboBox<NhanVienChon> employee=new JComboBox<>();for(int i=1;i<nhanVien.getItemCount();i++)employee.addItem(nhanVien.getItemAt(i));
        if(nhanVien.getSelectedItem()!=null)employee.setSelectedItem(nhanVien.getSelectedItem());
        JTextField shiftName=new JTextField(28),startTime=new JTextField("08:00"),endTime=new JTextField("12:00");
        JTextField date=new JTextField(LocalDate.now().toString());date.setEditable(false);date.setHorizontalAlignment(JTextField.CENTER);
        JButton chooseDate=new JButton("Lịch");
        JPanel datePicker=new JPanel(new BorderLayout(6,0));datePicker.add(date,BorderLayout.CENTER);datePicker.add(chooseDate,BorderLayout.EAST);
        chooseDate.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                LocalDate selected=com.quanlynhakhoa.view.benhnhan.ChonNgayDialog.chon(
                        SwingUtilities.getWindowAncestor(date),"Chọn ngày làm việc",LocalDate.parse(date.getText()),
                        LocalDate.of(1900,1,1),LocalDate.of(9999,12,31));
                if(selected!=null)date.setText(selected.toString());
            }
        });
        JTextArea task=new JTextArea(3,28);task.setLineWrap(true);
        JPanel form=assignmentForm(employee,shiftName,startTime,endTime,datePicker,task);
        Object[] options={"Lưu phân công","Hủy"};
        while(JOptionPane.showOptionDialog(this,form,"Thêm phân công",JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,null,options,options[0])==0){
            try{LocalDate day=LocalDate.parse(date.getText().trim());PhanCongService.range(day,day);String work=task.getText().trim();NhanVienChon nv=(NhanVienChon)employee.getSelectedItem();String name=shiftName.getText().trim();
                java.time.format.DateTimeFormatter hour=java.time.format.DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT);
                LocalTime start=LocalTime.parse(startTime.getText().trim(),hour),end=LocalTime.parse(endTime.getText().trim(),hour);
                if(nv==null||name.isEmpty()||name.length()>50||work.isEmpty()||work.length()>200)
                    throw new IllegalArgumentException("Chọn nhân viên, nhập tên ca 1–50 ký tự và nhiệm vụ 1–200 ký tự.");
                if(!start.isBefore(end))throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu; không hỗ trợ ca qua đêm.");
                write(()->service.createWithNewShift(nv.id(),name,start,end,day,work),"Đã tạo phân công. ",new Runnable(){
                    public void run(){setWeek(day);tuKhoa.setText("");nhanVien.setSelectedItem(nv);}
                });return;
            }catch(DateTimeException e){JOptionPane.showMessageDialog(this,"Giờ không hợp lệ. Nhập theo HH:mm, ví dụ 08:00; ngày làm việc chọn trên lịch.");}catch(IllegalArgumentException e){JOptionPane.showMessageDialog(this,e.getMessage());}
        }
    }
    private static JPanel assignmentForm(JComboBox<NhanVienChon> employee, JTextField name,
                                          JTextField start, JTextField end, JPanel datePicker, JTextArea task) {
        JPanel form=new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8,8,8,8));
        form.setPreferredSize(new Dimension(460,330));
        for(JComponent input:new JComponent[]{employee,name,start,end,datePicker}){
            Dimension size=input.getPreferredSize();input.setPreferredSize(new Dimension(size.width,34));
            input.setMinimumSize(new Dimension(70,34));
        }
        start.setHorizontalAlignment(JTextField.CENTER);end.setHorizontalAlignment(JTextField.CENTER);
        name.setToolTipText("Tên ca mới, tối đa 50 ký tự.");
        start.setToolTipText("Giờ bắt đầu, ví dụ 08:00.");end.setToolTipText("Giờ kết thúc, ví dụ 17:00.");
        task.setLineWrap(true);task.setWrapStyleWord(true);
        JPanel hours=new JPanel(new GridLayout(1,2,12,0));
        hours.add(labeledField("Bắt đầu (HH:mm)",start));hours.add(labeledField("Kết thúc (HH:mm)",end));
        addFormRow(form,0,"Nhân viên",employee,false);
        addFormRow(form,1,"Tên ca",name,false);
        addFormRow(form,2,"Khung giờ",hours,false);
        addFormRow(form,3,"Ngày làm việc",datePicker,false);
        JScrollPane notes=new JScrollPane(task);notes.setPreferredSize(new Dimension(290,88));
        task.setToolTipText("Nhập nhiệm vụ cho nhân viên, tối đa 200 ký tự.");
        addFormRow(form,4,"Nhiệm vụ",notes,true);
        return form;
    }
    private static JPanel labeledField(String text,JComponent control){
        JPanel panel=new JPanel(new BorderLayout(0,4));panel.setOpaque(false);
        JLabel label=new JLabel(text);label.setLabelFor(control);panel.add(label,BorderLayout.NORTH);panel.add(control,BorderLayout.CENTER);
        return panel;
    }
    private static void addFormRow(JPanel form,int row,String text,JComponent control,boolean stretch){
        GridBagConstraints label=new GridBagConstraints();label.gridx=0;label.gridy=row;
        label.anchor=GridBagConstraints.NORTHWEST;label.insets=new Insets(8,0,8,16);
        JLabel caption=new JLabel(text);caption.setLabelFor(control);form.add(caption,label);
        GridBagConstraints input=new GridBagConstraints();input.gridx=1;input.gridy=row;input.weightx=1;
        input.weighty=stretch?1:0;input.fill=GridBagConstraints.BOTH;input.insets=new Insets(4,0,8,0);form.add(control,input);
    }
    private void cancel(){
        ThongTinPhanCong p=selected();if(p==null||busy||!service.canManage())return;
        if(!PhanCongService.canCancelStatus(p.trangThai())){trangThai.setText("Chỉ hủy phân công chờ hiệu lực. Hãy tải lại.");return;}
        String reason=JOptionPane.showInputDialog(this,"Lý do hủy phân công "+p.id()+" (bắt buộc, tối đa 300 ký tự):");if(reason==null)return;
        if(reason.isBlank()||reason.trim().length()>300){trangThai.setText("Lý do hủy phải có 1–300 ký tự.");return;}
        write(()->{service.cancel(p.id(),reason);return p.id();},"Đã hủy phân công, giữ nguyên lịch sử. ");
    }
    private interface Operation{String run()throws Exception;}
    private void write(Operation operation,String success){write(operation,success,null);}
    private void write(Operation operation,String success,Runnable afterSuccess){
        busy=true;buttons();trangThai.setText("Đang lưu phân công…");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground()throws Exception{return operation.run();}
            @Override protected void done(){busy=false;try{String savedId=get();if(afterSuccess!=null){afterSuccess.run();maChonSauTai=savedId;}thongBao=success;JOptionPane.showMessageDialog(LichLamViecPanel.this,success,"Kết quả",JOptionPane.INFORMATION_MESSAGE);refreshData();}catch(Exception e){trangThai.setText(error(e));JOptionPane.showMessageDialog(LichLamViecPanel.this,error(e),"Không thể lưu phân công",JOptionPane.ERROR_MESSAGE);}finally{if(!service.isActive())clear();buttons();}}
        }.execute();
    }
    private static String error(Throwable e){
        while(e instanceof ExecutionException&&e.getCause()!=null)e=e.getCause();
        if(e instanceof AccessDeniedException)return "Bạn không có quyền hoặc phiên đăng nhập đã kết thúc.";
        if(e instanceof IllegalArgumentException)return e.getMessage();
        if(e instanceof java.sql.SQLException sql){
            if(sql.getErrorCode()==229)return "BLOCKED: tài khoản SQL chưa có quyền cho lịch làm việc. Liên hệ quản trị.";
            if(sql.getErrorCode()==207||sql.getErrorCode()==208)return "BLOCKED: cấu trúc database không khớp V1–V5. Liên hệ quản trị.";
            return "Không xác nhận được thao tác SQL. Kiểm tra database cô lập và quyền truy cập; tải lại trước khi thử lại.";
        }
        return "Không tải/lưu được lịch. Hãy tải lại hoặc liên hệ quản trị.";
    }
    private static final class Rows extends AbstractTableModel {
        private List<ThongTinPhanCong> danhSach=List.of();private final String[] titles={"Ngày","Nhân viên","Ca","Bắt đầu","Kết thúc","Nhiệm vụ","Trạng thái"};
        @Override public int getRowCount(){return danhSach.size();}@Override public int getColumnCount(){return titles.length;}@Override public String getColumnName(int col){return titles[col];}
        @Override public Object getValueAt(int row,int col){ThongTinPhanCong p=danhSach.get(row);return switch(col){case 0->p.ngay();case 1->p.tenNhanVien()+" ("+p.nhanVienId()+")";case 2->p.ca().ten();case 3->p.ca().batDau();case 4->p.ca().ketThuc();case 5->p.nhiemVu();default->p.trangThaiHienThi();};}
        @Override public Class<?> getColumnClass(int col){return switch(col){case 0->LocalDate.class;case 3,4->LocalTime.class;default->String.class;};}
    }
}
