package code.shubham.multithreading.semaphores;

import java.util.concurrent.Semaphore;

class Shared {
    static int count = 0;
}

class MyThread extends Thread {
    // A semaphore controls access to a shared resource through the use of a counter. (Counting Semaphore)
    Semaphore semaphore;
    String threadName;
    public MyThread(final Semaphore semaphore, final String threadName) {
        super(threadName);
        this.semaphore = semaphore;
        this.threadName = threadName;
    }

    @Override
    public void run() {
        if(this.getName().equals("A")) {
            System.out.println("Starting " + threadName);
            try {
                System.out.println(threadName + " is waiting for a permit.");
                semaphore.acquire();

                System.out.println(threadName + " gets a permit.");
                for(int i = 0; i < 5; i++) {
                    Shared.count++;
                    System.out.println(threadName + ": sem" + Shared.count);
                    Thread.sleep(10);
                }
            } catch (InterruptedException exc) {
                System.out.println(exc);
            }
            System.out.println(threadName + " releases the permit.");
            semaphore.release();
        } else {
            System.out.println("Starting " + threadName);
            try {
                System.out.println(threadName + " is waiting for a permit.");
                semaphore.acquire();
                System.out.println(threadName + " gets a permit.");
                for(int i=0; i < 5; i++) {
                    Shared.count--;
                    System.out.println(threadName + ": " + Shared.count);

                    Thread.sleep(10);
                }
            } catch (InterruptedException exc) {
                System.out.println(exc);
            }
            System.out.println(threadName + " releases the permit.");
            semaphore.release();
        }
    }
}

/**
 * https://www.geeksforgeeks.org/semaphores-in-process-synchronization/
 */
public class SemaphoreDemo {
    public static void main(String args[]) throws InterruptedException {
        Semaphore semaphore = new Semaphore(1/*, true*/);

        MyThread mt1 = new MyThread(semaphore, "A");
        MyThread mt2 = new MyThread(semaphore, "B");

        mt1.start();
        mt2.start();

        mt1.join();
        mt2.join();

        System.out.println("count: " + Shared.count);
    }
}