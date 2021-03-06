package org.embulk.plugin;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.embulk.config.ConfigException;

public class PluginManager
{
    private final List<PluginSource> sources;
    private final Injector injector;

    // Set<PluginSource> is injected BuiltinPluginSourceModule or extensions
    // using Multibinder<PluginSource>.
    @Inject
    public PluginManager(Set<PluginSource> pluginSources, Injector injector)
    {
        this.sources = ImmutableList.copyOf(pluginSources);
        this.injector = injector;
    }

    public <T> T newPlugin(Class<T> iface, PluginType type)
    {
        if (sources.isEmpty()) {
            throw new ConfigException("No PluginSource is installed");
        }

        List<Throwable> causes = new ArrayList<Throwable>();
        for (PluginSource source : sources) {
            try {
                return source.newPlugin(iface, type);
            } catch (PluginSourceNotMatchException e) {
                if (e.getCause() != null) {
                    causes.add(e.getCause());
                }
            }
        }

        ConfigException e = new ConfigException(String.format("%s '%s' is not found",
                    iface.getSimpleName(), type.getName()), causes.remove(causes.size()-1));
        for (Throwable cause : causes) {
            e.addSuppressed(cause);
        }
        throw e;
    }
}
