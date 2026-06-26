package com.mikle.zerologic.core.repair;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProjectSnapshotService {
    private static final Set<String> IGNORED_DIRS = Set.of("node_modules", "dist", ".git", "coverage");
    private static final Set<String> PROTECTED_FILES = Set.of(
            "package.json", "package-lock.json", "pnpm-lock.yaml", "yarn.lock",
            "vite.config.js", "vite.config.ts", "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
            ".env", ".env.local", ".env.production", ".npmrc");

    public Map<String, String> snapshot(Path root) throws IOException {
        Map<String, String> snapshot = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !isIgnored(root.relativize(path)))
                    .forEach(path -> snapshot.put(normalize(root.relativize(path)), hash(path)));
        }
        return snapshot;
    }

    public Map<String, byte[]> snapshotProtectedFiles(Path root) throws IOException {
        Map<String, byte[]> snapshot = new LinkedHashMap<>();
        for (String file : PROTECTED_FILES) {
            Path path = root.resolve(file);
            if (Files.isRegularFile(path)) {
                snapshot.put(file, Files.readAllBytes(path));
            }
        }
        return snapshot;
    }

    public Map<String, byte[]> snapshotContents(Path root) throws IOException {
        Map<String, byte[]> snapshot = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> !isIgnored(root.relativize(file))).toList()) {
                snapshot.put(normalize(root.relativize(path)), Files.readAllBytes(path));
            }
        }
        return snapshot;
    }

    public void restoreSnapshot(Path root, Map<String, byte[]> snapshot) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> !isIgnored(root.relativize(file))).toList()) {
                String relative = normalize(root.relativize(path));
                if (!snapshot.containsKey(relative)) {
                    Files.deleteIfExists(path);
                }
            }
        }
        for (Map.Entry<String, byte[]> entry : snapshot.entrySet()) {
            Path path = root.resolve(entry.getKey()).normalize();
            Files.createDirectories(path.getParent());
            Files.write(path, entry.getValue());
        }
    }

    public void restoreProtectedFiles(Path root, Map<String, byte[]> snapshot) throws IOException {
        for (String file : PROTECTED_FILES) {
            Path path = root.resolve(file);
            byte[] content = snapshot.get(file);
            if (content == null) {
                Files.deleteIfExists(path);
            } else if (!Files.exists(path) || !java.util.Arrays.equals(content, Files.readAllBytes(path))) {
                Files.write(path, content);
            }
        }
    }

    public List<String> changedFiles(Map<String, String> before, Map<String, String> after) {
        Set<String> all = new java.util.TreeSet<>(before.keySet());
        all.addAll(after.keySet());
        return all.stream().filter(path -> !java.util.Objects.equals(before.get(path), after.get(path))).toList();
    }

    private boolean isIgnored(Path relative) {
        for (Path part : relative) {
            if (IGNORED_DIRS.contains(part.toString())) return true;
        }
        return false;
    }

    private String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private String hash(Path path) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash project file: " + path, e);
        }
    }
}
