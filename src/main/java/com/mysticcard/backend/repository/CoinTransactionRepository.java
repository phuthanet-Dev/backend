package com.mysticcard.backend.repository;

import com.mysticcard.backend.entity.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {
    List<CoinTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    boolean existsByTransRef(String transRef);
}
