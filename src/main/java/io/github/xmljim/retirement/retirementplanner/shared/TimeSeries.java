/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable, append-only sequence of entries used as a building block
 * for cash-flow ledgers, balance histories, and (eventually) Monte
 * Carlo paths.
 *
 * <p>Operations return new {@code TimeSeries} instances; the underlying
 * list is defensively copied at construction. This is the project's
 * canonical &ldquo;running record&rdquo; type — the contribution
 * engine threads one through 12 months and queries it for year-to-date
 * totals instead of carrying an explicit accumulator (ADR-003 §"Engine
 * Behavior").
 *
 * <p>Entries are stored in insertion order. {@code TimeSeries} is
 * deliberately temporal-shape-agnostic: callers attach the period
 * (e.g. {@code YearMonth}) to entries themselves. Domain wrappers like
 * {@code CashFlowLedger} layer entry-specific filters on top.
 *
 * @param <T> the entry type
 */
public record TimeSeries<T>(List<T> entries) {

    public TimeSeries {
        Objects.requireNonNull(entries, "entries");
        entries = List.copyOf(entries);
    }

    /** An empty series. */
    public static <T> TimeSeries<T> empty() {
        return new TimeSeries<>(List.of());
    }

    /** A series containing the given entries in order. */
    public static <T> TimeSeries<T> of(Collection<T> entries) {
        return new TimeSeries<>(List.copyOf(entries));
    }

    /** Returns a new series with {@code entry} appended. */
    public TimeSeries<T> append(T entry) {
        Objects.requireNonNull(entry, "entry");
        return new TimeSeries<>(
                Stream.concat(entries.stream(), Stream.of(entry)).toList());
    }

    /** Returns a new series with all of {@code more} appended in order. */
    public TimeSeries<T> appendAll(Collection<? extends T> more) {
        Objects.requireNonNull(more, "more");
        if (more.isEmpty()) {
            return this;
        }
        return new TimeSeries<>(Stream.concat(entries.stream(), more.stream()).toList());
    }

    /** Returns a new series containing only entries matching {@code predicate}. */
    public TimeSeries<T> where(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return new TimeSeries<>(entries.stream().filter(predicate).toList());
    }

    /** Returns a stream over the entries. */
    public Stream<T> stream() {
        return entries.stream();
    }

    /** Number of entries. */
    public int size() {
        return entries.size();
    }

    /** Whether this series is empty. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Sums {@link Money} extracted from each entry. Returns
     * {@link Money#ZERO_USD} on an empty series.
     */
    public Money sumOf(Function<? super T, Money> extractor) {
        Objects.requireNonNull(extractor, "extractor");
        return entries.stream().map(extractor).reduce(Money.ZERO_USD, Money::plus);
    }

    /** Collector that accumulates a stream into a {@code TimeSeries}. */
    public static <T> Collector<T, ?, TimeSeries<T>> toTimeSeries() {
        return Collectors.collectingAndThen(Collectors.toList(), TimeSeries::new);
    }
}
