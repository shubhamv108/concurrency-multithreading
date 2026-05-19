package multithreading.falsesharing;

/*
 https://jenkov.com/tutorials/java-concurrency/false-sharing.html
 */
//@jdk.internal.vm.annotation.Contended
class Counter {
    // can pad fields inside classes with empty bytes (after the field - when stored in RAM), so that the fields inside an object of that class are not stored within the same CPU cache line
//    @jdk.internal.vm.annotation.Contended
//    public volatile long count1 = 0;
//    public volatile long count2 = 0;

    @jdk.internal.vm.annotation.Contended("group1")
    public volatile long count1 = 0;

    @jdk.internal.vm.annotation.Contended("group1")
    public volatile long count2 = 0;

    @jdk.internal.vm.annotation.Contended("group2")
    public volatile long count3 = 0;
}

public class FalseSharing {

    public static void main(String[] args) {
        Counter counter1 = new Counter();
        Counter counter2 = counter1; // 32000 // @Contended = 28000
//        Counter counter2 = new Counter(); // 6500 // @Contended =

        long iterations = 1_000_000_000;

        Thread thread1 = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for (long i=0; i < iterations; ++i)
                ++counter1.count1;
            long endTime = System.currentTimeMillis();
            System.out.println("total time: " + (endTime - startTime));
        });
        Thread thread2 = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for (long i = 0; i < iterations; ++i)
                ++counter2.count2;
            long endTime = System.currentTimeMillis();
            System.out.println("total time: " + (endTime - startTime));
        });

        thread1.start();
        thread2.start();
    }

}
