package code.shubham.multithreading.semaphores;

import java.util.concurrent.Semaphore;

public class PrintOddEvenUsingTwoThreadsUsingSemaphores {

    Semaphore odd  = new Semaphore(0);
    Semaphore even = new Semaphore(0);

    PrintOddEvenUsingTwoThreadsUsingSemaphores() {
        even.release();
    }

    void printOdd() throws InterruptedException {
        for (int __ = 1; __ <= 10; __ = __ + 2) {
            even.acquire();
            System.out.print(__ + " ");
            odd.release();
        }
    }

    void printEven() throws InterruptedException {
        for (int __ = 2; __ <= 10; __ = __ + 2) {
            odd.acquire();
            System.out.print(__ + " ");
            even.release();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PrintOddEvenUsingTwoThreadsUsingSemaphores printer = new PrintOddEvenUsingTwoThreadsUsingSemaphores();
        Thread t1 = new Thread(() -> {
            try {
                printer.printEven();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                printer.printOdd();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
    }

}
