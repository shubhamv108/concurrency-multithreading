package multithreading.semaphores;

public class PrintOddEvenUsingTwoThreads {

    public static void main(String[] args) {

    }

}

class PrintOddEvenRunnable implements Runnable {

    @Override
    public void run() {
        int a;
        if ("ThreadOdd".equals(Thread.currentThread().getName())) {
            a = 1;
        }
        if ("ThreadEven".equals(Thread.currentThread().getName())) {
            a = 0;
        }


    }

}