package com.quanlynhakhoa.configuration.swing;

import com.quanlynhakhoa.desktop.frame.MainFrame;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public final class DesktopLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(DesktopLauncher.class);
    private final ConfigurableApplicationContext applicationContext;
    private final boolean desktopEnabled;

    public DesktopLauncher(ConfigurableApplicationContext applicationContext,
                           @Value("${n08.desktop.enabled:true}") boolean desktopEnabled) {
        this.applicationContext = applicationContext;
        this.desktopEnabled = desktopEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openDesktopWhenReady() {
        if (!desktopEnabled) {
            LOG.info("Desktop disabled; embedded REST API is still available.");
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("Desktop requested in headless environment; JFrame was not opened.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    new Thread(applicationContext::close, "n08-context-shutdown").start();
                }
            });
            frame.setVisible(true);
        });
    }
}
