package code.shubham.multithreading.forkjoinpool;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

public class Main {
    public static void main(String[] args) {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
    }
}
