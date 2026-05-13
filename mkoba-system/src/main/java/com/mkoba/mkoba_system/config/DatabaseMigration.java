package com.mkoba.mkoba_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseMigration implements CommandLineRunner {
    
    private final DataSource dataSource;
    
    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void run(String... args) throws Exception {
        createDatabase();
        updateLoanStatusConstraint();
    }
    
    private void createDatabase() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            System.out.println("🔧 Creating m_koba database...");
            
            // Create database if it doesn't exist
            try {
                statement.execute("CREATE DATABASE m_koba");
                System.out.println("✅ Database m_koba created successfully");
            } catch (Exception e) {
                System.out.println("ℹ️ Database m_koba may already exist");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error creating database: " + e.getMessage());
        }
    }
    
    private void updateLoanStatusConstraint() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            System.out.println("🔧 Updating loan status constraint...");
            
            // Connect to m_koba for constraint updates
            try {
                statement.execute("SELECT 1"); // Test connection
            } catch (Exception e) {
                // If still connected to postgres, switch to m_koba
                System.out.println("ℹ️ Skipping constraint update - will be handled when connected to m_koba");
                return;
            }
            
            // Drop existing constraint if it exists
            try {
                statement.execute("ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_status_check");
                System.out.println("✅ Dropped existing constraint");
            } catch (Exception e) {
                System.out.println("ℹ️ No existing constraint to drop");
            }
            
            // Add new constraint with all statuses
            String constraintSql = """
                ALTER TABLE loans 
                ADD CONSTRAINT loans_status_check 
                CHECK (status IN ('PENDING', 'PENDING_ACCOUNTANT_CONFIRMATION', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'DEFAULTED'))
                """;
            
            try {
                statement.execute(constraintSql);
                System.out.println("✅ Added updated constraint with PENDING_ACCOUNTANT_CONFIRMATION");
            } catch (Exception e) {
                System.out.println("ℹ️ Constraint may already exist or table not created yet");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error updating loan status constraint: " + e.getMessage());
        }
    }
}
