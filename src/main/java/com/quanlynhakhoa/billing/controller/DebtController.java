package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.event.*;

public final class DebtController {
    private final DebtPanel view;private final DesktopSession session;private final HoaDonService invoices;private boolean busy,reloadPending;
    private final Runnable invalidated;
    public DebtController(DebtPanel view,DesktopSession session,ConnectionProvider connections){
        this.view=view;this.session=session;invoices=new HoaDonService(session,connections);
        view.btnTim.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});view.txtTuKhoa.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});view.btnThuNo.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!busy)view.paySelected();}});
        view.tblBenhNhan.getSelectionModel().addListSelectionListener(new ListSelectionListener(){public void valueChanged(ListSelectionEvent e){if(!busy&&!e.getValueIsAdjusting()){busy=true;view.selectPatient();busy=false;}}});
        view.tblHoaDon.getSelectionModel().addListSelectionListener(new ListSelectionListener(){public void valueChanged(ListSelectionEvent e){if(!busy&&!e.getValueIsAdjusting())detail();}});
        invalidated=new Runnable(){public void run(){load();}};BillingChangeBus.forSession(session).subscribe(invalidated);
    }
    private abstract class Task<T> extends SessionWorker<T>{
        Task(){super(DebtController.this.session);}
        protected void finished(){busy=false;view.busy(false);}
        protected void expired(){view.expired();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(BillingUi.error(failure));if(reloadPending){reloadPending=false;load();}}
        protected void next(){if(reloadPending){reloadPending=false;load();}}
    }
    public void load(){
        if(busy){reloadPending=true;return;}if(!session.isActive()){view.expired();return;}
        final String keyword=view.txtTuKhoa.getText();final LocalDate from,to;
        try{from=date(view.txtTuNgay.getText());to=date(view.txtDenNgay.getText());}catch(RuntimeException e){view.lblTrangThai.setText("Ngày phải có dạng yyyy-MM-dd và tồn tại.");return;}
        busy=true;view.busy(true);
        new Task<List<PatientDebt>>(){
            protected List<PatientDebt> doInBackground()throws Exception{return DebtService.byPatient(invoices.find(keyword,from,to,null));}
            protected void succeeded(List<PatientDebt> rows){busy=true;view.showPatients(rows);busy=false;view.lblTrangThai.setText(rows.isEmpty()?"Không có khoản còn phải thu phù hợp.":"Chọn bệnh nhân rồi chọn hóa đơn để xem chi tiết hoặc thu nợ.");next();}
        }.execute();
    }
    private void detail(){final String id=view.selectedInvoice();if(id==null)return;busy=true;view.busy(true);new Task<ChiTietBilling>(){
        protected ChiTietBilling doInBackground()throws Exception{return invoices.detail(id);}
        protected void succeeded(ChiTietBilling value){if(id.equals(view.selectedInvoice()))view.showDetail(value);next();}
    }.execute();}
    private static LocalDate date(String text){return text==null||text.isBlank()?null:LocalDate.parse(text.trim());}
}
