package com.tenpo.challenge.application.callhistory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CallHistoryApplicationBoundaryTest {

  private static final Pattern FORBIDDEN_IMPORT =
      Pattern.compile(
          "^\\s*import\\s+(org\\.springframework\\.(web|data|scheduling|core\\.task)|jakarta\\.persistence|"
              + "javax\\.persistence|org\\.hibernate|org\\.postgresql)\\.");

  @Test
  void applicationClassesDoNotImportFrameworkPersistenceOrAdapterTypes() throws IOException {
    Path sourceRoot =
        Path.of(System.getProperty("user.dir"))
            .resolve("src/main/java/com/tenpo/challenge/application");

    List<String> forbiddenImports;
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      forbiddenImports =
          files
              .filter(path -> path.toString().endsWith(".java"))
              .flatMap(CallHistoryApplicationBoundaryTest::forbiddenImports)
              .toList();
    }

    assertThat(forbiddenImports).isEmpty();
  }

  private static Stream<String> forbiddenImports(Path path) {
    try {
      return Files.readAllLines(path).stream()
          .filter(line -> FORBIDDEN_IMPORT.matcher(line).find())
          .map(line -> path.getFileName() + ": " + line.trim());
    } catch (IOException ex) {
      throw new IllegalStateException("Could not inspect " + path, ex);
    }
  }
}
