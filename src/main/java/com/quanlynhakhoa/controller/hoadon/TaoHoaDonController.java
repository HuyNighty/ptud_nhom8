package com.quanlynhakhoa.controller.hoadon;

import com.quanlynhakhoa.view.hoadon.TaoHoaDonDialog;
import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.service.hoadon.TaoHoaDonService;
import com.quanlynhakhoa.service.hoadon.HoaDonService;
import com.quanlynhakhoa.model.hoadon.LuaChonLapHoaDon;
import com.quanlynhakhoa.model.hoadon.BanNhapHoaDon;
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
    private BanNhapHoaDon banNhap;
    private boolean busy,updating;
    public TaoHoaDonController(TaoHoaDonDialog view,DesktopSession session,HoaDonService service,Saved saved){
        this.view=view;this.session=session;this.service=service;this.saved=saved;
        view.btnTimBenhNhan.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){patients();}});
        view.txtBenhNhan.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){patients();}});
        view.cmbBenhNhan.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updating&&!busy)visits();}});
        view.cmbPhieuKham.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updating&&!busy){clear();view.busy(false,false);view.guide(view.cmbPhieuKham.getSelectedItem()==null?"Chọn lượt khám":"Bấm Tải dịch vụ tính phí để kiểm tra lượt khám đã chọn.");}}});
        view.btnTaiDichVu.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){preview();}});
        view.btnLuuHoaDon.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){save();}});
    }
    private void clear(){banNhap=null;view.showDraft(null);view.btnLuuHoaDon.setEnabled(false);}
    private boolean start(){if(busy)return false;if(!session.isActive()){view.dispose();return false;}busy=true;view.busy(true,false);return true;}
    private abstract class Task<T> extends SessionWorker<T>{
        Task(){super(TaoHoaDonController.this.session);}
        protected void finished(){busy=false;view.busy(false,banNhap!=null);}
        protected void expired(){clear();view.dispose();}
        protected void failed(Throwable failure){view.guide(GiaoDienHoaDon.error(failure));}
    }
    private void patients(){
        if(!start())return;clear();final String text=view.txtBenhNhan.getText();
        updating=true;view.cmbBenhNhan.removeAllItems();view.cmbPhieuKham.removeAllItems();updating=false;view.guide("Đang tìm bệnh nhân…");
        new Task<List<LuaChonLapHoaDon>>(){
            protected List<LuaChonLapHoaDon> doInBackground()throws Exception{return service.patientsForInvoice(text);}
            protected void succeeded(List<LuaChonLapHoaDon> result){
                updating=true;for(LuaChonLapHoaDon item:result)view.cmbBenhNhan.addItem(item);view.cmbBenhNhan.setSelectedIndex(-1);updating=false;
                view.busy(false,false);view.guide(result.isEmpty()?"Không tìm thấy bệnh nhân. Hãy đổi mã, tên hoặc số điện thoại.":"Chọn bệnh nhân");
            }
        }.execute();
    }
    private void visits(){
        clear();final LuaChonLapHoaDon patient=(LuaChonLapHoaDon)view.cmbBenhNhan.getSelectedItem();
        updating=true;view.cmbPhieuKham.removeAllItems();updating=false;
        if(patient==null){view.busy(false,false);view.guide("Chọn bệnh nhân");return;}
        if(!start())return;view.guide("Đang tải lượt khám của bệnh nhân đã chọn…");
        new Task<List<LuaChonLapHoaDon>>(){
            protected List<LuaChonLapHoaDon> doInBackground()throws Exception{return service.visitsForInvoice(patient.id());}
            protected void succeeded(List<LuaChonLapHoaDon> result){
                updating=true;for(LuaChonLapHoaDon item:result)view.cmbPhieuKham.addItem(item);view.cmbPhieuKham.setSelectedIndex(-1);updating=false;
                view.busy(false,false);view.guide(result.isEmpty()?"Bệnh nhân chưa có lượt khám phù hợp để lập hóa đơn.":"Chọn lượt khám");
            }
        }.execute();
    }
    private void preview(){
        final LuaChonLapHoaDon patient=(LuaChonLapHoaDon)view.cmbBenhNhan.getSelectedItem(),visit=(LuaChonLapHoaDon)view.cmbPhieuKham.getSelectedItem();
        if(patient==null||visit==null||!start())return;clear();view.guide("Đang kiểm tra dịch vụ được phép tính phí…");
        new Task<BanNhapHoaDon>(){
            protected BanNhapHoaDon doInBackground()throws Exception{return service.previewInvoice(patient.id(),visit.id());}
            protected void succeeded(BanNhapHoaDon result){banNhap=result;view.showDraft(result);view.busy(false,true);view.guide("Kiểm tra dịch vụ, số lượng và tổng tiền trước khi lưu.");}
        }.execute();
    }
    private void save(){
        if(busy||banNhap==null)return;
        if(JOptionPane.showConfirmDialog(view,"Lưu hóa đơn với tổng "+GiaoDienHoaDon.vnd(banNhap.total())+"?","Xác nhận",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        if(!start())return;final BanNhapHoaDon reviewed=banNhap;view.guide("Đang lưu hóa đơn…");
        new Task<String>(){
            protected String doInBackground()throws Exception{return service.createInvoice(reviewed);}
            protected void succeeded(String id){GiaoDienHoaDon.result(view,"Đã lập hóa đơn "+id+" thành công.");view.dispose();saved.accept(id);ThongBaoThayDoiHoaDon.forSession(session).publish();}
            protected void failed(Throwable failure){if(failure instanceof TaoHoaDonService.CommittedInvoiceException committed){succeeded(committed.invoiceId());return;}GiaoDienHoaDon.result(view,GiaoDienHoaDon.error(failure)+" Xác minh kết quả trước khi lưu lại.","Chưa xác nhận lưu hóa đơn",javax.swing.JOptionPane.ERROR_MESSAGE);clear();view.guide(GiaoDienHoaDon.error(failure)+" Xác minh kết quả trước khi lưu lại.");}
        }.execute();
    }
}
