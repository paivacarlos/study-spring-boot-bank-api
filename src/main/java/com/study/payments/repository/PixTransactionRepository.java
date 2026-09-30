package com.study.payments.repository;

import com.study.payments.model.PixTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PixTransactionRepository extends JpaRepository<PixTransaction, UUID> {}
