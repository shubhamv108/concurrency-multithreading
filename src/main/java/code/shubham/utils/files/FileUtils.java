package code.shubham.utils.files;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static void cp(
            String inputFilePath,
            String outputFilePath)
            throws IOException {
        cp(inputFilePath, 0, outputFilePath, 1024);
    }

    public static void cp(
            String inputFilePath,
            String outputFilePath,
            int byteBufferCapacity)
            throws IOException {
        cp(inputFilePath, 0, outputFilePath, byteBufferCapacity);
    }

    public static void cp(
            String inputFilePath,
            long offsetInBytes,
            String outputFilePath,
            int byteBufferCapacity)
            throws IOException {
        Long start = System.nanoTime();

        Path outputPath = Paths.get(outputFilePath);
        Files.createDirectories(outputPath.getParent());
        FileUtils.createFileIfNotExists(outputFilePath);

        try (FileOutputStream fos = new FileOutputStream(outputFilePath);
             FileInputStream fis = new FileInputStream(inputFilePath);
             FileChannel fileChannel = fis.getChannel()) {

            // Offset for reading file
            fis.skip(offsetInBytes);

            ByteBuffer buffer = ByteBuffer.allocate(byteBufferCapacity); // Allocate 1KB buffer
            while (fileChannel.read(buffer) > 0) {
                buffer.flip(); // Switch to read mode
                while (buffer.hasRemaining() ) {
                    // Process the data (e.g., print characters)
                    fos.write(buffer.get());
                }
                fos.flush(); // force flush output stream to file
                buffer.clear();  // Clear for the next readIndexOf
            }
        }

        System.out.println(String.format("startBytes=%s totalTime=%s", offsetInBytes, System.nanoTime() - start));
    }
}
