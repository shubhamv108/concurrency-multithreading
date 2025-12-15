package code.shubham.processes;

public class ProcessUtils {

    public static long pid() {
        return ProcessHandle.current().pid();
    }

    public static boolean isRunning(final long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

}
