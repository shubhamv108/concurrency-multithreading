package processes;

public class ProcessID {
    public static void main(String[] args) {
        System.out.println("PID: " + ProcessHandle.current().pid());
    }
}
