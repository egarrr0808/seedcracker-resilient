package kaptainwutax.seedcrackerX.cracker.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduledSetTest {
    @Test
    void committedSnapshotExcludesPendingValues() {
        ScheduledSet<String> values = new ScheduledSet<>(null);

        values.scheduleAdd("pending");
        assertEquals(0, values.committedSnapshot().size());
        assertEquals(1, values.snapshot().size());

        values.dump();
        assertEquals("pending", values.committedSnapshot().getFirst());
    }
}
