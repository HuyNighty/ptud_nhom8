package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

public final class TaoHoaDonController {
    public interface Saved { void accept(String id); }
    private final TaoHoaDonDialog view;
    private final DesktopSession session;
    private final HoaDonService service;
    private final Saved saved;
    private InvoiceDraft draft;
    private boolean busy, updating;
    public TaoHoaDonController(TaoHoaDonDialog view,DesktopSession session,HoaDonService service,Saved saved) {
        this.view=view;this.session=session;this.service=service;this.saved=saved;
        view.btnTimBenhNhan.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){patients();}});
        view.txtBenhNhan.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){patients();}});
        view.cmbBenhNhan.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updating&&!busy)visits();}});
        view.cmbPhieuKham.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updating&&!busy)clear();}});
        view.btnTaiDichVu.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){preview();}});
        view.btnLuuHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){save();}});
    }
    private void clear(){draft=null;view.showDraft(null);view.btnLuuHoaDon.setEnabled(false);}
    private boolean start(){if(busy)return false;if(!session.isActive()){view.dispose();return false;}busy=true;view.busy(true,false);return true;}
    private abstract class Task<T> extends SessionWorker<T> {
        Task(){super(TaoHoaDonController.this.session);}
        protected void finished(){busy=false;view.busy(false,draft!=null);}
        protected void expired(){clear();view.dispose();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(BillingUi.error(failure));}
    }
    private void patients(){
        if(!start())return;clear();final String text=view.txtBenhNhan.getText();
        new Task<List<BillingChoice>>() {
            protected List<BillingChoice> doInBackground()throws Exception{return service.patientsForInvoice(text);}
            protected void succeeded(List<BillingChoice> result){updating=true;view.cmbBenhNhan.removeAllItems();view.cmbPhieuKham.removeAllItems();for(BillingChoice v:result)view.cmbBenhNhan.addItem(v);updating=false;view.lblTrangThai.setText(result.isEmpty()?"Không có bệnh nhân phù hợp.":"Chọn bệnh nhân và lượt khám.");visits();}
        }.execute();
    }
    private void visits(){
        clear();final BillingChoice patient=(BillingChoice)view.cmbBenhNhan.getSelectedItem();
        updating=true;view.cmbPhieuKham.removeAllItems();updating=false;
        if(patient==null||!start())return;
        new Task<List<BillingChoice>>() {
            protected List<BillingChoice> doInBackground()throws Exception{return service.visitsForInvoice(patient.id());}
            protected void succeeded(List<BillingChoice> result){updating=true;for(BillingChoice v:result)view.cmbPhieuKham.addItem(v);updating=false;view.lblTrangThai.setText(result.isEmpty()?"Không có lượt khám phù hợp.":"Tải dịch vụ để kiểm tra điều kiện tính phí.");}
        }.execute();
    }
    private void preview(){
        final BillingChoice patient=(BillingChoice)view.cmbBenhNhan.getSelectedItem(),visit=(BillingChoice)view.cmbPhieuKham.getSelectedItem();
        if(patient==null||visit==null||!start())return;clear();
        new Task<InvoiceDraft>() {
            protected InvoiceDraft doInBackground()throws Exception{return service.previewInvoice(patient.id(),visit.id());}
            protected void succeeded(InvoiceDraft result){draft=result;view.showDraft(result);view.busy(false,true);view.lblTrangThai.setText("Kiểm tra dòng tính phí và tổng tiền trước khi lưu.");}
        }.execute();
    }
    private void save(){
        if(busy||draft==null)return;
        if(JOptionPane.showConfirmDialog(view,"Lưu hóa đơn với tổng "+BillingUi.vnd(draft.total())+"?","Xác nhận",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        if(!start())return;final InvoiceDraft reviewed=draft;
        new Task<String>() {
            protected String doInBackground()throws Exception{return service.createInvoice(reviewed);}
            protected void succeeded(String id){view.dispose();saved.accept(id);BillingChangeBus.forSession(session).publish();}
            protected void failed(Throwable failure){
                if(failure instanceof InvoiceCreationService.CommittedInvoiceException committed){succeeded(committed.invoiceId());return;}
                clear();view.lblTrangThai.setText(BillingUi.error(failure)+" Tải lại/xác minh kết quả trước khi lưu lại; không tự thử lại.");
            }
        }.execute();
    }
}
