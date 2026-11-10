package com.quanlynhakhoa.controller.hoadon;

import com.quanlynhakhoa.view.hoadon.YeuCauHuyHoaDonDialog;
import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.model.hoadon.XemTruocHuyHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.service.hoadon.HuyHoaDonService;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import javax.swing.JOptionPane;

public final class YeuCauHuyHoaDonController {
    private final YeuCauHuyHoaDonDialog view;private final DesktopSession session;private final HuyHoaDonService service;
    private final String invoiceId;private final Runnable saved;private boolean busy,maySubmit;
    public YeuCauHuyHoaDonController(YeuCauHuyHoaDonDialog view,DesktopSession session,HuyHoaDonService service,String invoiceId,Runnable saved){
        this.view=view;this.session=session;this.service=service;this.invoiceId=invoiceId;this.saved=saved;
        view.btnGuiYeuCau.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){send();}});
        view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});load();
    }
    private abstract class Task<T> extends SessionWorker<T>{
        Task(){super(YeuCauHuyHoaDonController.this.session);}
        protected void finished(){busy=false;view.busy(false,maySubmit);}
        protected void expired(){view.dispose();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(GiaoDienHoaDon.error(failure));}
    }
    private void load(){
        if(busy)return;busy=true;maySubmit=false;view.busy(true,false);
        new Task<XemTruocHuyHoaDon>(){
            protected XemTruocHuyHoaDon doInBackground()throws Exception{return service.preview(invoiceId);}
            protected void succeeded(XemTruocHuyHoaDon preview){maySubmit=!preview.pending()&&preview.invoice().hoaDon().trangThai()!=TrangThaiHoaDon.DA_HUY&&preview.invoice().hoaDon().daThu().signum()==0;view.showPreview(preview);view.busy(false,maySubmit);}
        }.execute();
    }
    private void send(){
        if(busy||!maySubmit)return;final String reason=view.txaLyDo.getText().trim();
        if(reason.isEmpty()||reason.length()>500){view.lblTrangThai.setText("Lý do bắt buộc, tối đa 500 ký tự.");return;}
        if(JOptionPane.showConfirmDialog(view,"Gửi yêu cầu hủy hóa đơn "+invoiceId+"? Hóa đơn chỉ bị hủy sau khi quản lý duyệt.","Xác nhận",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        busy=true;maySubmit=false;view.busy(true,false);
        new Task<String>(){
            protected String doInBackground()throws Exception{return service.request(invoiceId,reason);}
            protected void succeeded(String id){GiaoDienHoaDon.result(view,"Đã gửi yêu cầu hủy "+id+". Hóa đơn chờ người có quyền xét duyệt.");view.lblTrangThai.setText("Đã gửi yêu cầu "+id+". Hóa đơn chưa bị hủy.");saved.run();view.busy(false,false);ThongBaoThayDoiHoaDon.forSession(session).publish();}
            protected void failed(Throwable failure){if(failure instanceof HuyHoaDonService.CommittedCancellationException committed){succeeded(committed.id());return;}GiaoDienHoaDon.result(view,GiaoDienHoaDon.error(failure)+" Kiểm tra kết quả trước khi gửi lại.","Chưa xác nhận gửi yêu cầu",javax.swing.JOptionPane.ERROR_MESSAGE);view.lblTrangThai.setText(GiaoDienHoaDon.error(failure)+" Kiểm tra/Làm mới trước khi gửi tiếp; không tự thử lại.");view.busy(false,false);}
        }.execute();
    }
}
