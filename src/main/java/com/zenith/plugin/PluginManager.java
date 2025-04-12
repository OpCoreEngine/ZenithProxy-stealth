package com.zenith.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.event.plugin.PluginLoadFailureEvent;
import com.zenith.event.plugin.PluginLoadedEvent;
import com.zenith.plugin.api.InstancedPluginAPI;
import com.zenith.plugin.api.PluginInfo;
import com.zenith.plugin.api.PluginInstance;
import com.zenith.plugin.api.ZenithProxyPlugin;
import com.zenith.util.ImageInfo;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.zenith.Globals.*;

public class PluginManager {
    public static final Path PLUGINS_PATH = Path.of("plugins");
    private final Map<String, ConfigInstance> pluginConfigurations = new ConcurrentHashMap<>();
    private final Map<String, PluginInstance> pluginInstances = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean pluginsLoaded = new AtomicBoolean(false);

    public List<PluginInfo> getPluginInfos() {
        return pluginInstances.values().stream().map(PluginInstance::getPluginInfo).collect(Collectors.toList());
    }

    public String getId(final ZenithProxyPlugin pluginInstance) {
        return pluginInstances.values().stream()
            .filter(i -> i.getPluginInstance() == pluginInstance)
            .findFirst()
            .map(PluginInstance::getId)
            .orElseThrow(() -> new RuntimeException("Plugin instance " + pluginInstance.getClass().getName() + " not found"));
    }

    public PluginInfo getPluginInfo(final ZenithProxyPlugin pluginInstance) {
        return pluginInstances.get(getId(pluginInstance)).getPluginInfo();
    }

    public void saveConfigs(BiConsumer<File, Object> saveFunction) {
        pluginConfigurations.values()
            .forEach(config -> saveFunction.accept(config.file(), config.instance()));
    }

    public List<ConfigInstance> getAllPluginConfigs() {
        return new ArrayList<>(pluginConfigurations.values());
    }

