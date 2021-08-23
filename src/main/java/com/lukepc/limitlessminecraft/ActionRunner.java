package com.lukepc.limitlessminecraft;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record ActionRunner(String classId, String code) {
    public boolean compile(File tempDir) {
        File sourceFile = new File(tempDir, "Action" + classId + ".java");
        try {
            FileWriter sourceWriter = new FileWriter(sourceFile);
            sourceWriter.write(code);
            sourceWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        List<File> sourceFiles = List.of(sourceFile);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                null, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);

        List<String> compilerOptions = new ArrayList<>();
        try {
            URI serverJarUri = Bukkit.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            String serverJar = new File(serverJarUri).getPath();

            compilerOptions.add("-classpath");
            compilerOptions.add(serverJar);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, compilerOptions, null, compilationUnits);
        return task.call();
    }

    public boolean run(Player player, JavaPlugin plugin, File tempDir, Consumer<Boolean> callback) {
        URL[] urls;
        try {
            urls = new URL[]{tempDir.toURI().toURL()};
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        ClassLoader classLoader = getClass().getClassLoader();
        URLClassLoader urlClassLoader = new URLClassLoader(urls, classLoader);
        try {
            Class<?> actionClass = urlClassLoader.loadClass("Action" + classId);
            Method actionMethod = actionClass.getMethod("runAction", Player.class, World.class, Server.class);
            plugin.getServer().getScheduler().runTask(plugin, task -> {
                try {
                    actionMethod.invoke(null, player, player.getWorld(), plugin.getServer());
                    callback.accept(true);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    callback.accept(false);
                }
            });
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
