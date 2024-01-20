package deque;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {
    private int nextFirst;
    private int nextLast;
    private int size;
    private T[] items = (T[]) new Objects[8];

    public ArrayDeque() {
        nextFirst = 3;
        nextLast = 4;
        size = 0;
    }

    public ArrayDeque(T item) {
        items[3] = item;
        nextFirst = 2;
        nextLast = 4;
        size = 1;
    }

    @Override
    public void addFirst(T item) {
        items[nextFirst] = item;
        size += 1;
        nextFirst -= 1;
        if (nextFirst == -1) {
            resize(size * 2);
        }
    }

    @Override
    public void addLast(T item) {
        items[nextLast] = item;
        size += 1;
        nextLast += 1;
        if (nextLast == items.length) {
            resize(size * 2);
        }
    }

    @Override
    public int size() {
        return size;
    }

    private void downsize() {
        if (isEmpty()) {
            resize(8);
        }
        if (size < items.length / 4 && items.length >= 16) {
            resize(size * 2);
        }
    }

    private void resize(int newSize) {
        T[] newItems = (T[]) new Objects[newSize];
        int firstPos = Math.abs(newSize - size) / 2;
        System.arraycopy(items, nextFirst + 1, newItems, firstPos, size);
        items = newItems;
        nextFirst = firstPos - 1;
        nextLast = firstPos + size;
    }

    @Override
    public void printDeque() {
        int count = this.size();
        int p = nextFirst + 1;
        if (count == 1){
            System.out.println(items[p]);
            return;
        }
        while(count > 1){
            System.out.print(items[p] + " ");
            p = (p + 1) % items.length;
            count -= 1;
        }
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        nextFirst += 1;
        T item = items[nextFirst];
        items[nextFirst] = null;
        size -= 1;
        downsize();
        return item;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        nextLast -= 1;
        T item = items[nextLast];
        items[nextLast] = null;
        size -= 1;
        downsize();
        return item;
    }

    @Override
    public T get(int index) {
        if (isEmpty()) {
            return null;
        }
        if (index < 0 || index >= size) {
            return null;
        }
        int itemIndex = nextFirst + 1 + index;
        return items[itemIndex];
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof ArrayDeque)) {
            return false;
        }
        ArrayDeque<T> other = (ArrayDeque<T>) o;
        if (other.size() != this.size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (other.get(i) != this.get(i)) {
                return false;
            }
        }
        return true;
    }

    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int index;

        ArrayDequeIterator() {
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public T next() {
            T item = get(index);
            index += 1;
            return item;
        }
    }
}