    public record ConfigInstance(Object instance, Class<?> clazz, File file) { }

    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            if (!ImageInfo.inImageCode()) {
                preLoadPlugins();
                loadPlugins();
            } else {
                if (ImageInfo.inImageRuntimeCode())
                    linuxChannelIncompatibilityWarning();
            }
        }
    }

    private void linuxChannelIncompatibilityWarning() {
        var potentialJars = findPotentialPluginJars();
        int potentialPluginCount = potentialJars.size();
        if (potentialPluginCount > 0) {
            PLUGIN_LOG.warn("""
                Plugins are not supported on the `linux` release channel.
                Detected {} potential plugin jars in the plugins directory.
                
                To use plugins, switch to the `java` channel: `channel set java <mcVersion>`
                """, potentialPluginCount);
        }
    }

    private void preLoadPlugins() {
        var potentialPlugins = findPotentialPluginJars();
        for (var jar : potentialPlugins) {
            try {
                preLoadPotentialPluginJar(jar);
            } catch (Throwable e) {
                PLUGIN_LOG.error("Error loading plugin jar: {}", jar, e);
            }
        }
    }

    private void loadPlugins() {
        for (var instance : pluginInstances.entrySet()) {
            try {
                loadPlugin(instance.getValue());
            } catch (Throwable e) {
                PLUGIN_LOG.error("Error loading plugin: {} : {}", instance.getKey(), instance.getValue().getJarPath(), e);
            }
        }
    }

    private List<Path> findPotentialPluginJars() {
        if (!PLUGINS_PATH.toFile().exists()) return Collections.emptyList();
        final List<Path> list = new ArrayList<>();
        try (var jarStream = Files.newDirectoryStream(PLUGINS_PATH, p -> p.toFile().isFile() && p.toString().endsWith(".jar"))) {
            for (var jarPath : jarStream) {
                list.add(jarPath);
            }
        } catch (Throwable e) {
            PLUGIN_LOG.error("Error loading plugins", e);
        }
        // sort alphabetically by filename
        list.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return list;
    }

    private void preLoadPotentialPluginJar(final Path jarPath) {
        String id = null;
        URLClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader());;
            PluginInfo pluginInfo = readPluginInfo(classLoader, jarPath);
            id = Objects.requireNonNull(pluginInfo.id(), "Plugin id is null");
            if (pluginInstances.containsKey(id)) {
                // todo: we could try to sort by version and load the "newest" one
                throw new RuntimeException("Plugin id already exists: " + id);
            }
            if (pluginInfo.mcVersions().isEmpty()) {
                PLUGIN_LOG.error("Plugin: {} has no MC versions specified", jarPath);
                throw new RuntimeException("Plugin has no MC versions specified");
            }
            if (!pluginInfo.mcVersions().contains("*") && !pluginInfo.mcVersions().contains(MC_VERSION)) {
                PLUGIN_LOG.warn("Plugin: {} not compatible with current MC version. Actual: {}, Plugin Required: {}", jarPath, MC_VERSION, pluginInfo.mcVersions());
                return;
            }

            PLUGIN_LOG.info(
                "Found Plugin:\n  id: {}\n  version: {}\n  description: {}\n  url: {}\n  authors: {}\n  jar: {}",
                pluginInfo.id(),
                pluginInfo.version(),
                pluginInfo.description(),
                pluginInfo.url(),
                pluginInfo.authors(),
                jarPath.getFileName()
            );

            pluginInstances.put(id, new PluginInstance(id, jarPath, pluginInfo, classLoader));
        } catch (Throwable e) {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (IOException ignored) { }
            }
            PLUGIN_LOG.error("Error loading plugin: {}", jarPath, e);
            EVENT_BUS.postAsync(new PluginLoadFailureEvent(id, jarPath, e));
        }
    }

    private void loadPlugin(final PluginInstance pluginInstance) {
        try {
            var pluginInfo = pluginInstance.getPluginInfo();
            var classLoader = pluginInstance.getClassLoader();
            var jarPath = pluginInstance.getJarPath();
            String entrypoint = Objects.requireNonNull(pluginInfo.entrypoint(), "Plugin entrypoint is null");

            PLUGIN_LOG.info("Loading Plugin: {}", pluginInfo.id());

            Class<?> pluginClass = classLoader.loadClass(entrypoint);
            if (!ZenithProxyPlugin.class.isAssignableFrom(pluginClass)) {
                throw new RuntimeException("Plugin does not implement ZenithProxyPlugin interface");
            }
            ZenithProxyPlugin plugin = (ZenithProxyPlugin) pluginClass.getDeclaredConstructor().newInstance();

            pluginInstance.setPluginInstance(plugin);

            try {
                plugin.onLoad(new InstancedPluginAPI(plugin, pluginInfo));
            } catch (final Throwable e) {
                PLUGIN_LOG.error("Exception in plugin onLoad: {}", jarPath, e);
                pluginInstances.remove(pluginInstance.getId());
                throw new RuntimeException("Exception in plugin onLoad: " + e.getMessage(), e);
            }
            EVENT_BUS.postAsync(new PluginLoadedEvent(pluginInfo));
        } catch (Throwable e) {
            try {
                pluginInstance.getClassLoader().close();
            } catch (IOException ignored) { }
            PLUGIN_LOG.error("Error loading plugin: {}", pluginInstance, e);
            EVENT_BUS.postAsync(new PluginLoadFailureEvent(pluginInstance.getId(), pluginInstance.getJarPath(), e));
        }
    }

    @SneakyThrows
    private PluginInfo readPluginInfo(URLClassLoader classLoader, Path path) {
        try (var stream = classLoader.getResourceAsStream("plugin.json")) {
            if (stream == null) {
                throw new RuntimeException("plugin.json not found in jar");
            }
            return OBJECT_MAPPER.readValue(stream, PluginInfo.class);
        } catch (IOException e) {
            PLUGIN_LOG.error("Error reading plugin.json: {}", path, e);
            throw e;
        }
    }

    public synchronized <T> T registerConfig(String fileName, Class<T> clazz) {
        if (pluginConfigurations.containsKey(fileName)) {
            throw new RuntimeException("Config already registered: " + fileName);
        }
        var config = loadPluginConfig(fileName, clazz);
        File configFile = PLUGINS_PATH.resolve("config").resolve(fileName + ".json").toFile();
        if (!configFile.exists()) {
            if (!configFile.getParentFile().mkdirs() && !configFile.getParentFile().exists()) {
                throw new RuntimeException("Unable to create plugin config directory: " + configFile.getParentFile());
            }
        }
        var configInstance = new ConfigInstance(
            config,
            clazz,
            configFile
        );
        pluginConfigurations.put(fileName, configInstance);
        return config;
    }

    @SneakyThrows
    private <T> T loadPluginConfig(String fileName, Class<T> clazz) {
        try {
            PLUGIN_LOG.info("Loading plugin config...");
            File configFile = new File("plugins/" + fileName + ".json");
            T config;
            if (configFile.exists()) {
                try (Reader reader = new FileReader(configFile)) {
                    final Gson GSON = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .create();
                    config = GSON.fromJson(reader, clazz);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load plugin config: " + fileName, e);
                }
                PLUGIN_LOG.info("Plugin config: {} loaded.", fileName);
            } else {
                config = clazz.getDeclaredConstructor().newInstance();
                PLUGIN_LOG.info("Plugin config: {} not found.", fileName);
            }
            return config;
        } catch (final Throwable e) {
            PLUGIN_LOG.error("Unable to load plugin config: {}", fileName, e);
            PLUGIN_LOG.error("Config must be manually fixed or deleted");
            throw e;
        }
    }
}
