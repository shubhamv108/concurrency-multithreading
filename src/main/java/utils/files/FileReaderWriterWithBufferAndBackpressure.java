package utils.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Thread.sleep;

public class FileReaderWriterWithBufferAndBackpressure {

    private long backPressure;

    public FileReaderWriterWithBufferAndBackpressure(long backPressure) {
        this.backPressure = backPressure;
    }

    private final ConcurrentHashMap<Integer, String> lines = new ConcurrentHashMap<>();
    private final AtomicInteger readCounter = new AtomicInteger();
    private final AtomicInteger writeCounter = new AtomicInteger();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void read(String filePath) throws IOException {
        Files.lines(Paths.get(filePath))
                .peek(line -> {
                    while (readCounter.get() - writeCounter.get() > backPressure) {
                        try {
                            sleep(10000);
                            System.out.println("READWORKER->" + writeCounter.get() + " " + readCounter.get());
                            System.gc();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .forEach(line -> lines.put(readCounter.incrementAndGet(), line));
    }

    private String pollLine(int lineNumber) {
        return lines.remove(lineNumber);
    }

    public void executeWrite(String outputFilePath, ScheduledExecutorService service) throws IOException {
        File op = new File(outputFilePath);
        if (!op.exists())
            Files.createFile(op.toPath());

        service.scheduleAtFixedRate(() -> {
            try {
                write(outputFilePath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void write(String outputFilePath) throws FileNotFoundException {
        OutputStream outputStream = new FileOutputStream(outputFilePath, true);
        while (writeCounter.intValue() < readCounter.intValue()) {
            String nextLine = pollLine(writeCounter.incrementAndGet());
            write(nextLine, outputStream);
        }
    }

    public void write(String line, OutputStream os) {
        try {
            lock.writeLock().lock();
            os.write((line + "\n").getBytes());
            os.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (lock != null)
                lock.writeLock().unlock();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String inputFile = "/home/r00t/git/largefiles/6GB";
        String outputFile = "/home/r00t/git/largefiles/6GB.cp";
//        String inputFile = "build.gradle";
//        String outputFile = "build.gradle.cp";
        long backPressure = (long) 1e7;

        Files.deleteIfExists(Paths.get(outputFile));
        FileReaderWriterWithBufferAndBackpressure readerWriter = new FileReaderWriterWithBufferAndBackpressure(backPressure);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Thread readWorker = new Thread(() -> {
            try {
                readerWriter.read(inputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Thread writeWorker = new Thread(() -> {
            try {
                readerWriter.executeWrite(outputFile, scheduler);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        readWorker.start();
        writeWorker.start();

        System.out.println("Started awaiting for read worker to complete");
        readWorker.join();
        System.out.println("Completed awaiting for read worker to complete");

        while (readerWriter.writeCounter.get() < readerWriter.readCounter.get()) {
            System.out.println(readerWriter.writeCounter.get() + " " + readerWriter.readCounter.get());
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown...");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
