package multithreading;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockDemo {

    private final ConcurrentHashMap<String, ReadWriteLock> locks = new ConcurrentHashMap();

    public static ReadWriteLockDemo getInstance() {
        return ReadWriteLockDemo.SingletonHolder.INSTANCE;
    }
    private static final class SingletonHolder {
        private static final ReadWriteLockDemo INSTANCE = new ReadWriteLockDemo();
    }

    public void doTransaction(final ReadWriteLockDemo.Account account, final Double amount) {
        try {
            this.getLock(account.number).lock();
            System.out.println(String.format("Executing Transaction for Account: %s Amount: ", account.number, amount));
            System.out.println(String.format("Before balance for Account: %s = %s", account.number, account.balance));
            account.balance += amount;
            System.out.println(String.format("After balance for Account: %s = %s", account.number, account.balance));
            System.out.println(String.format("Executed Transaction for Account: %s Amount: %s", account.number, amount));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.getLock(account.number).unlock();
        }
    }

    public void getBalance(final ReadWriteLockDemo.Account account) {
        try {
            this.getLock(account.number).RLock();
            System.out.println(String.format("balance for Account: %s = %s", account.number, account.balance));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.getLock(account.number).RUnlock();
        }
    }

    private ReadWriteLock getLock(final String accountNumber) {
        var lock = this.locks.get(accountNumber);
        if (lock == null) {
            synchronized (accountNumber) {
                lock = locks.get(accountNumber);
                if (lock == null) {
                    this.locks.put(accountNumber, lock = new ReadWriteLock());
                }
            }
        }
        return lock;
    }


    public static void main(String[] args) throws InterruptedException {
        var onObject = ReadWriteLockDemo.getInstance();

       Account account1 = new Account("1");
       Account account2 = new Account("2");
       Account account3 = new Account("3");
       Account account4 = new Account("4");

        Thread readAccount1 = new Thread(
                () -> {
                    int i = 0;
                    while (i++ < 100) {
                        onObject.getBalance(account1);
                    }
                }
        );

        readAccount1.start();
        Thread thread1Account1 = new Thread(() -> onObject.doTransaction(account1, 3.0));
        Thread thread2Account1 = new Thread(() -> onObject.doTransaction(account1, -2.0));
        Thread thread3Account1 = new Thread(() -> onObject.doTransaction(account1, 10.0));
        Thread thread4Account1 = new Thread(() -> onObject.doTransaction(account1, -13.0));

        Thread thread1Account2 = new Thread(() -> onObject.doTransaction(account2, 3.0));
        Thread thread2Account2 = new Thread(() -> onObject.doTransaction(account2, -2.0));
        Thread thread3Account2 = new Thread(() -> onObject.doTransaction(account2, 10.0));
        Thread thread4Account2 = new Thread(() -> onObject.doTransaction(account2, -13.0));

        Thread thread1Account3 = new Thread(() -> onObject.doTransaction(account3, 3.0));
        Thread thread2Account3 = new Thread(() -> onObject.doTransaction(account3, -2.0));
        Thread thread3Account3 = new Thread(() -> onObject.doTransaction(account3, 10.0));
        Thread thread4Account3 = new Thread(() -> onObject.doTransaction(account3, -13.0));

        thread1Account1.start();
        thread2Account1.start();
        thread3Account1.start();
        thread4Account1.start();

        thread1Account2.start();
        thread2Account2.start();
        thread3Account2.start();
        thread4Account2.start();

        thread1Account3.start();
        thread2Account3.start();
        thread3Account3.start();
        thread4Account3.start();


        thread1Account1.join();
        thread2Account1.join();
        thread3Account1.join();
        thread4Account1.join();
        thread1Account2.join();
        thread2Account2.join();
        thread3Account2.join();
        thread4Account2.join();
        thread1Account3.join();
        thread2Account3.join();
        thread3Account3.join();
        thread4Account3.join();

        System.out.println("Balance Account1: " + account1.balance);
        System.out.println("Balance Account1: " + account2.balance);
        System.out.println("Balance Account1: " + account3.balance);
    }

    private static class Account {
        String number;
        volatile Double balance = 0D;
        Account(String number) {
            this.number = number;
        }
    }

}
