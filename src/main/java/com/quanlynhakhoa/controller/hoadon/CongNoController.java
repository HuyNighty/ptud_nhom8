package com.quanlynhakhoa.controller.hoadon;

import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.view.hoadon.CongNoPanel;
import com.quanlynhakhoa.service.hoadon.HoaDonService;
import com.quanlynhakhoa.service.hoadon.CongNoService;
import com.quanlynhakhoa.model.hoadon.TongHopCongNoBenhNhan;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.event.*;

public final class CongNoController {
    private final CongNoPanel view;private final DesktopSession session;private final HoaDonService invoices;private boolean busy,reloadPending;
    private final Runnable invalidated;
    public CongNoController(CongNoPanel view,DesktopSession session,ConnectionProvider connections){
        this.view=view;this.session=session;invoices=new HoaDonService(session,connections);
        view.btnTim.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(busy)return;view.txtTuKhoa.setText("");view.txtTuNgay.setText("");view.txtDenNgay.setText("");load();}});view.txtTuKhoa.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});view.btnThuNo.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!busy)view.paySelected();}});
        view.tblBenhNhan.getSelectionModel().addListSelectionListener(new ListSelectionListener(){public void valueChanged(ListSelectionEvent e){if(!busy&&!e.getValueIsAdjusting()){busy=true;view.selectPatient();busy=false;}}});
        view.tblHoaDon.getSelectionModel().addListSelectionListener(new ListSelectionListener(){public void valueChanged(ListSelectionEvent e){if(!busy&&!e.getValueIsAdjusting())detail();}});
        invalidated=new Runnable(){public void run(){load();}};ThongBaoThayDoiHoaDon.forSession(session).subscribe(invalidated);
    }
    private abstract class Task<T> extends SessionWorker<T>{
        Task(){super(CongNoController.this.session);}
        protected void finished(){busy=false;view.busy(false);}
        protected void expired(){view.expired();}
        protected void failed(Throwable failure){view.failed(GiaoDienHoaDon.error(failure));if(reloadPending){reloadPending=false;load();}}
        protected void next(){if(reloadPending){reloadPending=false;load();}}
    }
    public void load(){
        if(busy){reloadPending=true;return;}if(!session.isActive()){view.expired();return;}
        final String keyword=view.txtTuKhoa.getText();final LocalDate from,to;
        try{from=date(view.txtTuNgay.getText());to=date(view.txtDenNgay.getText());}catch(RuntimeException e){view.lblTrangThai.setText("Ngày phải có dạng dd/MM/yyyy và tồn tại.");return;}
        busy=true;view.busy(true);view.loading();
        new Task<List<TongHopCongNoBenhNhan>>(){
            protected List<TongHopCongNoBenhNhan> doInBackground()throws Exception{return CongNoService.byPatient(invoices.find(keyword,from,to,null));}
            protected void succeeded(List<TongHopCongNoBenhNhan> rows){busy=true;view.showPatients(rows);busy=false;view.lblTrangThai.setText(rows.isEmpty()?"Tổng hợp theo bộ lọc ngày lập hóa đơn hiện tại.":" ");next();}
        }.execute();
    }
    private void detail(){final String id=view.selectedInvoice();if(id==null)return;busy=true;view.busy(true);new Task<DuLieuChiTietHoaDon>(){
        protected DuLieuChiTietHoaDon doInBackground()throws Exception{return invoices.detail(id);}
        protected void succeeded(DuLieuChiTietHoaDon value){if(id.equals(view.selectedInvoice()))view.showDetail(value);next();}
    }.execute();}
    private static LocalDate date(String text){return GiaoDienHoaDon.parseDate(text);}
}
