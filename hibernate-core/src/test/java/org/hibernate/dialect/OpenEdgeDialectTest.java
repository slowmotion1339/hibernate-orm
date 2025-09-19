/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenEdgeDialect.
 *
 * @author Progress User
 */
public class OpenEdgeDialectTest {

	@Test
	public void testVersioning() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		assertTrue( dialect.getVersion().isSameOrAfter( 12, 0 ) );
	}

	@Test
	public void testColumnTypes() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		// Test boolean type mapping to logical
		assertEquals( "logical", dialect.columnType( SqlTypes.BOOLEAN ) );
		
		// Test integer type mappings
		assertEquals( "tinyint", dialect.columnType( SqlTypes.TINYINT ) );
		assertEquals( "smallint", dialect.columnType( SqlTypes.SMALLINT ) );
		assertEquals( "integer", dialect.columnType( SqlTypes.INTEGER ) );
		assertEquals( "int64", dialect.columnType( SqlTypes.BIGINT ) );
		
		// Test string type mappings
		assertEquals( "character($l)", dialect.columnType( SqlTypes.CHAR ) );
		assertEquals( "character($l) varying", dialect.columnType( SqlTypes.VARCHAR ) );
		assertEquals( "clob", dialect.columnType( SqlTypes.CLOB ) );
		
		// Test binary type mappings
		assertEquals( "raw($l)", dialect.columnType( SqlTypes.BINARY ) );
		assertEquals( "raw($l)", dialect.columnType( SqlTypes.VARBINARY ) );
		assertEquals( "blob", dialect.columnType( SqlTypes.BLOB ) );
	}

	@Test
	public void testCastTypes() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		assertEquals( "character", dialect.castType( SqlTypes.VARCHAR ) );
		assertEquals( "logical", dialect.castType( SqlTypes.BOOLEAN ) );
		assertEquals( "integer", dialect.castType( SqlTypes.INTEGER ) );
		assertEquals( "int64", dialect.castType( SqlTypes.BIGINT ) );
		assertEquals( "raw", dialect.castType( SqlTypes.VARBINARY ) );
	}

	@Test
	public void testCurrentFunctions() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		assertEquals( "current-date", dialect.currentDate() );
		assertEquals( "time", dialect.currentTime() );
		assertEquals( "now", dialect.currentTimestamp() );
		assertEquals( "select now from sysprogress.syscalctable", dialect.getCurrentTimestampSelectString() );
	}

	@Test
	public void testLimitations() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		// Test various limitations of OpenEdge Progress
		assertFalse( dialect.supportsLimitOffset() );
		assertFalse( dialect.supportsForUpdate() );
		assertFalse( dialect.supportsOuterJoinForUpdate() );
		assertFalse( dialect.supportsLockTimeouts() );
		assertFalse( dialect.supportsNoWait() );
		assertFalse( dialect.supportsSkipLocked() );
		assertFalse( dialect.supportsRowValueConstructorSyntax() );
		assertFalse( dialect.supportsRowValueConstructorSyntaxInInList() );
		assertFalse( dialect.supportsTupleDistinctCounts() );
		assertFalse( dialect.supportsCaseInsensitiveLike() );
		assertFalse( dialect.supportsCommentOn() );
		assertFalse( dialect.supportsSubqueryOnMutatingTable() );
	}

	@Test
	public void testSupportedFeatures() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		// Test supported features
		assertTrue( dialect.hasAlterTable() );
		assertTrue( dialect.dropConstraints() );
		assertTrue( dialect.supportsColumnCheck() );
		assertTrue( dialect.supportsTableCheck() );
		assertTrue( dialect.supportsCascadeDelete() );
		assertTrue( dialect.supportsCurrentTimestampSelection() );
		assertTrue( dialect.supportsUnionAll() );
		assertTrue( dialect.supportsUnionInSubquery() );
		assertTrue( dialect.supportsExistsInSelect() );
		assertTrue( dialect.supportsBindAsCallableArgument() );
	}

	@Test
	public void testStringLimits() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		// Test Progress specific string length limits
		assertEquals( 32767, dialect.getMaxVarcharLength() );
		assertEquals( 32767, dialect.getMaxVarcharCapacity() );
		assertEquals( 32767, dialect.getMaxVarbinaryLength() );
	}

	@Test
	public void testPrecisionDefaults() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		assertEquals( 19, dialect.getDefaultDecimalPrecision() );
		assertEquals( 7, dialect.getFloatPrecision() );
		assertEquals( 15, dialect.getDoublePrecision() );
	}

	@Test
	public void testBooleanHandling() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		// Progress uses 'yes'/'no' for boolean values
		final StringBuilder trueBuilder = new StringBuilder();
		dialect.appendBooleanValueString( trueBuilder::append, true );
		assertEquals( "yes", trueBuilder.toString() );
		
		final StringBuilder falseBuilder = new StringBuilder();
		dialect.appendBooleanValueString( falseBuilder::append, false );
		assertEquals( "no", falseBuilder.toString() );
	}

	@Test
	public void testLowercaseFunction() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		assertEquals( "lc", dialect.getLowercaseFunction() );
	}

	@Test
	public void testNativeGenerationStrategy() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		// Progress doesn't have native sequences or identity, so should use TABLE
		assertEquals( jakarta.persistence.GenerationType.TABLE, dialect.getNativeValueGenerationStrategy() );
	}

	@Test
	public void testInExpressionLimit() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		assertEquals( 1000, dialect.getInExpressionCountLimit() );
	}

	@Test
	public void testLockStringsAreEmpty() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		// Progress doesn't support row-level locking
		assertEquals( "", dialect.getForUpdateString() );
		assertEquals( "", dialect.getForUpdateString( "alias" ) );
		assertEquals( "", dialect.getWriteLockString( 1000 ) );
		assertEquals( "", dialect.getReadLockString( 1000 ) );
	}

	@Test
	public void testUnsupportedGuidGeneration() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		assertThrows( UnsupportedOperationException.class, dialect::getSelectGUIDString );
	}

	@Test
	public void testEmptyStringHandling() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		
		// Progress should distinguish between empty string and null
		assertFalse( dialect.isEmptyStringTreatedAsNull() );
	}

	@Test
	public void testCascadeConstraints() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		assertEquals( " cascade", dialect.getCascadeConstraintsString() );
	}

	@Test
	public void testAddColumn() {
		final OpenEdgeDialect dialect = new OpenEdgeDialect();
		assertEquals( "add column", dialect.getAddColumnString() );
	}
}
