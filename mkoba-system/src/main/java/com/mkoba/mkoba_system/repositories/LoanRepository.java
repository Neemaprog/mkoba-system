package com.mkoba.mkoba_system.repositories;

import com.mkoba.mkoba_system.entities.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    List<Loan> findByUserId(Long userId);
    
    List<Loan> findByGroupId(Long groupId);
    
    List<Loan> findByUserIdOrderByApplicationDateDesc(Long userId);
    
    List<Loan> findByStatus(Loan.LoanStatus status);
    
    @Query("SELECT l FROM Loan l WHERE l.user.id = :userId AND l.status IN :statuses")
    List<Loan> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<Loan.LoanStatus> statuses);
    
    @Query("SELECT SUM(l.amount) FROM Loan l WHERE l.user.id = :userId AND l.status = :status")
    Double getTotalLoansByUserAndStatus(@Param("userId") Long userId, @Param("status") Loan.LoanStatus status);
    
    @Query("SELECT SUM(l.remainingBalance) FROM Loan l WHERE l.user.id = :userId AND l.status IN :statuses")
    Double getTotalOutstandingBalance(@Param("userId") Long userId, @Param("statuses") List<Loan.LoanStatus> statuses);
    
    @Query("SELECT l FROM Loan l WHERE l.user.id = :userId AND l.applicationDate BETWEEN :startDate AND :endDate")
    List<Loan> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.user.id = :userId AND l.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Loan.LoanStatus status);
    
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.user.id = :userId")
    Long countLoansByUser(@Param("userId") Long userId);
}
