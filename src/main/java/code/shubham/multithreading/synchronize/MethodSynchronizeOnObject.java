package code.shubham.multithreading.synchronize;

import java.util.HashSet;
import java.util.Set;

public class MethodSynchronizeOnObject {
    Set<Object> set = new HashSet<>();

    synchronized void method1() {
        int i = 0;
        System.out.println("In method1");
        while (true) {
            System.out.println("1" + i++);
            try {
                if (i == 3)
                    return;
                Thread.sleep(10000l);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

     synchronized void method2() {
            System.out.println("In method2");
     }

    public static void main(String[] args) {
        MethodSynchronizeOnObject object = new MethodSynchronizeOnObject();
        Thread t1 = new Thread(() -> object.method1());
        Thread t2 = new Thread(() -> object.method2());
        t1.start();
        t2.start();;
    }
}
