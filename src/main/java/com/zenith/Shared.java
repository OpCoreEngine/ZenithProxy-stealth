package com.zenith;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.rfresh2.SimpleEventBus;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.cache.DataCache;
import com.zenith.command.CommandManager;
import com.zenith.database.DatabaseManager;
import com.zenith.discord.DiscordBot;
import com.zenith.feature.items.PlayerInventoryManager;
import com.zenith.feature.tps.TPSCalculator;
import com.zenith.feature.whitelist.PlayerListsManager;
import com.zenith.feature.world.InputManager;
import com.zenith.mc.block.BlockDataManager;
import com.zenith.mc.dimension.DimensionDataManager;
import com.zenith.mc.entity.EntityDataManager;
import com.zenith.mc.language.TranslationRegistryInitializer;
import com.zenith.mc.map.MapBlockColorManager;
import com.zenith.module.ModuleManager;
import com.zenith.network.server.handler.player.InGameCommandManager;
import com.zenith.plugins.PluginManager;
import com.zenith.terminal.TerminalManager;
import com.zenith.util.Config;
import com.zenith.util.ConfigVerifier;
import com.zenith.util.LaunchConfig;
import com.zenith.util.Wait;
import com.zenith.via.ZenithViaInitializer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Shared {
    public static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule());
    public static final Logger DEFAULT_LOG = LoggerFactory.getLogger("Proxy");
    public static final Logger AUTH_LOG = LoggerFactory.getLogger("Auth");
    public static final Logger CACHE_LOG = LoggerFactory.getLogger("Cache");
    public static final Logger CLIENT_LOG = LoggerFactory.getLogger("Client");
    public static final Logger CHAT_LOG = LoggerFactory.getLogger("Chat");
    public static final Logger MODULE_LOG = LoggerFactory.getLogger("Module");
    public static final Logger SERVER_LOG = LoggerFactory.getLogger("Server");
    public static final Logger DISCORD_LOG = LoggerFactory.getLogger("Discord");
    public static final Logger DATABASE_LOG = LoggerFactory.getLogger("Database");
    public static final Logger TERMINAL_LOG = LoggerFactory.getLogger("Terminal");
    public static final Logger PLUGIN_LOG = LoggerFactory.getLogger("Plugin");
    public static final Logger PATH_LOG = LoggerFactory.getLogger("Pathfinder");
    public static final File CONFIG_FILE = new File("config.json");
    public static final File LAUNCH_CONFIG_FILE = new File("launch_config.json");
    public static final Config CONFIG;
    public static final LaunchConfig LAUNCH_CONFIG;
    public static final DataCache CACHE;
    public static final DiscordBot DISCORD;
    public static final SimpleEventBus EVENT_BUS;
    public static final ScheduledExecutorService EXECUTOR;
    public static final PlayerListsManager PLAYER_LISTS;
    public static final BlockDataManager BLOCK_DATA;
    public static final EntityDataManager ENTITY_DATA;
    public static final MapBlockColorManager MAP_BLOCK_COLOR;
    public static final DimensionDataManager DIMENSION_DATA;
    public static final DatabaseManager DATABASE;
    public static final TPSCalculator TPS;
    public static final ModuleManager MODULE;
    public static final InputManager INPUTS;
    public static final TerminalManager TERMINAL;
    public static final InGameCommandManager IN_GAME_COMMAND;
    public static final CommandManager COMMAND;
    public static final PlayerInventoryManager INVENTORY;
    public static final ZenithViaInitializer VIA_INITIALIZER;
    public static final PluginManager PLUGIN_MANAGER;
    public static synchronized Config loadConfig() {
        try {
            DEFAULT_LOG.info("Loading config...");

            Config config;
            if (CONFIG_FILE.exists()) {
                try (Reader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, Config.class);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load config!", e);
                }
            } else {
                config = new Config();
            }

            DEFAULT_LOG.info("Config loaded.");
            return config;
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to load config!", e);
            DEFAULT_LOG.error("config.json must be manually fixed or deleted");
            DEFAULT_LOG.error("Shutting down in 10s");
            Wait.wait(10);
            System.exit(1);
            return null;
        }
    }

    public static synchronized LaunchConfig loadLaunchConfig() {
        try {
            DEFAULT_LOG.info("Loading launch config...");

            LaunchConfig config = null;
            if (LAUNCH_CONFIG_FILE.exists()) {
                try (Reader reader = new FileReader(LAUNCH_CONFIG_FILE)) {
                    config = GSON.fromJson(reader, LaunchConfig.class);
                } catch (IOException e) {
                    DEFAULT_LOG.error("Unable to load launch config. Writing default config", e);
                    config = new LaunchConfig();
                }
            } else {
                config = new LaunchConfig();
            }
            DEFAULT_LOG.info("Launch config loaded.");
            return config;
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to load launch config!", e);
            DEFAULT_LOG.error("launch_config.json must be manually fixed or deleted");
            DEFAULT_LOG.error("Shutting down in 10s");
            Wait.wait(10);
            System.exit(1);
            return null;
        }
    }

    public static @Nullable String getExecutableCommit() {
        return readResourceTxt("zenith_commit.txt");
    }

    public static @Nullable String getExecutableReleaseVersion() {
        return readResourceTxt("zenith_release.txt");
    }

    private static @Nullable String readResourceTxt(final String name) {
        try (InputStream in = Shared.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            return null;
        }
    }
    public static void saveConfigAsync() {
        Thread.ofVirtual().name("Async Config Save").start(Shared::saveConfig);
    }

    public static synchronized void saveConfig() {
        saveConfig(CONFIG_FILE, CONFIG);
        PLUGIN_MANAGER.getPluginConfigurations().forEach((name, config) -> {
            File configFile = new File("plugins/" + name + ".json");
            saveConfig(configFile, config.instance());
        });
    }
    public static synchronized void saveLaunchConfig() {
        saveConfig(LAUNCH_CONFIG_FILE, LAUNCH_CONFIG);
    }

    static void saveConfig(File file, Object config) {
        DEFAULT_LOG.debug("Saving {}...", file.getName());

        if (config == null) {
            DEFAULT_LOG.error("Cannot save unloaded config");
            return;
        }

        try {
            final File tempFile = File.createTempFile(file.getName(), null);
            try (Writer out = new FileWriter(tempFile)) {
                GSON.toJson(config, out);
            }
            Files.move(tempFile, file);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save config!", e);
        }

        DEFAULT_LOG.debug("Config {} saved.", file.getName());
    }

    static {
        try {
            Thread.setDefaultUncaughtExceptionHandler(
                (thread, e) -> DEFAULT_LOG.error("Uncaught exception in thread {}", thread, e));
            EXECUTOR = Executors.newScheduledThreadPool(4, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Scheduled Executor - #%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler((thread, e) -> DEFAULT_LOG.error("Uncaught exception in scheduled executor thread {}", thread, e))
                .build());
            EVENT_BUS = new SimpleEventBus(Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Async EventBus - #%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler((thread, e) -> DEFAULT_LOG.error("Uncaught exception in event bus thread {}", thread, e))
                .build()), DEFAULT_LOG);
            DISCORD = new DiscordBot();
            DIMENSION_DATA = new DimensionDataManager();
            CACHE = new DataCache();
            PLAYER_LISTS = new PlayerListsManager();
            BLOCK_DATA = new BlockDataManager();
            ENTITY_DATA = new EntityDataManager();
            MAP_BLOCK_COLOR = new MapBlockColorManager();
            DATABASE = new DatabaseManager();
            TPS = new TPSCalculator();
            MODULE = new ModuleManager();
            INPUTS = new InputManager();
            TERMINAL = new TerminalManager();
            IN_GAME_COMMAND = new InGameCommandManager();
            COMMAND = new CommandManager();
            INVENTORY = new PlayerInventoryManager();
            VIA_INITIALIZER = new ZenithViaInitializer();
            TranslationRegistryInitializer.registerAllTranslations();
            CONFIG = loadConfig();
            LAUNCH_CONFIG = loadLaunchConfig();
            ConfigVerifier.verifyConfigs();
            PLUGIN_MANAGER = new PluginManager();
            PLAYER_LISTS.init(); // must be init after config
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to initialize!", e);
            throw e;
        }
    }

}
