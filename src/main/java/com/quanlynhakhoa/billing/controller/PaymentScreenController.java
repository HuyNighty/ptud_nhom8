package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.util.List;

public final class PaymentScreenController {
    private final PaymentPanel view;private final DesktopSession session;private final ConnectionProvider connections;private final HoaDonService invoices;
    private PaymentController collection;private PaymentForm form;private String invoiceId;private boolean searching,updating,searchPending;
    private final Runnable invalidated;
    public PaymentScreenController(PaymentPanel view,DesktopSession session,ConnectionProvider connections){
        this.view=view;this.session=session;this.connections=connections;invoices=new HoaDonService(session,connections);
        view.btnTim.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){search();}});view.btnTaiLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){refresh();}});view.txtTimHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){search();}});
        view.cmbHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updating&&view.cmbHoaDon.getSelectedItem() instanceof BillingChoice choice)openInvoice(choice.id());}});
        invalidated=new Runnable(){public void run(){refresh();}};BillingChangeBus.forSession(session).subscribe(invalidated);
    }
    private void updatePicker(){view.pickerEnabled(!searching&&(form==null||!form.isWorking()&&!form.hasUncertainResult()));}
    public void openInvoice(String id){
        if(!session.isActive()){view.expired();return;}
        if(form!=null&&(form.isWorking()||form.hasUncertainResult())){view.lblTrangThai.setText("Hoàn tất/kiểm tra lần thu hiện tại trước khi đổi hóa đơn.");return;}
        if(id.equals(invoiceId)&&collection!=null){collection.refresh();return;}
        invoiceId=id;updating=true;
        for(int index=0;index<view.cmbHoaDon.getItemCount();index++)if(view.cmbHoaDon.getItemAt(index).id().equals(id)){view.cmbHoaDon.setSelectedIndex(index);break;}
        updating=false;form=new PaymentForm();form.onStateChange(new Runnable(){public void run(){updatePicker();}});view.showForm(form);
        collection=new PaymentController(form,session,connections,id,new Runnable(){public void run(){view.lblTrangThai.setText("Đã thu tiền thành công. Lịch sử và công nợ đang được cập nhật.");search();}});updatePicker();
    }
    public void refresh(){if(!session.isActive()){view.expired();return;}search();if(collection!=null)collection.refresh();}
    private void search(){
        if(searching){searchPending=true;return;}if(!session.isActive()){view.expired();return;}
        searching=true;updatePicker();final String keyword=view.txtTimHoaDon.getText();
        new SessionWorker<List<HoaDon>>(session){
            protected List<HoaDon> doInBackground()throws Exception{return DebtService.outstanding(invoices.find(keyword,null,null,null));}
            protected void finished(){searching=false;updatePicker();}
            protected void expired(){view.expired();}
            protected void succeeded(List<HoaDon> found){updating=true;view.showChoices(found,invoiceId);updating=false;view.lblTrangThai.setText(found.isEmpty()?"Không có hóa đơn còn nợ phù hợp. Vẫn giữ lịch sử hóa đơn đang chọn.":found.size()+" hóa đơn còn nợ. Chọn hóa đơn để thu tiền.");if(searchPending){searchPending=false;search();}}
            protected void failed(Throwable failure){view.lblTrangThai.setText("Tìm hóa đơn thất bại. "+BillingUi.error(failure));}
        }.execute();
    }
}
