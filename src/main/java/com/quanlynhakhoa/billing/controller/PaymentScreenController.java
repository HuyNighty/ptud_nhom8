package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.util.List;

public final class PaymentScreenController {
    private final PaymentPanel view;
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final HoaDonService invoices;
    private PaymentController collection;
    private PaymentForm form;
    private String invoiceId;
    private boolean searching,updating,searchPending;
    private final Runnable invalidated;
    public PaymentScreenController(PaymentPanel view,DesktopSession session,ConnectionProvider connections){
        this.view=view;this.session=session;this.connections=connections;invoices=new HoaDonService(session,connections);
        view.btnTim.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){userSearch();}});
        view.txtTimHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){userSearch();}});
        view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){refresh();}});
        view.cmbHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updating&&view.cmbHoaDon.getSelectedItem() instanceof BillingChoice choice)openInvoice(choice.id());}});
        invalidated=new Runnable(){public void run(){refresh();}};BillingChangeBus.forSession(session).subscribe(invalidated);
    }
    private boolean locked(){return form!=null&&(form.isWorking()||form.hasUncertainResult());}
    private void updatePicker(){view.pickerEnabled(!searching&&!locked());}
    private void userSearch(){
        if(locked()||searching)return;
        // A fresh search must not leave an unrelated invoice ready to collect.
        collection=null;invoiceId=null;form=null;updating=true;view.showChoices(List.of(),null);updating=false;view.showForm(new PaymentForm());search();
    }
    public void openInvoice(String id){
        if(!session.isActive()){view.expired();return;}
        if(locked()){view.lblTrangThai.setText("Hoàn tất hoặc kiểm tra lần thu hiện tại trước khi đổi hóa đơn.");return;}
        if(id.equals(invoiceId)&&collection!=null){collection.refresh();return;}
        invoiceId=id;view.txtTimHoaDon.setText(id);updating=true;
        for(int index=0;index<view.cmbHoaDon.getItemCount();index++)if(view.cmbHoaDon.getItemAt(index).id().equals(id)){view.cmbHoaDon.setSelectedIndex(index);break;}
        updating=false;form=new PaymentForm();form.onStateChange(new Runnable(){public void run(){updatePicker();}});view.showForm(form);
        collection=new PaymentController(form,session,connections,id,new Runnable(){public void run(){view.lblTrangThai.setText("Đã thu tiền thành công. Lịch sử và công nợ đang cập nhật.");search();}});
        search();updatePicker();
    }
    public void refresh(){if(!session.isActive()){view.expired();return;}search();if(collection!=null)collection.refresh();}
    private void search(){
        if(searching){searchPending=true;return;}if(!session.isActive()){view.expired();return;}
        searching=true;if(form!=null)form.setCollectionContextAllowed(false);updatePicker();view.lblTrangThai.setText("Đang tìm hóa đơn còn nợ…");final String keyword=view.txtTimHoaDon.getText();
        new SessionWorker<List<HoaDon>>(session){
            protected List<HoaDon> doInBackground()throws Exception{return DebtService.outstanding(invoices.find(keyword,null,null,null));}
            protected void finished(){searching=false;updatePicker();}
            protected void expired(){view.expired();}
            protected void succeeded(List<HoaDon> found){
                if(searchPending||!keyword.equals(view.txtTimHoaDon.getText())){searchPending=false;search();return;}
                updating=true;view.showChoices(found,invoiceId);updating=false;
                boolean matches=false;for(HoaDon invoice:found)if(invoice.maHoaDon().equals(invoiceId))matches=true;
                if(form!=null)form.setCollectionContextAllowed(matches);
                String message=found.isEmpty()?(keyword.isBlank()?"Chưa có hóa đơn còn phải thu.":"Không tìm thấy hóa đơn còn nợ phù hợp."):found.size()+" kết quả. Chọn hóa đơn để thu tiền.";
                if(invoiceId!=null)message=matches?"Đang xem hóa đơn "+invoiceId+".":"Tra cứu hóa đơn "+invoiceId+" · không thuộc kết quả còn nợ hiện tại.";
                view.lblTrangThai.setText(message);
            }
            protected void failed(Throwable failure){view.lblTrangThai.setText("Tìm hóa đơn thất bại. "+BillingUi.error(failure));if(searchPending){searchPending=false;search();}}
        }.execute();
    }
}
