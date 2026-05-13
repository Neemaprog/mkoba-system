package com.mkoba.mkoba_system.repositories;

import com.mkoba.mkoba_system.entities.Savings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsRepository extends JpaRepository<Savings, Long> {
    
    List<Savings> findByUserId(Long userId);
    
    List<Savings> findByGroupId(Long groupId);
    
    List<Savings> findByUserIdOrderByContributionDateDesc(Long userId);
    
    @Query("SELECT SUM(s.amount) FROM Savings s WHERE s.user.id = :userId")
    Double getTotalSavingsByUser(@Param("userId") Long userId);
    
    @Query("SELECT SUM(s.amount) FROM Savings s WHERE s.group.id = :groupId")
    Double getTotalSavingsByGroup(@Param("groupId") Long groupId);
    
    @Query("SELECT s FROM Savings s WHERE s.user.id = :userId AND s.contributionDate BETWEEN :startDate AND :endDate")
    List<Savings> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM Savings s WHERE s.user.id = :userId")
    Long countSavingsByUser(@Param("userId") Long userId);
}
