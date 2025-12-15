package code.shubham.utils.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileSearchUtils {

    public static Collection<String> searchAccuratelyLineByLine(
            String filePath,
            String keyword,
            int workerCount)
            throws IOException, InterruptedException {
        if (!Files.exists(Paths.get(filePath)))
            return Collections.emptyList();

        if (workerCount < 1)
            throw new IllegalArgumentException(String.format("noWorkerPriveledgeProvided workerCount=%s", workerCount));

        Path path = Paths.get(filePath);
        long fileSize = Files.size(path);
        long chunkSize = Math.max(fileSize / workerCount, 1);

        Collection<FileChunkSearchTask> tasks = new ArrayList<>();
        for (int workerNumber = 0; workerNumber < workerCount; ++workerNumber) {
            long start = workerNumber * chunkSize;
            long end = (workerNumber == workerCount - 1) ? fileSize : (workerNumber + 1) * chunkSize;
            FileChunkSearchTask task = new FileChunkSearchTask(new File(filePath), start, end, keyword); // Allocate 1KB bufferCapacity
            tasks.add(task);
        }

        ExecutorService executorService= null;
        try {
            executorService = Executors.newFixedThreadPool(workerCount);
            Collection<Future<List<String>>> results = executorService.invokeAll(tasks);
            return results.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .flatMap(Collection::stream)
                    .toList();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
                executorService.close();
            }
        }
    }

    public static Collection<String> searchInaccuratelyWithByteBuffer(
            String filePath,
            String keyword,
            int workerCount)
            throws IOException, InterruptedException {
        if (!Files.exists(Paths.get(filePath)))
            return Collections.emptyList();

        if (workerCount < 1)
            throw new IllegalArgumentException(String.format("noWorkerPriveledgeProvided workerCount=%s", workerCount));

        Path path = Paths.get(filePath);
        long size = Files.size(path);
        long chunkSize = Math.max(size / workerCount, 1);

        Collection<Callable<List<String>>> tasks = new ArrayList<>();
        for (long offset = 0; offset < size; offset += chunkSize) {
            long finalOffset = offset;
            Callable<List<String>> task = () -> search(filePath, finalOffset, chunkSize, 1024, keyword); // Allocate 1KB bufferCapacity
            tasks.add(task);
        }

        ExecutorService executorService= null;
        try {
            executorService = Executors.newFixedThreadPool(workerCount);
            Collection<Future<List<String>>> results = executorService.invokeAll(tasks);
            return results.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .flatMap(Collection::stream)
                    .toList();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
                executorService.close();
            }
        }
    }

    public static List<String> search(
            String filePath,
            long offsetInBytes,
            long remainingBytes,
            int bufferCapacity,
            String keyword)
            throws IOException {
        if (!Files.exists(Paths.get(filePath)))
            return Collections.emptyList();

        Long start = System.nanoTime();

        ArrayList<String> foundInLines = new ArrayList<>();

        StringBuilder line = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             FileChannel fileChannel = fis.getChannel()) {

            // Offset for reading file
            fis.skip(offsetInBytes);

            int byteBufferCapacity = remainingBytes < bufferCapacity ? (int) remainingBytes : bufferCapacity;
            ByteBuffer buffer = ByteBuffer.allocate(bufferCapacity);
            while (fileChannel.read(buffer) > 0) {
                buffer.flip(); // Switch to read mode
                while (buffer.hasRemaining() ) {
                    // Process the data (e.g., print characters)
                    line.append((char) buffer.get());
                }
                buffer.clear();  // Clear for the next readIndexOf

                Arrays.stream(line.toString().split("\n"))
                        .filter(l -> l.contains(keyword))
                        .forEach(foundInLines::add);

                line.setLength(0);

                remainingBytes -= byteBufferCapacity;
                if (remainingBytes <= 0)
                    break;
            }
        }
        System.out.println(String.format("startBytes=%s totalTime=%s", offsetInBytes, System.nanoTime() - start));

        return foundInLines;
    }

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        String filePath = "/home/r00t/git/largefiles/6GB";
        Long start = System.nanoTime();
//        System.out.println(FileSearchUtils.searchAccuratelyLineByLine(filePath, "LAST_LINE", 4)); // 323.457543813 seconds
//        System.out.println(FileSearchUtils.searchInaccuratelyWithByteBuffer(filePath, "LAST_LINE", 10)); // 6.6 seconds
        System.out.println(FileSearchUtils.search(filePath, 0, Files.size(Path.of(filePath)), 1024, "LAST_LINE")); // 31.229373737
        System.out.println(System.nanoTime() - start);
    }
}
