package utils;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

    public static Path createFileIfNotExists(final String filePath) {
        final Path path = Path.of(filePath);
        if (Files.exists(path))
            return path;

        synchronized (filePath) {
            if (Files.exists(path))
                return path;

            try {
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }

                return Files.createFile(path);
            } catch (final FileAlreadyExistsException exception) {
            } catch (final IOException exception) {
                exception.printStackTrace();
            }
        }
        return path;
    }
}
