/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.account.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountRepository;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountService;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.shared.NotFoundException;

@Service
class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;

    AccountServiceImpl(AccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Account create(PlanId planId, Account account) {
        Account stamped = new Account(
                Optional.empty(),
                planId,
                account.type(),
                account.owner(),
                account.sleeves(),
                account.contributionPolicy());
        return repository.save(stamped);
    }

    @Override
    public Account findById(AccountId id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Override
    public List<Account> findByPlanId(PlanId planId) {
        return repository.findByPlanId(planId);
    }

    @Override
    public List<AccountSleeve> findSleevesByAccountId(AccountId id) {
        return findById(id).sleeves();
    }

    @Override
    public Account replace(AccountId id, Account replacement) {
        Account existing = repository.findById(id).orElseThrow(() -> notFound(id));
        Account merged = new Account(
                existing.id(),
                existing.planId(),
                replacement.type(),
                replacement.owner(),
                replacement.sleeves(),
                replacement.contributionPolicy());
        return repository.save(merged);
    }

    @Override
    public void deleteById(AccountId id) {
        repository.deleteById(id);
    }

    private static NotFoundException notFound(AccountId id) {
        return new NotFoundException("Account " + id.value() + " not found");
    }
}
