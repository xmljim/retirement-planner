/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeSeriesTest {

    @Test
    @DisplayName("empty series has zero size and ZERO_USD sum")
    void emptySeries() {
        TimeSeries<Money> series = TimeSeries.empty();
        assertThat(series.isEmpty()).isTrue();
        assertThat(series.size()).isZero();
        assertThat(series.sumOf(m -> m)).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("append returns a new series; original is unchanged")
    void appendIsImmutable() {
        TimeSeries<Money> original = TimeSeries.empty();
        TimeSeries<Money> appended = original.append(Money.usd("100"));
        assertThat(original.size()).isZero();
        assertThat(appended.size()).isEqualTo(1);
        assertThat(appended.entries()).containsExactly(Money.usd("100"));
    }

    @Test
    @DisplayName("appendAll preserves order")
    void appendAllPreservesOrder() {
        TimeSeries<Money> series = TimeSeries.<Money>empty()
                .appendAll(List.of(Money.usd("1"), Money.usd("2")))
                .appendAll(List.of(Money.usd("3")));
        assertThat(series.entries()).containsExactly(Money.usd("1"), Money.usd("2"), Money.usd("3"));
    }

    @Test
    @DisplayName("appendAll on empty collection returns same instance")
    void appendAllEmptyIsIdentity() {
        TimeSeries<Money> series = TimeSeries.of(List.of(Money.usd("1")));
        assertThat(series.appendAll(List.of())).isSameAs(series);
    }

    @Test
    @DisplayName("where filters entries")
    void whereFilters() {
        TimeSeries<Money> series = TimeSeries.of(List.of(Money.usd("1"), Money.usd("5"), Money.usd("10")));
        TimeSeries<Money> filtered = series.where(m -> m.amount().intValue() >= 5);
        assertThat(filtered.size()).isEqualTo(2);
        assertThat(filtered.sumOf(m -> m)).isEqualTo(Money.usd("15"));
    }

    @Test
    @DisplayName("sumOf totals extracted Money values")
    void sumOfTotals() {
        TimeSeries<Money> series = TimeSeries.of(List.of(Money.usd("100"), Money.usd("250.50")));
        assertThat(series.sumOf(m -> m)).isEqualTo(Money.usd("350.50"));
    }

    @Test
    @DisplayName("stream exposes entries in order")
    void streamInOrder() {
        TimeSeries<Money> series = TimeSeries.of(List.of(Money.usd("1"), Money.usd("2")));
        assertThat(series.stream().toList()).containsExactly(Money.usd("1"), Money.usd("2"));
    }

    @Test
    @DisplayName("constructor copies entries — caller mutation does not leak")
    void constructorDefensiveCopy() {
        List<Money> mutable = new ArrayList<>(List.of(Money.usd("1")));
        TimeSeries<Money> series = new TimeSeries<>(mutable);
        mutable.add(Money.usd("99"));
        assertThat(series.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("null entries collection rejected")
    void nullEntriesRejected() {
        assertThatThrownBy(() -> new TimeSeries<>(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("entries");
    }

    @Test
    @DisplayName("null appended entry rejected")
    void nullAppendRejected() {
        assertThatThrownBy(() -> TimeSeries.<Money>empty().append(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("toTimeSeries collector accumulates a stream")
    void collectorAccumulates() {
        TimeSeries<Money> series = Stream.of(Money.usd("1"), Money.usd("2")).collect(TimeSeries.toTimeSeries());
        assertThat(series.size()).isEqualTo(2);
    }
}
