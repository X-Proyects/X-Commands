package com.fabian.xcommands.managers;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.LoggerUtils;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DependencyManager {

    private final XCommands plugin;
    private BukkitLibraryManager libraryManager;

    public DependencyManager(XCommands plugin) {
        this.plugin = plugin;
        this.libraryManager = new BukkitLibraryManager(plugin);
        setSharedLibraryPath();
        this.libraryManager.addMavenCentral();
        this.libraryManager.addSonatype();
        this.libraryManager.addRepository("https://repo.papermc.io/repository/maven-public/");
    }

    /**
     * Redirects libby's save directory to plugins/X-API/ so all plugins
     * share a single dependency folder instead of each having their own libs/.
     */
    private void setSharedLibraryPath() {
        try {
            Path sharedPath = Paths.get(plugin.getDataFolder().getParent(), "X-API");
            Files.createDirectories(sharedPath);
            Field field = LibraryManager.class.getDeclaredField("saveDirectory");
            field.setAccessible(true);
            field.set(libraryManager, sharedPath);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set shared library path (plugins/X-API/): " + e.getMessage());
        }
    }

    public void loadDependencies() {
        LoggerUtils.debug("Loading runtime dependencies via X-API...");
        try {
            plugin.logInfo("Loading runtime dependencies via X-API...");
            loadAdventureDependencies();
            plugin.logInfo("All dependencies loaded successfully!");
            LoggerUtils.debug("All dependencies loaded successfully");
        } catch (Exception e) {
            plugin.logSevere("Failed to load runtime dependencies! " + e.getMessage());
            LoggerUtils.debug("Dependency loading failed: " + e.getMessage());
        }
    }

    private void loadAdventureDependencies() {
        Library adventureApi = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-api")
                .version("4.14.0")
                .build();

        Library miniMessage = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-minimessage")
                .version("4.14.0")
                .build();

        Library legacySerializer = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-legacy")
                .version("4.14.0")
                .build();

        Library plainSerializer = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-text-serializer-plain")
                .version("4.14.0")
                .build();

        Library key = Library.builder()
                .groupId("net.kyori")
                .artifactId("adventure-key")
                .version("4.14.0")
                .build();

        Library examinationApi = Library.builder()
                .groupId("net.kyori")
                .artifactId("examination-api")
                .version("1.3.0")
                .build();

        Library examinationString = Library.builder()
                .groupId("net.kyori")
                .artifactId("examination-string")
                .version("1.3.0")
                .build();

        libraryManager.loadLibrary(adventureApi);
        libraryManager.loadLibrary(miniMessage);
        libraryManager.loadLibrary(legacySerializer);
        libraryManager.loadLibrary(plainSerializer);
        libraryManager.loadLibrary(key);
        libraryManager.loadLibrary(examinationApi);
        libraryManager.loadLibrary(examinationString);
    }
}