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
import com.quanlynhakhoa.hethong.tinhnang.RefreshableFeature;
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
    private final boolean management;
    private final JTextField from=new JTextField(10),to=new JTextField(10),keyword=new JTextField(14);
    private final JComboBox<NhanVienChon> employees=new JComboBox<>();
    private final JButton previous=new JButton("‹ Tuần trước"),next=new JButton("Tuần sau ›"),today=new JButton("Tuần này"),refresh=new JButton("Làm mới"),create=new JButton("+ Thêm phân công"),cancel=new JButton("Hủy phân công"),detail=new JButton("Xem chi tiết");
    private final JLabel status=new JLabel("Chưa tải dữ liệu");
    private final Rows model=new Rows();
    private final JTable table=new JTable(model);
    private List<CaLamViec> shifts=List.of();
    private boolean busy,pending;
    private String notice="";
    private String selectionAfterLoad;
    public LichLamViecPanel(DesktopSession session,boolean management){this(session,management,new PhanCongService(session,TeamDevConnection::open),true);}
    public LichLamViecPanel(DesktopSession session,boolean management,PhanCongService service,boolean autoLoad){
        new AuthorizationService(new RolePermissionPolicy()).require(session,management?Quyen.WORKFORCE_READ:Quyen.SCHEDULE_SELF_READ);
        this.management=management;this.service=service;
        setLayout(new BorderLayout(0,14));setBackground(new Color(245,248,252));setBorder(new EmptyBorder(24,26,24,26));
        JPanel heading=new JPanel(new BorderLayout(0,8));heading.setOpaque(false);
        JLabel title=new JLabel(management?"Phân công ca":"Lịch làm việc của tôi");title.setFont(new Font("SansSerif",Font.BOLD,23));title.setForeground(new Color(29,61,102));heading.add(title,BorderLayout.NORTH);
        add(heading,BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(0,10));body.setOpaque(false);
        JPanel tools=new JPanel();tools.setLayout(new BoxLayout(tools,BoxLayout.Y_AXIS));tools.setBackground(Color.WHITE);tools.setBorder(new EmptyBorder(10,12,10,12));
        JPanel dates=new JPanel(new FlowLayout(FlowLayout.LEFT));dates.setOpaque(false);
        dates.add(previous);dates.add(today);dates.add(next);dates.add(new JLabel("Từ ngày:"));dates.add(com.quanlynhakhoa.hethong.giaodien.DateInput.iso(from,"ngày bắt đầu"));dates.add(new JLabel("Đến:"));dates.add(com.quanlynhakhoa.hethong.giaodien.DateInput.iso(to,"ngày kết thúc"));dates.add(refresh);dates.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(dates);
        setWeek(LocalDate.now());
        if(management){JPanel filters=new JPanel(new GridBagLayout());filters.setOpaque(false);employees.setPreferredSize(new Dimension(260,34));employees.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){return super.getListCellRendererComponent(l,v==null?"Tất cả nhân viên":v,i,s,f);}
        });employees.addItem(null);
            GridBagConstraints filterCell=new GridBagConstraints();filterCell.gridy=0;filterCell.fill=GridBagConstraints.HORIZONTAL;filterCell.anchor=GridBagConstraints.SOUTH;filterCell.insets=new Insets(6,4,4,8);
            filterCell.gridx=0;filterCell.weightx=.35;filters.add(labeledField("Tìm nhân viên",keyword),filterCell);
            filterCell.gridx=1;filterCell.weightx=.65;filters.add(labeledField("Nhân viên hiển thị",employees),filterCell);
            filterCell.gridx=2;filterCell.weightx=0;filters.add(create,filterCell);
            filters.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(filters);
            keyword.setToolTipText("Chỉ tìm trong danh sách lựa chọn nhân viên; không tìm nội dung bảng phân công.");
            employees.setToolTipText("Chọn nhân viên rồi nhấn Enter ở ô tìm để lọc bảng phân công.");
            JLabel hint=new JLabel("Tìm lựa chọn NV: thu hẹp danh sách chọn. Lọc bảng: chọn nhân viên rồi bấm Làm mới (người đang lọc luôn được giữ).");
            hint.setBorder(new EmptyBorder(4,4,0,0));hint.setText("Chọn nhân viên rồi nhấn Enter ở ô tìm để xem lịch. Làm mới sẽ bỏ lọc nhân viên.");hint.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(hint);}
        body.add(tools,BorderLayout.NORTH);
        table.setRowHeight(32);table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setFillsViewportHeight(true);table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());body.add(scroll,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.setOpaque(false);actions.add(detail);if(management)actions.add(cancel);body.add(actions,BorderLayout.SOUTH);add(body,BorderLayout.CENTER);
        status.setForeground(new Color(62,82,106));add(status,BorderLayout.SOUTH);
        previous.addActionListener(e->move(-7));next.addActionListener(e->move(7));today.addActionListener(e->{setWeek(LocalDate.now());refreshData();});refresh.addActionListener(e->{keyword.setText("");if(management&&employees.getItemCount()>0)employees.setSelectedIndex(0);refreshData();});keyword.addActionListener(e->refreshData());
        // Selection is an explicit filter, applied by Tải lại. Avoid recursive reload while replacing choices.
        detail.addActionListener(e->showDetail());create.addActionListener(e->create());cancel.addActionListener(e->cancel());
        for(JComponent control:new JComponent[]{from,to,keyword,employees,previous,next,today,refresh,create,cancel,detail}){
            Dimension preferred=control.getPreferredSize();control.setPreferredSize(new Dimension(preferred.width,34));
            if(control instanceof JButton button){button.setMargin(new Insets(6,12,6,12));button.setFocusPainted(false);}
        }
        table.getSelectionModel().addListSelectionListener(e->buttons());buttons();
        if(autoLoad)SwingUtilities.invokeLater(this::refreshData);
    }
    private void setWeek(LocalDate day){LocalDate monday=day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));from.setText(monday.toString());to.setText(monday.plusDays(6).toString());}
    private void move(int days){try{LocalDate f=LocalDate.parse(from.getText().trim()),t=LocalDate.parse(to.getText().trim());from.setText(f.plusDays(days).toString());to.setText(t.plusDays(days).toString());refreshData();}catch(DateTimeException e){status.setText("Ngày không hợp lệ. Dùng yyyy-MM-dd.");}}
    private ThongTinPhanCong selected(){int row=table.getSelectedRow();return row<0?null:model.rows.get(table.convertRowIndexToModel(row));}
    private void buttons(){
        boolean active=service.isActive(),write=management&&service.canManage();
        for(Component c:new Component[]{from,to,keyword,employees,previous,next,today,refresh,table})c.setEnabled(!busy&&active);
        create.setEnabled(!busy&&active&&write&&employees.getItemCount()>1);
        ThongTinPhanCong p=selected();detail.setEnabled(!busy&&active&&p!=null);cancel.setEnabled(!busy&&write&&p!=null&&PhanCongService.canCancelStatus(p.trangThai()));
    }
    private void clear(){model.rows=List.of();model.fireTableDataChanged();shifts=List.of();employees.removeAllItems();employees.addItem(null);}
    @Override public void refreshData(){
        if(busy){pending=true;return;}if(!service.isActive()){clear();status.setText("Phiên đăng nhập đã kết thúc.");buttons();return;}
        try{
            LocalDate f=LocalDate.parse(from.getText().trim()),t=LocalDate.parse(to.getText().trim());PhanCongService.range(f,t);
            String search=keyword.getText();NhanVienChon chosen=(NhanVienChon)employees.getSelectedItem();String id=chosen==null?null:chosen.id();
            busy=true;buttons();status.setText("Đang tải lịch làm việc…");
            new SwingWorker<DuLieuPhanCong,Void>(){
                @Override protected DuLieuPhanCong doInBackground() throws Exception{return management?service.management(f,t,search,id):service.mine(f,t);}
                @Override protected void done(){
                    busy=false;
                    try{
                        var data=get();if(!service.isActive()){clear();status.setText("Phiên đăng nhập đã kết thúc.");return;}
                        model.rows=data.rows();model.fireTableDataChanged();shifts=data.shifts();
                        if(management){employees.removeAllItems();employees.addItem(null);for(var employee:data.employees())employees.addItem(employee);if(chosen!=null){if(!data.employees().contains(chosen))employees.addItem(chosen);employees.setSelectedItem(chosen);}}
                        String message=!data.message().isEmpty()?data.message():data.rows().isEmpty()?"Không có phân công trong khoảng ngày đã chọn.":data.rows().size()+" phân công (bao gồm lịch sử hủy).";
                        if(management && data.shifts().isEmpty()) message += " Chưa có ca làm việc để phân công.";
                        if(management && data.employees().isEmpty()) message += " Không tìm thấy nhân viên đang làm việc; xóa từ khóa để tải lại.";
                        status.setText(notice+message);notice="";
                        if(selectionAfterLoad!=null){
                            for(int index=0;index<model.rows.size();index++)if(selectionAfterLoad.equals(model.rows.get(index).id())){
                                int visible=table.convertRowIndexToView(index);
                                if(visible>=0){table.setRowSelectionInterval(visible,visible);table.scrollRectToVisible(table.getCellRect(visible,0,true));}
                                break;
                            }
                            selectionAfterLoad=null;
                        }
                    }catch(Exception e){clear();status.setText(error(e));}
                    finally{buttons();if(pending){pending=false;refreshData();}}
                }
            }.execute();
        }catch(DateTimeException|IllegalArgumentException e){status.setText("Khoảng ngày không hợp lệ. Dùng yyyy-MM-dd.");}
    }
    private void showDetail(){ThongTinPhanCong p=selected();if(p==null)return;JTextArea text=new JTextArea("Mã: "+p.id()+"\nNhân viên: "+p.tenNhanVien()+" ("+p.nhanVienId()+")\nNgày: "+p.ngay()+"\nCa: "+p.ca()+"\nNhiệm vụ: "+p.nhiemVu()+"\nTrạng thái: "+p.trangThaiHienThi()+"\nLý do hủy: "+(p.lyDoHuy()==null?"—":p.lyDoHuy()),10,48);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);JOptionPane.showMessageDialog(this,new JScrollPane(text),"Chi tiết phân công",JOptionPane.INFORMATION_MESSAGE);}
    private void create(){
        if(busy||!service.canManage())return;
        JComboBox<NhanVienChon> employee=new JComboBox<>();for(int i=1;i<employees.getItemCount();i++)employee.addItem(employees.getItemAt(i));
        if(employees.getSelectedItem()!=null)employee.setSelectedItem(employees.getSelectedItem());
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
                    public void run(){setWeek(day);keyword.setText("");employees.setSelectedItem(nv);}
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
        if(!PhanCongService.canCancelStatus(p.trangThai())){status.setText("Chỉ hủy phân công chờ hiệu lực. Hãy tải lại.");return;}
        String reason=JOptionPane.showInputDialog(this,"Lý do hủy phân công "+p.id()+" (bắt buộc, tối đa 300 ký tự):");if(reason==null)return;
        if(reason.isBlank()||reason.trim().length()>300){status.setText("Lý do hủy phải có 1–300 ký tự.");return;}
        write(()->{service.cancel(p.id(),reason);return p.id();},"Đã hủy phân công, giữ nguyên lịch sử. ");
    }
    private interface Operation{String run()throws Exception;}
    private void write(Operation operation,String success){write(operation,success,null);}
    private void write(Operation operation,String success,Runnable afterSuccess){
        busy=true;buttons();status.setText("Đang lưu phân công…");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground()throws Exception{return operation.run();}
            @Override protected void done(){busy=false;try{String savedId=get();if(afterSuccess!=null){afterSuccess.run();selectionAfterLoad=savedId;}notice=success;JOptionPane.showMessageDialog(LichLamViecPanel.this,success,"Kết quả",JOptionPane.INFORMATION_MESSAGE);refreshData();}catch(Exception e){status.setText(error(e));JOptionPane.showMessageDialog(LichLamViecPanel.this,error(e),"Không thể lưu phân công",JOptionPane.ERROR_MESSAGE);}finally{if(!service.isActive())clear();buttons();}}
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
        private List<ThongTinPhanCong> rows=List.of();private final String[] titles={"Ngày","Nhân viên","Ca","Bắt đầu","Kết thúc","Nhiệm vụ","Trạng thái"};
        @Override public int getRowCount(){return rows.size();}@Override public int getColumnCount(){return titles.length;}@Override public String getColumnName(int col){return titles[col];}
        @Override public Object getValueAt(int row,int col){ThongTinPhanCong p=rows.get(row);return switch(col){case 0->p.ngay();case 1->p.tenNhanVien()+" ("+p.nhanVienId()+")";case 2->p.ca().ten();case 3->p.ca().batDau();case 4->p.ca().ketThuc();case 5->p.nhiemVu();default->p.trangThaiHienThi();};}
        @Override public Class<?> getColumnClass(int col){return switch(col){case 0->LocalDate.class;case 3,4->LocalTime.class;default->String.class;};}
    }
}
