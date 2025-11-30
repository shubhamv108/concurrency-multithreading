package multithreading.waitgroup;

import java.util.concurrent.Phaser;

public class WaitGroup {

    private final Phaser phaser = new Phaser();

    public void add(int n) {
        this.phaser.arrive();
    }

    public void await() {
        try {
            this.phaser.wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {

    }

}
