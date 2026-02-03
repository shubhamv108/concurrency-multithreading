package code.shubham.multithreading.semaphores;

import java.util.concurrent.Semaphore;

/**
 * n threads, one thread should print 1 number a time in round robin
 */
public class FibonacciPrinterTest {
    public static void main(String[] args) {
        new FibonacciPrinterTest().new Solution().printSeries(5);
    }

    class Solution {
        void printSeries(int n) {
            Semaphore[] s = new Semaphore[n + 1];
            for (int i = 0; i < n; ++i)
                s[i] = new Semaphore(0);
            Thread[] threads = new Thread[n];
            s[0].release();
            for (int i = 0; i < n; ++i) {
                threads[i] = new Thread(new FibonacciPrinterTest().new Solution().new FibonacciPrinter(i, n, s[i % n], s[(i + 1) % n]));
                threads[i].start();
            }
        }

        private class FibonacciPrinter implements Runnable {
            int id, n;
            Semaphore cur, next;

            public FibonacciPrinter(int id, int n, Semaphore cur, Semaphore next) {
                this.id = id;
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
    }

    public class Solution2 {

        private static class SharedState {
            private final int totalThreads;
            private int currentTurn = 0;

            public SharedState(int totalThreads) {
                this.totalThreads = totalThreads;
            }

            public synchronized void waitForTurn(int threadId) throws InterruptedException {
                while (currentTurn != threadId)
                    wait();
            }

            public synchronized void nextTurn() {
                currentTurn = (currentTurn + 1) % totalThreads;
                notifyAll();
            }
        }

        class FibonacciPrinter implements Runnable {
            int id, n;
            SharedState state;

            public FibonacciPrinter(int id, int n, SharedState state) {
                this.id = id;
                this.n = n;
                this.state = state;
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
                        state.waitForTurn(id);
                        System.out.println(curr);
                        state.nextTurn();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            }
        }

        void printSeries(int n) {
            SharedState state = new SharedState(n);
            Thread[] threads = new Thread[n];

            for (int i = 0; i < n; ++i) {
                threads[i] = new Thread(new FibonacciPrinter(i, n, state));
                threads[i].start();
            }

            // Optional: wait for all threads to complete
            for (int i = 0; i < n; ++i) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
