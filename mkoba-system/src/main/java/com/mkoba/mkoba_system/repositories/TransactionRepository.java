package com.mkoba.mkoba_system.repositories;

import com.mkoba.mkoba_system.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByUserId(Long userId);
    
    List<Transaction> findByGroupId(Long groupId);
    
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);
    
    List<Transaction> findByType(Transaction.TransactionType type);
    
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                              @Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    Double getTotalByUserAndType(@Param("userId") Long userId, @Param("type") Transaction.TransactionType type);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId")
    Long countTransactionsByUser(@Param("userId") Long userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.type IN :types ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndTypes(@Param("userId") Long userId, @Param("types") List<Transaction.TransactionType> types);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.type = :type ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndTypeOrderByTransactionDateDesc(@Param("userId") Long userId, @Param("type") Transaction.TransactionType type);
}
