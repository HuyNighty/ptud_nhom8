package com.quanlynhakhoa.controller.hoadon;

import com.quanlynhakhoa.view.hoadon.ThanhToanPanel;
import com.quanlynhakhoa.view.hoadon.ThanhToanForm;
import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.service.hoadon.HoaDonService;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.LuaChonLapHoaDon;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.util.List;

public final class ManHinhThanhToanController {
    private final ThanhToanPanel view;
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final HoaDonService hoaDon;
    private ThanhToanController thuTien;
    private ThanhToanForm form;
    private String maHoaDon;
    private boolean searching,updating,searchPending;
    private final Runnable invalidated;
    public ManHinhThanhToanController(ThanhToanPanel view,DesktopSession session,ConnectionProvider connections){
        this.view=view;this.session=session;this.connections=connections;hoaDon=new HoaDonService(session,connections);
        view.btnTim.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){userSearch();}});
        view.txtTimHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){userSearch();}});
        view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(locked()||searching)return;view.txtTimHoaDon.setText("");userSearch();}});
        view.cmbHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updating&&view.cmbHoaDon.getSelectedItem() instanceof LuaChonLapHoaDon choice)openInvoice(choice.id());}});
        invalidated=new Runnable(){public void run(){refresh();}};ThongBaoThayDoiHoaDon.forSession(session).subscribe(invalidated);
    }
    private boolean locked(){return form!=null&&(form.isWorking()||form.hasUncertainResult());}
    private void updatePicker(){view.pickerEnabled(!searching&&!locked());}
    private void userSearch(){
        if(locked()||searching)return;
        // A fresh search must not leave an unrelated invoice ready to collect.
        thuTien=null;maHoaDon=null;form=null;updating=true;view.showChoices(List.of(),null);updating=false;view.showForm(new ThanhToanForm());search();
    }
    public void openInvoice(String id){
        if(!session.isActive()){view.expired();return;}
        if(locked()){view.lblTrangThai.setText("Hoàn tất hoặc kiểm tra lần thu hiện tại trước khi đổi hóa đơn.");return;}
        if(id.equals(maHoaDon)&&thuTien!=null){thuTien.refresh();return;}
        maHoaDon=id;view.txtTimHoaDon.setText(id);updating=true;
        for(int index=0;index<view.cmbHoaDon.getItemCount();index++)if(view.cmbHoaDon.getItemAt(index).id().equals(id)){view.cmbHoaDon.setSelectedIndex(index);break;}
        updating=false;form=new ThanhToanForm();form.onStateChange(new Runnable(){public void run(){updatePicker();}});view.showForm(form);
        thuTien=new ThanhToanController(form,session,connections,id,new Runnable(){public void run(){view.lblTrangThai.setText("Đã thu tiền thành công. Lịch sử và công nợ đang cập nhật.");search();}});
        search();updatePicker();
    }
    public void refresh(){if(!session.isActive()){view.expired();return;}search();if(thuTien!=null)thuTien.refresh();}
    private void search(){
        if(searching){searchPending=true;return;}if(!session.isActive()){view.expired();return;}
        searching=true;if(form!=null)form.setCollectionContextAllowed(false);updatePicker();view.lblTrangThai.setText("Đang tìm hóa đơn còn nợ…");final String keyword=view.txtTimHoaDon.getText();
        new SessionWorker<List<ThongTinHoaDon>>(session){
            protected List<ThongTinHoaDon> doInBackground()throws Exception{return hoaDon.outstandingChoices(keyword);}
            protected void finished(){searching=false;updatePicker();}
            protected void expired(){view.expired();}
            protected void succeeded(List<ThongTinHoaDon> found){
                if(searchPending||!keyword.equals(view.txtTimHoaDon.getText())){searchPending=false;search();return;}
                boolean limited=found.size()>100;if(limited)found=found.subList(0,100);
                updating=true;view.showChoices(found,maHoaDon);updating=false;
                boolean matches=false;for(ThongTinHoaDon invoice:found)if(invoice.maHoaDon().equals(maHoaDon))matches=true;
                if(form!=null)form.setCollectionContextAllowed(matches);
                String message=found.isEmpty()?(keyword.isBlank()?"Chưa có hóa đơn còn phải thu.":"Không tìm thấy hóa đơn còn nợ phù hợp."):"";
                if(maHoaDon!=null)message=matches?"Đang xem hóa đơn "+maHoaDon+".":"Tra cứu hóa đơn "+maHoaDon+" · không thuộc kết quả còn nợ hiện tại.";
                view.lblTrangThai.setText(limited?message+" · Hiển thị 100 hóa đơn. Nhập mã hoặc từ khóa để tìm cụ thể.":message);
            }
            protected void failed(Throwable failure){view.lblTrangThai.setText("Tìm hóa đơn thất bại. "+GiaoDienHoaDon.error(failure));if(searchPending){searchPending=false;search();}}
        }.execute();
    }
}
