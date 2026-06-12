/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.internal;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.github.xmljim.retirement.retirementplanner.api.AccountOperations;
import io.github.xmljim.retirement.retirementplanner.api.dto.AccountDto;
import io.github.xmljim.retirement.retirementplanner.api.dto.AccountSleeveDto;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountService;

@RestController
class AccountController implements AccountOperations {

    private final AccountService service;

    AccountController(AccountService service) {
        this.service = service;
    }

    @Override
    public List<AccountDto> findByPlanId(long planId) {
        return service.findByPlanId(new PlanId(planId)).stream()
                .map(AccountDto::from)
                .toList();
    }

    @Override
    public ResponseEntity<AccountDto> create(long planId, AccountDto account) {
        PlanId parent = new PlanId(planId);
        Account created = service.create(parent, account.toNewAccount(parent));
        long newId = created.id().orElseThrow().value();
        return ResponseEntity.created(AccountOperations.locationOf(newId)).body(AccountDto.from(created));
    }

    @Override
    public AccountDto findById(long id) {
        return AccountDto.from(service.findById(new AccountId(id)));
    }

    @Override
    public List<AccountSleeveDto> findSleeves(long id) {
        return service.findSleevesByAccountId(new AccountId(id)).stream()
                .map(AccountSleeveDto::from)
                .toList();
    }

    @Override
    public AccountDto replace(long id, AccountDto account) {
        Account existing = service.findById(new AccountId(id));
        Account replacement = account.toNewAccount(existing.planId());
        return AccountDto.from(service.replace(new AccountId(id), replacement));
    }

    @Override
    public ResponseEntity<Void> deleteById(long id) {
        service.deleteById(new AccountId(id));
        return ResponseEntity.noContent().build();
    }
}
