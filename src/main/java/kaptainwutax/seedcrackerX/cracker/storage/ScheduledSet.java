package kaptainwutax.seedcrackerX.cracker.storage;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.List;

public class ScheduledSet<T> implements Iterable<T> {

    protected final Set<T> baseSet;
    protected final Set<T> scheduledSet;

    public ScheduledSet(Comparator<T> comparator) {
        if (comparator != null) {
            this.baseSet = new TreeSet<>(comparator);
        } else {
            this.baseSet = new HashSet<>();
        }

        this.scheduledSet = new HashSet<>();
    }

    public synchronized void scheduleAdd(T e) {
        this.scheduledSet.add(e);
    }

    public synchronized void dump() {
        synchronized (this.baseSet) {
            this.baseSet.addAll(this.scheduledSet);
            this.scheduledSet.clear();
        }
    }

    public synchronized boolean contains(T e) {
        return this.baseSet.contains(e) || this.scheduledSet.contains(e);
    }

    public Set<T> getBaseSet() {
        return this.baseSet;
    }

    public synchronized List<T> snapshot() {
        List<T> result = new ArrayList<>(this.baseSet);
        for (T value : this.scheduledSet) {
            if (!result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public synchronized List<T> committedSnapshot() {
        return new ArrayList<>(this.baseSet);
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return this.baseSet.iterator();
    }

    public synchronized int size() {
        return this.baseSet.size();
    }

}
