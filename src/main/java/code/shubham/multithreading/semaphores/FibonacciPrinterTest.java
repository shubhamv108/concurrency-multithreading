package code.shubham.multithreading.semaphores;

import java.util.concurrent.Semaphore;

    /**
     * n threads, one thread should print 1 number a time in round robin
     */
public class FibonacciPrinterTest {
    class FibonacciPrinter implements Runnable {
        int id, n;
        Semaphore cur, next;
        public FibonacciPrinter(int id, int n, Semaphore cur, Semaphore next) {
            this.id  = id;
            this.n = n;
            this.cur = cur;
            this.next = next;
        }
        @Override
        public void run() {
            int prev = 0, curr = 1;
            for (int i = 0; i < 10; ++i) {
                int next = prev + curr;
                prev = curr;
                curr = next;

                if ((i % n) != id) {
                    continue;
                }

                try {
                    this.cur.acquire();
                    System.out.println(curr);
                    this.next.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class Solution {
        void printSeries(int n) {
            Semaphore[] s = new Semaphore[n+1];
            for (int i = 0; i < n; ++i)
                s[i] = new Semaphore(0);
            Thread[] threads = new Thread[n];
            s[0].release();
            for (int i = 0; i < n; ++i) {
                threads[i] = new Thread(new FibonacciPrinterTest.FibonacciPrinter(i, n, s[i%n], s[(i+1) % n]));
                threads[i].start();
            }
        }
    }

    public static void main(String[] args) {
        new FibonacciPrinterTest().new Solution().printSeries(5);
    }
}
