package com.mikle.zerologic.generation.build.executor;

import com.mikle.zerologic.generation.build.model.result.CommandResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface BuildCommandExecutor {
    CommandResult execute(Path workingDirectory, List<String> command, Duration timeout);
}
