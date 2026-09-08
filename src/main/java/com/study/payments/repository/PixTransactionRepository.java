package com.study.payments.repository;

import com.study.payments.model.PixTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PixTransactionRepository extends JpaRepository<PixTransaction, UUID> {
}