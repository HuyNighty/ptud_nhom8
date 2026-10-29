package com.quanlynhakhoa.features;

import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.navigation.NavigationPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JPanel;

public final class FeatureRegistry {
    private final Map<Module, FeaturePanelFactory> factories;
    private final NavigationPolicy navigation = new NavigationPolicy();

    private FeatureRegistry(Map<Module, FeaturePanelFactory> factories) {
        this.factories = Map.copyOf(factories);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean contains(Module module) {
        return module != null && factories.containsKey(module);
    }

    public JPanel create(DesktopSession session, Module module) {
        navigation.requireOpen(session, module);
        FeaturePanelFactory factory = factories.get(module);
        if (factory == null) {
            throw new IllegalArgumentException("Module has no registered panel: " + module);
        }
        return Objects.requireNonNull(factory.create(session), "Feature panel cannot be null");
    }

    public static final class Builder {
        private final EnumMap<Module, FeaturePanelFactory> factories = new EnumMap<>(Module.class);

        private Builder() { }

        public Builder register(Module module, FeaturePanelFactory factory) {
            Objects.requireNonNull(module, "module");
            Objects.requireNonNull(factory, "factory");
            if (module == Module.HOME) {
                throw new IllegalArgumentException("HOME belongs to the Core dashboard");
            }
            if (factories.putIfAbsent(module, factory) != null) {
                throw new IllegalArgumentException("Module registered twice: " + module);
            }
            return this;
        }

        public FeatureRegistry build() {
            return new FeatureRegistry(factories);
        }
    }
}
