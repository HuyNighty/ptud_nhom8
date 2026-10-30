package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.model.TrangThaiHoaDon;
import com.quanlynhakhoa.billing.service.CancellationService;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import javax.swing.JOptionPane;

public final class CancellationRequestController {
    private final CancellationRequestDialog view;private final DesktopSession session;private final CancellationService service;
    private final String invoiceId;private final Runnable saved;private boolean busy,maySubmit;
    public CancellationRequestController(CancellationRequestDialog view,DesktopSession session,CancellationService service,String invoiceId,Runnable saved){
        this.view=view;this.session=session;this.service=service;this.invoiceId=invoiceId;this.saved=saved;
        view.btnGuiYeuCau.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){send();}});
        view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});load();
    }
    private abstract class Task<T> extends SessionWorker<T>{
        Task(){super(CancellationRequestController.this.session);}
        protected void finished(){busy=false;view.busy(false,maySubmit);}
        protected void expired(){view.dispose();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(BillingUi.error(failure));}
    }
    private void load(){
        if(busy)return;busy=true;maySubmit=false;view.busy(true,false);
        new Task<CancellationService.Preview>(){
            protected CancellationService.Preview doInBackground()throws Exception{return service.preview(invoiceId);}
            protected void succeeded(CancellationService.Preview preview){maySubmit=!preview.pending()&&preview.invoice().hoaDon().trangThai()!=TrangThaiHoaDon.DA_HUY;view.showPreview(preview);view.busy(false,maySubmit);}
        }.execute();
    }
    private void send(){
        if(busy||!maySubmit)return;final String reason=view.txaLyDo.getText().trim();
        if(reason.isEmpty()||reason.length()>500){view.lblTrangThai.setText("Lý do bắt buộc, tối đa 500 ký tự.");return;}
        if(JOptionPane.showConfirmDialog(view,"Gửi yêu cầu hủy hóa đơn "+invoiceId+"? Hóa đơn chỉ bị hủy sau khi quản lý duyệt.","Xác nhận",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        busy=true;maySubmit=false;view.busy(true,false);
        new Task<String>(){
            protected String doInBackground()throws Exception{return service.request(invoiceId,reason);}
            protected void succeeded(String id){view.lblTrangThai.setText("Đã gửi yêu cầu "+id+". Hóa đơn chưa bị hủy.");saved.run();view.busy(false,false);}
            protected void failed(Throwable failure){if(failure instanceof CancellationService.CommittedCancellationException committed){succeeded(committed.id());return;}view.lblTrangThai.setText(BillingUi.error(failure)+" Kiểm tra/Tải lại trước khi gửi tiếp; không tự thử lại.");view.busy(false,false);}
        }.execute();
    }
}
