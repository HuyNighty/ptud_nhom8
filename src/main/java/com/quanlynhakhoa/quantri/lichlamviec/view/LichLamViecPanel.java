package com.quanlynhakhoa.quantri.lichlamviec.view;

import com.quanlynhakhoa.quantri.lichlamviec.model.*;
import com.quanlynhakhoa.quantri.lichlamviec.service.PhanCongService;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.*;
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
    private final JButton previous=new JButton("‹ Tuần trước"),next=new JButton("Tuần sau ›"),today=new JButton("Tuần này"),refresh=new JButton("Tải lại"),create=new JButton("+ Thêm phân công"),cancel=new JButton("Hủy phân công"),detail=new JButton("Xem chi tiết");
    private final JLabel status=new JLabel("Chưa tải dữ liệu");
    private final Rows model=new Rows();
    private final JTable table=new JTable(model);
    private List<CaLamViec> shifts=List.of();
    private boolean busy,pending;
    private String notice="";
    public LichLamViecPanel(DesktopSession session,boolean management){this(session,management,new PhanCongService(session,TeamDevConnection::open),true);}
    public LichLamViecPanel(DesktopSession session,boolean management,PhanCongService service,boolean autoLoad){
        new AuthorizationService(new RolePermissionPolicy()).require(session,management?Permission.WORKFORCE_READ:Permission.SCHEDULE_SELF_READ);
        this.management=management;this.service=service;
        setLayout(new BorderLayout(0,14));setBackground(new Color(245,248,252));setBorder(new EmptyBorder(24,26,24,26));
        JPanel heading=new JPanel(new BorderLayout(0,8));heading.setOpaque(false);
        JLabel title=new JLabel(management?"Phân công ca":"Lịch làm việc của tôi");title.setFont(new Font("SansSerif",Font.BOLD,23));title.setForeground(new Color(29,61,102));heading.add(title,BorderLayout.NORTH);
        heading.add(new JLabel(management?"Lịch theo ngày · Tạo và hủy phân công, giữ lịch sử":"Lịch cá nhân theo tài khoản đăng nhập · Chỉ xem"),BorderLayout.SOUTH);add(heading,BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(0,10));body.setOpaque(false);
        JPanel tools=new JPanel(new GridLayout(0,1,0,4));tools.setBackground(Color.WHITE);
        JPanel dates=new JPanel(new FlowLayout(FlowLayout.LEFT));dates.setOpaque(false);
        dates.add(previous);dates.add(today);dates.add(next);dates.add(new JLabel("Từ ngày:"));dates.add(from);dates.add(new JLabel("Đến:"));dates.add(to);dates.add(refresh);tools.add(dates);
        setWeek(LocalDate.now());
        if(management){JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT));filters.setOpaque(false);filters.add(new JLabel("Tìm lựa chọn NV:"));filters.add(keyword);filters.add(new JLabel("Lọc bảng:"));employees.setPreferredSize(new Dimension(260,28));employees.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){return super.getListCellRendererComponent(l,v==null?"Tất cả nhân viên":v,i,s,f);}
        });employees.addItem(null);filters.add(employees);filters.add(create);tools.add(filters);
            keyword.setToolTipText("Chỉ tìm trong danh sách lựa chọn nhân viên; không tìm nội dung bảng phân công.");
            employees.setToolTipText("Chọn nhân viên rồi bấm Tải lại để lọc bảng phân công.");
            JLabel hint=new JLabel("Tìm lựa chọn NV: thu hẹp danh sách chọn. Lọc bảng: chọn nhân viên rồi bấm Tải lại (người đang lọc luôn được giữ).");
            hint.setBorder(new EmptyBorder(0,6,4,6));tools.add(hint);}
        body.add(tools,BorderLayout.NORTH);
        table.setRowHeight(32);table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setFillsViewportHeight(true);table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());body.add(scroll,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.setOpaque(false);actions.add(detail);if(management)actions.add(cancel);body.add(actions,BorderLayout.SOUTH);add(body,BorderLayout.CENTER);
        status.setForeground(new Color(62,82,106));add(status,BorderLayout.SOUTH);
        previous.addActionListener(e->move(-7));next.addActionListener(e->move(7));today.addActionListener(e->{setWeek(LocalDate.now());refreshData();});refresh.addActionListener(e->refreshData());keyword.addActionListener(e->refreshData());
        // Selection is an explicit filter, applied by Tải lại. Avoid recursive reload while replacing choices.
        detail.addActionListener(e->showDetail());create.addActionListener(e->create());cancel.addActionListener(e->cancel());
        table.getSelectionModel().addListSelectionListener(e->buttons());buttons();
        if(autoLoad)SwingUtilities.invokeLater(this::refreshData);
    }
    private void setWeek(LocalDate day){LocalDate monday=day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));from.setText(monday.toString());to.setText(monday.plusDays(6).toString());}
    private void move(int days){try{LocalDate f=LocalDate.parse(from.getText().trim()),t=LocalDate.parse(to.getText().trim());from.setText(f.plusDays(days).toString());to.setText(t.plusDays(days).toString());refreshData();}catch(DateTimeException e){status.setText("Ngày không hợp lệ. Dùng yyyy-MM-dd.");}}
    private PhanCong selected(){int row=table.getSelectedRow();return row<0?null:model.rows.get(table.convertRowIndexToModel(row));}
    private void buttons(){
        boolean active=service.isActive(),write=management&&service.canManage();
        for(Component c:new Component[]{from,to,keyword,employees,previous,next,today,refresh,table})c.setEnabled(!busy&&active);
        create.setEnabled(!busy&&active&&write&&!shifts.isEmpty()&&employees.getItemCount()>1);
        PhanCong p=selected();detail.setEnabled(!busy&&active&&p!=null);cancel.setEnabled(!busy&&write&&p!=null&&PhanCongService.canCancelStatus(p.trangThai()));
    }
    private void clear(){model.rows=List.of();model.fireTableDataChanged();shifts=List.of();employees.removeAllItems();employees.addItem(null);}
    @Override public void refreshData(){
        if(busy){pending=true;return;}if(!service.isActive()){clear();status.setText("Phiên đăng nhập đã kết thúc.");buttons();return;}
        try{
            LocalDate f=LocalDate.parse(from.getText().trim()),t=LocalDate.parse(to.getText().trim());PhanCongService.range(f,t);
            String search=keyword.getText();NhanVienChon chosen=(NhanVienChon)employees.getSelectedItem();String id=chosen==null?null:chosen.id();
            busy=true;buttons();status.setText("Đang tải lịch làm việc…");
            new SwingWorker<PhanCongService.DuLieu,Void>(){
                @Override protected PhanCongService.DuLieu doInBackground() throws Exception{return management?service.management(f,t,search,id):service.mine(f,t);}
                @Override protected void done(){
                    busy=false;
                    try{
                        var data=get();if(!service.isActive()){clear();status.setText("Phiên đăng nhập đã kết thúc.");return;}
                        model.rows=data.rows();model.fireTableDataChanged();shifts=data.shifts();
                        if(management){employees.removeAllItems();employees.addItem(null);for(var employee:data.employees())employees.addItem(employee);if(chosen!=null){if(!data.employees().contains(chosen))employees.addItem(chosen);employees.setSelectedItem(chosen);}}
                        String message=!data.message().isEmpty()?data.message():data.rows().isEmpty()?"Không có phân công trong khoảng ngày đã chọn.":data.rows().size()+" phân công (bao gồm lịch sử hủy).";
                        status.setText(notice+message);notice="";
                    }catch(Exception e){clear();status.setText(error(e));}
                    finally{buttons();if(pending){pending=false;refreshData();}}
                }
            }.execute();
        }catch(DateTimeException|IllegalArgumentException e){status.setText("Khoảng ngày không hợp lệ. Dùng yyyy-MM-dd.");}
    }
    private void showDetail(){PhanCong p=selected();if(p==null)return;JTextArea text=new JTextArea("Mã: "+p.id()+"\nNhân viên: "+p.tenNhanVien()+" ("+p.nhanVienId()+")\nNgày: "+p.ngay()+"\nCa: "+p.ca()+"\nNhiệm vụ: "+p.nhiemVu()+"\nTrạng thái: "+p.trangThaiHienThi()+"\nLý do hủy: "+(p.lyDoHuy()==null?"—":p.lyDoHuy()),10,48);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);JOptionPane.showMessageDialog(this,new JScrollPane(text),"Chi tiết phân công",JOptionPane.INFORMATION_MESSAGE);}
    private void create(){
        if(busy||!service.canManage())return;
        JComboBox<NhanVienChon> employee=new JComboBox<>();for(int i=1;i<employees.getItemCount();i++)employee.addItem(employees.getItemAt(i));
        if(employees.getSelectedItem()!=null)employee.setSelectedItem(employees.getSelectedItem());
        JComboBox<CaLamViec> shift=new JComboBox<>(shifts.toArray(CaLamViec[]::new));JTextField date=new JTextField(from.getText());JTextArea task=new JTextArea(3,28);task.setLineWrap(true);
        JPanel form=new JPanel(new GridLayout(0,1,4,4));form.add(new JLabel("Nhân viên"));form.add(employee);form.add(new JLabel("Ca làm việc"));form.add(shift);form.add(new JLabel("Ngày (yyyy-MM-dd)"));form.add(date);form.add(new JLabel("Nhiệm vụ (tối đa 200 ký tự)"));form.add(new JScrollPane(task));
        while(JOptionPane.showConfirmDialog(this,form,"Thêm phân công",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try{LocalDate day=LocalDate.parse(date.getText().trim());PhanCongService.range(day,day);String work=task.getText().trim();NhanVienChon nv=(NhanVienChon)employee.getSelectedItem();CaLamViec ca=(CaLamViec)shift.getSelectedItem();
                if(nv==null||ca==null||work.isEmpty()||work.length()>200)throw new IllegalArgumentException("Chọn nhân viên, ca và nhập nhiệm vụ 1–200 ký tự.");
                write(()->service.create(nv.id(),ca.id(),day,work),"Đã tạo phân công. ");return;
            }catch(DateTimeException e){JOptionPane.showMessageDialog(this,"Ngày không hợp lệ. Dùng yyyy-MM-dd.");}catch(IllegalArgumentException e){JOptionPane.showMessageDialog(this,e.getMessage());}
        }
    }
    private void cancel(){
        PhanCong p=selected();if(p==null||busy||!service.canManage())return;
        if(!PhanCongService.canCancelStatus(p.trangThai())){status.setText("Chỉ hủy phân công chờ hiệu lực. Hãy tải lại.");return;}
        String reason=JOptionPane.showInputDialog(this,"Lý do hủy phân công "+p.id()+" (bắt buộc, tối đa 300 ký tự):");if(reason==null)return;
        if(reason.isBlank()||reason.trim().length()>300){status.setText("Lý do hủy phải có 1–300 ký tự.");return;}
        write(()->{service.cancel(p.id(),reason);return p.id();},"Đã hủy phân công, giữ nguyên lịch sử. ");
    }
    private interface Operation{String run()throws Exception;}
    private void write(Operation operation,String success){
        busy=true;buttons();status.setText("Đang lưu phân công…");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground()throws Exception{return operation.run();}
            @Override protected void done(){busy=false;try{get();notice=success;refreshData();}catch(Exception e){status.setText(error(e));}finally{if(!service.isActive())clear();buttons();}}
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
        private List<PhanCong> rows=List.of();private final String[] titles={"Ngày","Nhân viên","Ca","Bắt đầu","Kết thúc","Nhiệm vụ","Trạng thái"};
        @Override public int getRowCount(){return rows.size();}@Override public int getColumnCount(){return titles.length;}@Override public String getColumnName(int col){return titles[col];}
        @Override public Object getValueAt(int row,int col){PhanCong p=rows.get(row);return switch(col){case 0->p.ngay();case 1->p.tenNhanVien()+" ("+p.nhanVienId()+")";case 2->p.ca().ten();case 3->p.ca().batDau();case 4->p.ca().ketThuc();case 5->p.nhiemVu();default->p.trangThaiHienThi();};}
        @Override public Class<?> getColumnClass(int col){return switch(col){case 0->LocalDate.class;case 3,4->LocalTime.class;default->String.class;};}
    }
}
