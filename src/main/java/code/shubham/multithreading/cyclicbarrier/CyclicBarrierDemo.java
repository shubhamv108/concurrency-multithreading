package code.shubham.multithreading.cyclicbarrier;

import java.util.concurrent.CyclicBarrier;

/**
 * CyclicBarrier is used when multiple thread carry out different sub tasks and
 * the output of these sub tasks need to be combined to form the final output
 *
 * https://www.geeksforgeeks.org/java-util-concurrent-cyclicbarrier-java/
 */
public class CyclicBarrierDemo {

    public static void main(String[] args) {
        CyclicBarrier barrier = new CyclicBarrier(5, () -> System.out.println("All threads reached a barrier"));
        System.out.println(barrier.getParties());
        barrier.reset();
        System.out.println(barrier.isBroken());
        System.out.println(barrier.getNumberWaiting());
    }

}
