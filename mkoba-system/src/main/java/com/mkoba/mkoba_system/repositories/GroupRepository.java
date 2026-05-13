package com.mkoba.mkoba_system.repositories;

import com.mkoba.mkoba_system.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    Optional<Group> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT g FROM Group g WHERE g.id = :groupId")
    Optional<Group> findWithMembers(@Param("groupId") Long groupId);
    
    @Query("SELECT COUNT(g) FROM Group g")
    Long countAllGroups();
    
    List<Group> findByNameContainingIgnoreCase(String name);
    
    List<Group> findByActive(boolean active);
}
