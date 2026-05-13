package com.mkoba.mkoba_system.repositories;

import com.mkoba.mkoba_system.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByGroupId(Long groupId);
    
    @Query("SELECT u FROM User u WHERE u.group.id = :groupId AND u.role = :role")
    List<User> findByGroupIdAndRole(@Param("groupId") Long groupId, @Param("role") User.UserRole role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.group.id = :groupId")
    Long countByGroupId(@Param("groupId") Long groupId);
    
    boolean existsByEmail(String email);
}
