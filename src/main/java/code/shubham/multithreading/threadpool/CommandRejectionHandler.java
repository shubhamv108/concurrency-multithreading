package multithreading.threadpool;

@FunctionalInterface
public interface CommandRejectionHandler {
    void handle(Runnable command);
}
