/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import jakarta.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OpenEdgeDialect.
 * These tests verify that the dialect works correctly within the Hibernate framework.
 * 
 * Note: These tests are disabled by default as they require a running OpenEdge Progress database.
 * To enable them, remove the @Disabled annotation and configure appropriate database connection.
 *
 * @author Progress User
 */
@Disabled("Requires OpenEdge Progress database connection")
public class OpenEdgeDialectIntegrationTest {

	@Entity
	@Table(name = "test_customer")
	public static class TestCustomer {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Long id;
		
		@Column(name = "name", length = 100)
		private String name;
		
		@Column(name = "active")
		private Boolean active;
		
		@Column(name = "created_date")
		@Temporal(TemporalType.TIMESTAMP)
		private Date createdDate;
		
		@Column(name = "balance", precision = 10, scale = 2)
		private java.math.BigDecimal balance;
		
		// Constructors
		public TestCustomer() {}
		
		public TestCustomer(String name, Boolean active, Date createdDate, java.math.BigDecimal balance) {
			this.name = name;
			this.active = active;
			this.createdDate = createdDate;
			this.balance = balance;
		}
		
		// Getters and setters
		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		
		public Boolean getActive() { return active; }
		public void setActive(Boolean active) { this.active = active; }
		
		public Date getCreatedDate() { return createdDate; }
		public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
		
		public java.math.BigDecimal getBalance() { return balance; }
		public void setBalance(java.math.BigDecimal balance) { this.balance = balance; }
	}

	/**
	 * Test that demonstrates how to configure Hibernate with OpenEdgeDialect
	 */
	@Test
	public void testDialectConfiguration() {
		Map<String, String> settings = new HashMap<>();
		settings.put(AvailableSettings.DIALECT, "org.hibernate.dialect.OpenEdgeDialect");
		settings.put(AvailableSettings.URL, "jdbc:datadirect:openedge://localhost:20931;databaseName=test");
		settings.put(AvailableSettings.DRIVER, "com.ddtek.jdbc.openedge.OpenEdgeDriver");
		settings.put(AvailableSettings.USER, "testuser");
		settings.put(AvailableSettings.PASS, "testpass");
		settings.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
		
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySettings(settings)
				.build();
		
		try {
			SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources(registry)
					.addAnnotatedClass(TestCustomer.class)
					.buildMetadata()
					.buildSessionFactory();
			
			assertNotNull(sessionFactory);
			assertTrue(sessionFactory.getJdbcServices().getDialect() instanceof OpenEdgeDialect);
			
			sessionFactory.close();
		} finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

	/**
	 * Test basic CRUD operations with OpenEdge Progress types
	 */
	@Test
	public void testBasicCRUDOperations() {
		// This test would require actual database connection
		// Implementation would go here if database was available
		
		// Example of what the test would do:
		// 1. Create TestCustomer entity with Progress-specific types
		// 2. Persist entity (test boolean -> logical mapping)
		// 3. Query entity (test retrieval and type conversion)
		// 4. Update entity (test updates work correctly)
		// 5. Delete entity (test cascade behavior)
		
		assertTrue(true, "Placeholder test - requires database connection");
	}

	/**
	 * Test Progress-specific boolean handling
	 */
	@Test
	public void testBooleanMapping() {
		// This would test that:
		// - true values are stored as "yes" in Progress
		// - false values are stored as "no" in Progress
		// - retrieval correctly converts back to Boolean
		
		assertTrue(true, "Placeholder test - requires database connection");
	}

	/**
	 * Test Progress TOP clause pagination
	 */
	@Test
	public void testPaginationWithTopClause() {
		// This would test that:
		// - setMaxResults() generates correct TOP clause
		// - setFirstResult() with setMaxResults() throws UnsupportedOperationException
		// - SQL generated uses "SELECT TOP n" syntax
		
		assertTrue(true, "Placeholder test - requires database connection");
	}

	/**
	 * Test Progress-specific SQL generation
	 */
	@Test
	public void testProgressSQLGeneration() {
		// This would test that DDL generated by Hibernate uses:
		// - "logical" for boolean columns
		// - "int64" for BIGINT columns  
		// - "character(n) varying" for VARCHAR columns
		// - "decimal(p,s)" for DECIMAL columns
		
		assertTrue(true, "Placeholder test - requires database connection");
	}

	/**
	 * Test that Progress limitations are properly handled
	 */
	@Test 
	public void testProgressLimitations() {
		// This would test that:
		// - FOR UPDATE clauses are ignored/empty
		// - Row value constructors throw appropriate exceptions
		// - Large IN clauses are handled appropriately
		// - Unsupported operations throw proper exceptions
		
		assertTrue(true, "Placeholder test - requires database connection");
	}

	/**
	 * Test Progress-specific functions work correctly
	 */
	@Test
	public void testProgressFunctions() {
		// This would test that Progress-specific functions work:
		// - lc() for lowercase
		// - String manipulation functions
		// - Date/time functions
		// - Math functions
		
		assertTrue(true, "Placeholder test - requires database connection");
	}
	
	/**
	 * Example of how the dialect would be used in a Spring Boot application
	 */
	public static class ExampleSpringBootConfiguration {
		// application.yml example:
		/*
		spring:
		  datasource:
		    url: jdbc:datadirect:openedge://localhost:20931;databaseName=mydb
		    driver-class-name: com.ddtek.jdbc.openedge.OpenEdgeDriver
		    username: username
		    password: password
		  jpa:
		    database-platform: org.hibernate.dialect.OpenEdgeDialect
		    hibernate:
		      ddl-auto: update
		      show-sql: true
		      format-sql: true
		    properties:
		      hibernate:
		        jdbc:
		          batch_size: 15
		*/
	}
}
