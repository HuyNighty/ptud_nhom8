package com.quanlynhakhoa.controller.dieutri;

import com.quanlynhakhoa.view.dieutri.KeHoachDieuTriPanel;
import com.quanlynhakhoa.service.dieutri.KeHoachDieuTriService;
import com.quanlynhakhoa.service.dieutri.KeHoachDieuTriKhongKhaDungException;
import com.quanlynhakhoa.security.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public final class KeHoachDieuTriController {
    private final KeHoachDieuTriPanel view;
    private final KeHoachDieuTriService service;
    public KeHoachDieuTriController(KeHoachDieuTriPanel view,KeHoachDieuTriService service){
        this.view=view;
        this.service=service;
        view.refresh().addActionListener(e->loadPlans());
        view.createPlan().addActionListener(e->createPlan());
        view.addStep().addActionListener(e->addStep());
        view.completeStep().addActionListener(e->completeStep());
        view.cancelStep().addActionListener(e->cancelStep());
        view.completePlan().addActionListener(e->completePlan());
        view.cancelPlan().addActionListener(e->cancelPlan());
        view.plans().getSelectionModel().addListSelectionListener(e->{
            if(!e.getValueIsAdjusting())loadDetails();
        });
        loadPlans();
    }
    private void loadPlans(){
        async(service::danhSach,rows->fill(view.planModel(),rows));
    }

    private void loadDetails(){
        String id=view.selectedPlan();
        if(id==null){view.detailModel().setRowCount(0);
            return;
        }
        async(()->service.chiTiet(id),rows->fill(view.detailModel(),rows));
    }

    private void createPlan(){
        try{
            String id=view.planId().getText(),chart=view.chartId().getText(),visit=view.visitId().getText(),goal=view.goal().getText();
            LocalDate date=LocalDate.parse(view.planDate().getText().trim());
            async(()->{service.taoKeHoach(id,chart,visit,date,goal);
                return null;
                },x->{view.planId().setText("");
                view.goal().setText("");
                loadPlans();
                JOptionPane.showMessageDialog(view,"Đã tạo kế hoạch điều trị.");
            });
        }catch(RuntimeException e) {
            error(e);
        }
    }

    private void addStep(){
        try{
            String id=view.selectedPlan();
            if(id==null)throw new IllegalArgumentException("Chọn kế hoạch trước.");
            String serviceId=view.serviceId().getText(),tooth=view.tooth().getText(),note=view.stepNote().getText();
            LocalDate planned=LocalDate.parse(view.dueDate().getText().trim());
            async(()->{service.themBuoc(id,serviceId,tooth,planned,note);
                return null;
                },x->{view.stepNote().setText("");
                loadDetails();
                JOptionPane.showMessageDialog(view,"Đã thêm bước điều trị.");
            });
        }catch(RuntimeException e){
            error(e);
        }
    }

    private void completeStep(){
        try{Integer id=view.selectedDetail();
            if(id==null)throw new IllegalArgumentException("Chọn bước cần hoàn thành.");
            String result=JOptionPane.showInputDialog(view,"Nhập kết quả thực hiện:","Hoàn thành bước",JOptionPane.QUESTION_MESSAGE);
            if(result==null)return;
            LocalDate date=LocalDate.now();
            async(()->{service.hoanThanhBuoc(id,date,result);
                return null;
                },x->{loadDetails();
                loadPlans();
                JOptionPane.showMessageDialog(view,"Đã cập nhật bước điều trị thành công.");
            });
        }catch(RuntimeException e){
            error(e);
        }
    }

    private void cancelStep(){
        try{Integer id=view.selectedDetail();
            if(id==null)throw new IllegalArgumentException("Chọn bước cần hủy.");
            String reason=JOptionPane.showInputDialog(view,"Lý do hủy bước:","Hủy bước",JOptionPane.QUESTION_MESSAGE);
            if(reason==null)
                return;
            async(()->{service.huyBuoc(id,reason);
                return null;
                },x->{loadDetails();
                loadPlans();
                JOptionPane.showMessageDialog(view,"Đã cập nhật bước điều trị thành công.");
            });
        }catch(RuntimeException e){
            error(e);
        }
    }

    private void completePlan(){
        String id=view.selectedPlan();
        if(id==null){
            error(new IllegalArgumentException("Chọn kế hoạch cần hoàn tất."));
            return;
        }
        int ok=JOptionPane.showConfirmDialog(view,"Hoàn tất kế hoạch đã chọn?","Xác nhận",JOptionPane.YES_NO_OPTION);
        if(ok!=JOptionPane.YES_OPTION)
            return;
        async(()->{service.hoanThanhKeHoach(id);
            return null;
            },x->{loadPlans();
            JOptionPane.showMessageDialog(view,"Đã hoàn tất kế hoạch.");
        });
    }

    private void cancelPlan(){
        String id=view.selectedPlan();
        if(id==null){
            error(new IllegalArgumentException("Chọn kế hoạch cần hủy."));
            return;
        }
        String reason=JOptionPane.showInputDialog(view,"Lý do hủy kế hoạch:","Hủy kế hoạch",JOptionPane.QUESTION_MESSAGE);
        if(reason==null)
            return;
        async(()->{service.huyKeHoach(id,reason);
            return null;
            },x->{loadPlans();
            view.detailModel().setRowCount(0);
            JOptionPane.showMessageDialog(view,"Đã hủy kế hoạch điều trị.");
        });
    }

    private static void fill(DefaultTableModel model,List<Object[]> rows){
        model.setRowCount(0);
        for(Object[] row:rows)
            model.addRow(row);
    }

    private void error(Throwable t){
        JOptionPane.showMessageDialog(view,messageFor(t),"Kế hoạch điều trị",JOptionPane.ERROR_MESSAGE);
    }

    static String messageFor(Throwable failure){
        Throwable error = failure instanceof java.util.concurrent.ExecutionException
                && failure.getCause() != null ? failure.getCause() : failure;
        if(error instanceof KeHoachDieuTriKhongKhaDungException)
            return new KeHoachDieuTriKhongKhaDungException().getMessage();
        if(error instanceof AccessDeniedException)
            return "Bạn không có quyền thực hiện thao tác này hoặc phiên đăng nhập đã kết thúc.";
        if(error instanceof IllegalArgumentException)
            return "Thông tin kế hoạch điều trị chưa hợp lệ. Vui lòng kiểm tra lại.";
        return "Không thể xử lý kế hoạch điều trị. Vui lòng thử lại sau.";
    }

    private <T>void async(Callable<T> task,Consumer<T> success){
        new SwingWorker<T,Void>(){
            @Override protected T doInBackground()throws Exception{
                return task.call();
            }
            @Override protected void done(){
                try{success.accept(get());
                }catch(Exception e){
                    error(e);
                }
            }
        }
        .execute();
    }
}
