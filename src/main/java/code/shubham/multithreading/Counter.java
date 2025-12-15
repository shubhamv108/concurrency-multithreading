package code.shubham.multithreading;

public class Counter implements Runnable {

    static int counter = 0;

    public static void main(String[] args) {
        Thread t1 = new Thread(new Counter());
        Thread t2 = new Thread(new Counter());
        t1.start();
        t2.start();
        System.out.println(counter);
    }

    @Override
    public void run() {
        for (int i = 0; i < 1e7; ++i)
            ++this.counter;
    }
}
