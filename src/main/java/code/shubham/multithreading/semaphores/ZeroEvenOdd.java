package code.shubham.multithreading.semaphores;

import java.util.concurrent.Semaphore;
import java.util.function.IntConsumer;
import java.util.stream.*;

class ZeroEvenOdd {
    private int n;
    Semaphore zeroSemaphore;
    Semaphore evenSemaphore;
    Semaphore oddSemaphore;

    public ZeroEvenOdd(int n) {
        this.n = n;
        this.zeroSemaphore = new Semaphore(0);
        this.evenSemaphore = new Semaphore(0);
        this.oddSemaphore = new Semaphore(0);
        this.zeroSemaphore.release();
    }

    // printNumber.accept(x) outputs "x", where x is an integer.
    public void zero(IntConsumer printNumber) throws InterruptedException {
        for (int __ = 1; __ <= n; __++) {
            zeroSemaphore.acquire();
            printNumber.accept(0);
            if ((__ & 1) == 0) evenSemaphore.release();
            else oddSemaphore.release();
        }
    }

    public void even(IntConsumer printNumber) throws InterruptedException {
        for (int __ = 2; __ <= n; __ = __ + 2) {
            evenSemaphore.acquire();
            printNumber.accept(__);
            zeroSemaphore.release();
        }
    }

    public void odd(IntConsumer printNumber) throws InterruptedException {
        for (int __ = 1; __ <= n; __ = __ + 2) {
            oddSemaphore.acquire();
            printNumber.accept(__);
            zeroSemaphore.release();
        }
    }

    public static void main(String[] args) {
        IntConsumer printer = (n) -> System.out.print(n + " ");
        ZeroEvenOdd zeroEvenOdd = new ZeroEvenOdd(10);
        Thread t1 = new Thread(() -> {
            try {
                zeroEvenOdd.zero(printer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                zeroEvenOdd.even(printer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                zeroEvenOdd.odd(printer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t3.start();
        t2.start();
    }

}