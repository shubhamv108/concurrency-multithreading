package code.shubham.multithreading;

public class PrintEvenOddWithTwoThreads {

    public static void main(String... args) {
        OddEvenPrinter print = new OddEvenPrinter();
        Thread t1 = new Thread(new PrintOddEvenRunnable(print, false));
        Thread t2 = new Thread(new PrintOddEvenRunnable(print, true));
        t1.start();
        t2.start();
    }

}

class PrintOddEvenRunnable implements Runnable {

    private OddEvenPrinter print;
    private boolean isEvenNumber;

    public PrintOddEvenRunnable(final OddEvenPrinter print, final boolean isEvenNumber) {
        this.print = print;
        this.isEvenNumber = isEvenNumber;
    }

    @Override
    public void run() {
        int number = isEvenNumber == true ? 2 : 1;
        while (number <= 10) {
            if (isEvenNumber) {
                print.printEven(number);
            } else {
                print.printOdd(number);
            }
            number += 2;
        }
    }
}

class OddEvenPrinter {

    boolean isEven = false;

    synchronized void printEven(int number) {
        while (isEven == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.print(number + " ");
        isEven = false;
        notifyAll();
    }

    synchronized void printOdd(int number) {
        while (isEven == true) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.print(number + " ");
        isEven = true;
        notifyAll();
    }

}