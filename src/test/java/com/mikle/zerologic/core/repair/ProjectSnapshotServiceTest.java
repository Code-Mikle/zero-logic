package com.mikle.zerologic.core.repair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSnapshotServiceTest {
    @TempDir Path tempDir;
    private final ProjectSnapshotService service = new ProjectSnapshotService();

    @Test
    void restoresProtectedFilesAndReportsSourceChanges() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("package.json"), "original");
        Files.writeString(tempDir.resolve("src/App.vue"), "before");
        var before = service.snapshot(tempDir);
        var protectedFiles = service.snapshotProtectedFiles(tempDir);

        Files.writeString(tempDir.resolve("package.json"), "malicious");
        Files.writeString(tempDir.resolve("src/App.vue"), "after");
        service.restoreProtectedFiles(tempDir, protectedFiles);

        assertEquals("original", Files.readString(tempDir.resolve("package.json")));
        assertEquals(java.util.List.of("src/App.vue"),
                service.changedFiles(before, service.snapshot(tempDir)));
    }

    @Test
    void restoresWholeProjectAfterFailedRepair() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/App.vue"), "before");
        var snapshot = service.snapshotContents(tempDir);
        Files.writeString(tempDir.resolve("src/App.vue"), "partial repair");
        Files.writeString(tempDir.resolve("src/New.vue"), "new file");

        service.restoreSnapshot(tempDir, snapshot);

        assertEquals("before", Files.readString(tempDir.resolve("src/App.vue")));
        assertFalse(Files.exists(tempDir.resolve("src/New.vue")));
    }
}
