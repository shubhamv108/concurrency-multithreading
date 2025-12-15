package code.shubham.collections;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentHashMap<K, V> {

    private class Node<K, V> {
        private K key;
        private V value;

        private int hashCode;

        private Node<K, V> next;

        private Node() {}

        private Node (final K key, final V value, final int hashCode) {
            this.key = key;
            this.value = value;
            this.hashCode = hashCode;
        }
    }


    private Node<K, V>[] table;
    private int capacity;

    private ReentrantReadWriteLock[] locks;

//    private float loadFactor = 0.75f;

    private final AtomicInteger size = new AtomicInteger();

    public ConcurrentHashMap(final int capacity) {
        this.capacity = capacity;
        this.table = new Node[capacity];
        this.locks = new ReentrantReadWriteLock[capacity];
        for (int i = 0; i < capacity; ++i) {
            this.table[i] = new Node<>();
            this.locks[i] = new ReentrantReadWriteLock();
        }
    }

    public V put(final K key, final V value) {
        if (key == null)
            throw new RuntimeException("key cannot be null");

        final int hashCode = Objects.hash(key);
        final int tableIndex = hashCode % this.table.length;

        try {
            this.locks[tableIndex].writeLock().lock();

            Node<K, V> temp = this.table[tableIndex];
            while (temp.next != null) {
                if (temp.next.hashCode == hashCode && temp.next.key.equals(key)) {
                    final V lastValue = temp.next.value;
                    temp.next.value = value;
                    return lastValue;
                }
                temp = temp.next;
            }

            temp.next = new Node<>(key, value, hashCode);
            this.size.incrementAndGet();

        } finally {
            this.locks[tableIndex].writeLock().unlock();
        }
        return null;
    }

    public V get(final K key) {
        if (key == null)
            return null;

        if (this.getSize() == 0)
            return null;

        final int hashCode = Objects.hash(key);
        final int tableIndex = hashCode % this.table.length;

        try {
            this.locks[tableIndex].readLock().lock();
            Node<K, V> node = this.table[tableIndex].next;
            while (node != null) {
                if (node.key.equals(key))
                    return node.value;
                node = node.next;
            }
        } finally {
            this.locks[tableIndex].readLock().unlock();
        }

        return null;
    }

    public V remove(final K key) {
        if (key == null)
            return null;

        if (this.getSize() == 0)
            return null;

        final int hashCode = Objects.hash(key);
        final int tableIndex = hashCode % this.table.length;

        if (this.table[tableIndex] == null)
            return null;

        try {
            this.locks[tableIndex].writeLock().lock();
            Node<K, V> temp = this.table[tableIndex];
            while (temp.next != null) {
                if (temp.next.hashCode == hashCode && temp.next.key.equals(key)) {
                    final V lastValue = temp.next.value;
                    temp.next = temp.next.next;
                    this.size.decrementAndGet();
                    return lastValue;
                }
                temp = temp.next;
            }
        } finally {
            this.locks[tableIndex].writeLock().unlock();
        }

        return null;
    }

    public int getSize() {
        return size.get();
    }

    public static void main(String[] args) throws InterruptedException {
        final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>(1);
        map.put("a", "b");
        System.out.println(map.get("a"));
        map.put("a", "c");
        System.out.println(map.get("a"));
        map.put("b", "d");
        System.out.println(map.get("a"));
        System.out.println(map.get("b"));

        Thread t1 = new Thread(() ->  map.put("a", "a"));
        Thread t2 = new Thread(() ->  map.put("a", "b"));
        Thread t3 = new Thread(() ->  map.put("a", "c"));
        Thread t4 = new Thread(() -> System.out.println(map.get("a")));

        t1.start();
        t2.start();
        t3.start();

        Thread.sleep(200);
        t4.start();

        System.out.println(map.getSize());
        map.remove("a");
        System.out.println(map.getSize());
        System.out.println(map.get("a"));
        System.out.println(map.get("b"));
    }

}
