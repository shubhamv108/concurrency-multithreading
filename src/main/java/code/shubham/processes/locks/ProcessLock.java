package code.shubham.processes.locks;

import code.shubham.utils.files.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class ProcessLock {
    private final File lockTempDir = new File("tmp" + File.separator + "locks");
    private FileLock fileLock = null;
    private final String key;

    public ProcessLock(final String key) {
        this.key = key;
        Runtime.getRuntime().addShutdownHook(new Thread(ProcessLock.this::release));
    }

    public void run(final Runnable successCallback) {
        this.run(successCallback, null);
    }

    public void run(final Runnable successCallback, final Runnable timeOutCallback) {
        try {
            if (this.acquire(Long.MAX_VALUE)) {
                successCallback.run();
            } else if (timeOutCallback != null) {
                timeOutCallback.run();
            }
        } finally {
            this.release();
        }
    }

    private boolean acquire(final long waitInMilliseconds) {
        if (this.fileLock == null && waitInMilliseconds > 0) {
            try {
                final long dropDeadTime = System.currentTimeMillis() + waitInMilliseconds;
                final File file = new File(this.lockTempDir,  this.key + ".lock");
                FileUtils.createFileIfNotExists(file.getAbsolutePath());
                final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                final FileChannel fileChannel = randomAccessFile.getChannel();
                this.fileLock = fileChannel.lock();
            } catch (final OverlappingFileLockException exception) {
                return this.acquire(waitInMilliseconds);
            }
            catch (final Exception exception) {
                exception.printStackTrace();
            }
        }
        return this.fileLock != null;
    }

    private void release() {
        if (this.fileLock != null) {
            try {
                this.fileLock.release();
                this.fileLock = null;
            } catch (final IOException exception) {
                exception.printStackTrace();
            }
        }
    }
}