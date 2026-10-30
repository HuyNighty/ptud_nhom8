package com.quanlynhakhoa.billing.ui;

import java.awt.*;
import javax.swing.*;

/** Whole filter groups wrap together; surplus space belongs to the tables, not inputs. */
public final class FilterBar extends JPanel {
    public FilterBar(JComponent... groups) {
        super(new WrappingFlow());
        for(JComponent group:groups)add(group);
    }
    private static final class WrappingFlow extends FlowLayout {
        WrappingFlow(){super(FlowLayout.LEADING,8,4);}
        @Override public Dimension preferredLayoutSize(Container target){
            synchronized(target.getTreeLock()){
                Insets insets=target.getInsets();
                Container page=target.getParent();while(page!=null&&!(page instanceof com.quanlynhakhoa.features.RefreshableFeature))page=page.getParent();
                Container parent=page!=null?page:target.getParent();
                int available=parent==null?target.getWidth():parent.getWidth()-parent.getInsets().left-parent.getInsets().right;
                if(available<=0)available=1200;
                available=Math.max(1,available-insets.left-insets.right-2*getHgap());
                int width=0,height=0,rowWidth=0,rowHeight=0;
                for(Component child:target.getComponents())if(child.isVisible()){
                    Dimension size=child.getPreferredSize();
                    if(rowWidth>0&&rowWidth+getHgap()+size.width>available){width=Math.max(width,rowWidth);height+=rowHeight+getVgap();rowWidth=0;rowHeight=0;}
                    if(rowWidth>0)rowWidth+=getHgap();rowWidth+=size.width;rowHeight=Math.max(rowHeight,size.height);
                }
                return new Dimension(Math.max(width,rowWidth)+insets.left+insets.right+2*getHgap(),height+rowHeight+insets.top+insets.bottom+2*getVgap());
            }
        }
        @Override public Dimension minimumLayoutSize(Container target){return preferredLayoutSize(target);}
    }
}
