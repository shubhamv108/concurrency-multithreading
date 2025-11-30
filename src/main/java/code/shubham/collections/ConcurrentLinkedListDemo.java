package collections;

import java.util.concurrent.ConcurrentLinkedDeque;

public class ConcurrentLinkedListDemo {

    ConcurrentLinkedDeque<Integer> q = new ConcurrentLinkedDeque<>();

    void action(int operation, int n) throws InterruptedException {
        if (1 == operation) {
            Thread.sleep(1000);
            System.out.println(Thread.currentThread().getName());
            q.offer(n);
        } else if (2 == operation) {
            q.pollFirst();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ConcurrentLinkedListDemo l = new ConcurrentLinkedListDemo();
        Thread t1 = new Thread(() -> {
            try {
                l.action(1, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                l.action(1, 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Thread t3 = new Thread(() -> {
            try {
                l.action(1, 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Thread t4 = new Thread(() -> {
            try {
                l.action(1, 9);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t4.start();
        t1.start();
        t3.start();
        t2.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        System.out.println(l.q);
    }

}
