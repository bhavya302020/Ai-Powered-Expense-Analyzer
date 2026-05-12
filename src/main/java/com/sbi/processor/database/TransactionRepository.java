package com.sbi.processor.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    
    // Find by date range
    List<TransactionEntity> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Find by category
    List<TransactionEntity> findByCategory(String category);
    
    // Find by type (Credit/Debit)
    List<TransactionEntity> findByType(String type);
    
    // Find by description
    List<TransactionEntity> findByDescriptionContainingIgnoreCase(String description);
    
    // Custom queries
    @Query("SELECT DISTINCT t.category FROM TransactionEntity t")
    List<String> findAllCategories();
    
    @Query("SELECT SUM(t.credit) FROM TransactionEntity t WHERE t.date BETWEEN :startDate AND :endDate")
    Double getTotalCredits(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(t.debit) FROM TransactionEntity t WHERE t.date BETWEEN :startDate AND :endDate")
    Double getTotalDebits(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}