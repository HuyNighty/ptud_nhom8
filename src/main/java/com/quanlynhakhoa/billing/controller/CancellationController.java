package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.CancellationService;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.event.*;

public final class CancellationController {
    private final CancellationPanel view;private final DesktopSession session;private final CancellationService service;
    private boolean busy,reloadPending;private CancellationService.Detail selected;private String writeMessage="";
    private final Runnable invalidated;
    public CancellationController(CancellationPanel view,DesktopSession session,CancellationService service){
        this.view=view;this.session=session;this.service=service;
        view.btnTim.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});
        view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});
        view.txtTuKhoa.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});
        view.cmbTrangThai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});
        view.tblYeuCau.getSelectionModel().addListSelectionListener(new ListSelectionListener(){public void valueChanged(ListSelectionEvent e){if(!busy&&!e.getValueIsAdjusting())detail();}});
        view.btnDuyet.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){decide(true);}});
        view.btnTuChoi.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){decide(false);}});
        invalidated=new Runnable(){public void run(){load();}};BillingChangeBus.forSession(session).subscribe(invalidated);
    }
    private boolean start(){if(busy)return false;if(!session.isActive()){view.expired();return false;}busy=true;view.busy(true);return true;}
    private boolean mayReject(){return selected!=null&&selected.request().trangThai()==YeuCauHuyHoaDon.TrangThai.CHO_DUYET&&!session.accountId().equals(selected.request().maTaiKhoanTao())&&selected.invoice().hoaDon().trangThai()!=TrangThaiHoaDon.DA_HUY;}
    private void buttons(){view.buttons(mayReject()&&selected.invoice().hoaDon().daThu().signum()==0,mayReject());}
    private abstract class Task<T> extends SessionWorker<T>{
        Task(){super(CancellationController.this.session);}
        protected void finished(){busy=false;view.busy(false);buttons();}
        protected void expired(){selected=null;view.expired();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(writeMessage+BillingUi.error(failure));if(reloadPending){reloadPending=false;load();}}
    }
    public void load(){
        if(busy){reloadPending=true;return;}final String keyword=view.txtTuKhoa.getText();final LocalDate from,to;
        try{from=date(view.txtTuNgay.getText());to=date(view.txtDenNgay.getText());}catch(IllegalArgumentException e){view.lblTrangThai.setText(e.getMessage());return;}
        final YeuCauHuyHoaDon.TrangThai status=view.cmbTrangThai.getSelectedItem() instanceof YeuCauHuyHoaDon.TrangThai value?value:null;
        if(!start())return;selected=null;view.clearDetail();
        new Task<List<YeuCauHuyHoaDon>>(){
            protected List<YeuCauHuyHoaDon> doInBackground()throws Exception{return service.find(keyword,from,to,status);}
            protected void succeeded(List<YeuCauHuyHoaDon> rows){busy=true;view.showRows(rows);busy=false;view.lblTrangThai.setText(writeMessage+(rows.isEmpty()?"Không có yêu cầu phù hợp.":rows.size()+" yêu cầu. Chọn dòng để xem chi tiết và lịch sử thu."));writeMessage="";if(reloadPending){reloadPending=false;load();}}
        }.execute();
    }
    private void detail(){
        selected=null;view.clearDetail();final String id=view.selectedId();if(id==null||!start())return;
        new Task<CancellationService.Detail>(){
            protected CancellationService.Detail doInBackground()throws Exception{return service.detail(id);}
            protected void succeeded(CancellationService.Detail result){
                if(!id.equals(view.selectedId())){detail();return;}selected=result;view.showDetail(result);buttons();
                view.lblTrangThai.setText(result.request().trangThai()!=YeuCauHuyHoaDon.TrangThai.CHO_DUYET?"Yêu cầu đã được xử lý, chỉ xem lịch sử.":session.accountId().equals(result.request().maTaiKhoanTao())?"Không được tự xét duyệt yêu cầu của mình.":result.invoice().hoaDon().daThu().signum()>0?"Chặn duyệt hủy: có khoản thu hợp lệ, chưa có quy trình hoàn tiền. Có thể từ chối và ghi lý do.":"Kiểm tra thông tin và lịch sử thu trước khi quyết định.");
                if(reloadPending){reloadPending=false;load();}
            }
        }.execute();
    }
    private void decide(final boolean approve){
        if(busy||!mayReject()||(approve&&selected.invoice().hoaDon().daThu().signum()!=0))return;
        final String reason=view.txaLyDoDuyet.getText().trim();if((!approve&&reason.isEmpty())||reason.length()>500){view.lblTrangThai.setText("Lý do từ chối bắt buộc; ý kiến tối đa 500 ký tự.");return;}
        final String id=selected.request().maYeuCau();
        if(JOptionPane.showConfirmDialog(view,(approve?"Duyệt hủy":"Từ chối")+" yêu cầu "+id+"?","Xác nhận xét duyệt",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        if(!start())return;
        new Task<Void>(){
            protected Void doInBackground()throws Exception{service.decide(id,approve,reason);return null;}
            protected void succeeded(Void ignored){selected=null;writeMessage=approve?"Đã duyệt hủy hóa đơn. ":"Đã từ chối; hóa đơn vẫn hoạt động. ";load();BillingChangeBus.forSession(session).publishExcept(invalidated);}
            protected void failed(Throwable failure){
                if(failure instanceof CancellationService.CommittedCancellationException){succeeded(null);return;}
                selected=null;view.buttons(false,false);writeMessage=BillingUi.error(failure)+" Chưa xác nhận kết quả; tải lại để kiểm tra trước khi thử lại. ";load();
            }
        }.execute();
    }
    private static LocalDate date(String text){if(text==null||text.isBlank())return null;try{return LocalDate.parse(text.trim());}catch(DateTimeParseException e){throw new IllegalArgumentException("Ngày phải có dạng yyyy-MM-dd và tồn tại.");}}
}
