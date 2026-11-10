package com.quanlynhakhoa.controller.hoadon;

import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.swing.SwingUtilities;

/** Session-scoped invalidation of created views, including hidden cards. No JDBC here. */
public final class ThongBaoThayDoiHoaDon {
    private static final Map<DesktopSession,ThongBaoThayDoiHoaDon> BUSES=new WeakHashMap<>();
    private final List<WeakReference<Runnable>> listeners=new ArrayList<>();
    public static synchronized ThongBaoThayDoiHoaDon forSession(DesktopSession session){
        ThongBaoThayDoiHoaDon bus=BUSES.get(session);if(bus==null){bus=new ThongBaoThayDoiHoaDon();BUSES.put(session,bus);}return bus;
    }
    public void subscribe(Runnable listener){listeners.add(new WeakReference<>(listener));}
    public void publish(){publishExcept(null);}
    public void publishExcept(final Runnable excluded){SwingUtilities.invokeLater(new Runnable(){public void run(){
        List<Runnable> callbacks=new ArrayList<>();
        Iterator<WeakReference<Runnable>> iterator=listeners.iterator();while(iterator.hasNext()){Runnable listener=iterator.next().get();if(listener==null)iterator.remove();else callbacks.add(listener);}
        for(Runnable callback:callbacks)if(callback!=excluded)callback.run();
    }});}
}
