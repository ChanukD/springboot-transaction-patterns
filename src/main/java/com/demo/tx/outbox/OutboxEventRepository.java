package com.demo.tx.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Lock a batch of unpublished events. SKIP LOCKED lets several pods run
     * the relay in parallel: each pod gets different rows, nobody waits.
     * Works on PostgreSQL, MySQL 8, Oracle and H2.
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE published_at IS NULL
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(int batchSize);

    long countByPublishedAtIsNull();
}
