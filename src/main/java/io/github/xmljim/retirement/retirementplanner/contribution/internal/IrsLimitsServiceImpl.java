/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimitsService;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Loads {@code resources/data/irs-limits.yaml} once at construction
 * and serves {@link IrsLimits} for any requested year, projecting
 * forward when the year exceeds the dataset's latest published year.
 */
@Service
class IrsLimitsServiceImpl implements IrsLimitsService {

    private static final Logger LOG = LoggerFactory.getLogger(IrsLimitsServiceImpl.class);
    private static final String CLASSPATH_LOCATION = "data/irs-limits.yaml";

    private final NavigableMap<Integer, IrsLimits> publishedByYear;
    private final BigDecimal growthRate;
    private final Map<Integer, IrsLimits> projectedCache = new ConcurrentHashMap<>();

    IrsLimitsServiceImpl() {
        this(new ClassPathResource(CLASSPATH_LOCATION));
    }

    IrsLimitsServiceImpl(Resource resource) {
        IrsLimitsYaml parsed = parse(resource);
        this.growthRate = parsed.projection().contributionLimitGrowthRate();
        NavigableMap<Integer, IrsLimits> byYear = new TreeMap<>();
        parsed.years().forEach(yl -> byYear.put(yl.year(), toLimits(yl, IrsLimits.Source.PUBLISHED)));
        this.publishedByYear = byYear;
        LOG.info(
                "Loaded IRS limits: published years {} – {}, projection growth rate {}",
                publishedByYear.firstKey(),
                publishedByYear.lastKey(),
                growthRate);
    }

    @Override
    public IrsLimits forYear(int year) {
        IrsLimits published = publishedByYear.get(year);
        if (published != null) {
            return published;
        }
        if (year < publishedByYear.firstKey()) {
            throw new IllegalArgumentException(
                    "year " + year + " precedes earliest published year " + publishedByYear.firstKey());
        }
        return projectedCache.computeIfAbsent(year, this::project);
    }

    private IrsLimits project(int targetYear) {
        Map.Entry<Integer, IrsLimits> latest = publishedByYear.lastEntry();
        int gap = targetYear - latest.getKey();
        BigDecimal multiplier = BigDecimal.ONE.add(growthRate).pow(gap);
        IrsLimits base = latest.getValue();
        LOG.info(
                "Projecting IRS limits for {} from published year {} (gap={}, growthRate={})",
                targetYear,
                latest.getKey(),
                gap,
                growthRate);
        return new IrsLimits(
                targetYear,
                base.employee401kBase().times(multiplier),
                base.employee401k50PlusCatchup().times(multiplier),
                base.employee401k60PlusCatchup().times(multiplier),
                base.iraBase().times(multiplier),
                base.ira50PlusCatchup().times(multiplier),
                base.hsaSelfOnly().times(multiplier),
                base.hsaFamily().times(multiplier),
                base.hsa55PlusCatchup().times(multiplier),
                base.totalDc().times(multiplier),
                base.secure2_0_603HighEarnerThreshold().times(multiplier),
                IrsLimits.Source.PROJECTED);
    }

    private static IrsLimits toLimits(IrsLimitsYaml.YearLimits yl, IrsLimits.Source source) {
        return new IrsLimits(
                yl.year(),
                Money.usd(yl.employee401kBase().toPlainString()),
                Money.usd(yl.employee401k50PlusCatchup().toPlainString()),
                Money.usd(yl.employee401k60PlusCatchup().toPlainString()),
                Money.usd(yl.iraBase().toPlainString()),
                Money.usd(yl.ira50PlusCatchup().toPlainString()),
                Money.usd(yl.hsaSelfOnly().toPlainString()),
                Money.usd(yl.hsaFamily().toPlainString()),
                Money.usd(yl.hsa55PlusCatchup().toPlainString()),
                Money.usd(yl.totalDc().toPlainString()),
                Money.usd(yl.secure2_0_603HighEarnerThreshold().toPlainString()),
                source);
    }

    private static IrsLimitsYaml parse(Resource resource) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream in = resource.getInputStream()) {
            return mapper.readValue(in, IrsLimitsYaml.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load IRS limits from " + resource, e);
        }
    }
}
