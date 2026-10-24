package com.cult.cryptids.dev;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LangSorter {
    public static void main(String... args) throws IOException {
        for (File file: Objects.requireNonNull(new File("src/main/resources/assets/cult_cryptids/lang").listFiles())) {
            String s;
            try (Stream<String> lines = Files.lines(file.toPath())) {
                s = "{\n" + lines.filter(line -> line.length() > 1).map(line -> line.replace(",", ""))
                        .sorted().collect(Collectors.joining(",\n")) + "\n}";
            }
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(s.getBytes());
            }
        }
    }
}
