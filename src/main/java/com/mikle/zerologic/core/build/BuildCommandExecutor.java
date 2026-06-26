package com.mikle.zerologic.core.build;

import com.mikle.zerologic.core.build.model.CommandResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface BuildCommandExecutor {
    CommandResult execute(Path workingDirectory, List<String> command, Duration timeout);
}
