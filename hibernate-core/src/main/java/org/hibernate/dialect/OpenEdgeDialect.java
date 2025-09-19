/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.descriptor.DateTimeUtils;

import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.type.SqlTypes.*;

/**
 * A {@linkplain Dialect SQL dialect} for OpenEdge Progress 12.0 and above.
 * <p>
 * This dialect provides support for OpenEdge Progress database features
 * including specific data types, functions, and SQL syntax variations.
 * <p>
 * Please refer to the
 * <a href="https://docs.progress.com/category/openedge">OpenEdge Progress documentation</a>.
 *
 * @author Progress User
 */
public class OpenEdgeDialect extends Dialect {

	protected static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 12, 0 );

	private final UniqueDelegate uniqueDelegate = new DefaultUniqueDelegate( this );

	public OpenEdgeDialect() {
		this( MINIMUM_VERSION );
	}

	public OpenEdgeDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( MINIMUM_VERSION ) );
		registerKeywords( info );
	}

	public OpenEdgeDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			// OpenEdge Progress specific type mappings
			case BOOLEAN -> "logical";

			// Integer types
			case TINYINT -> "tinyint";
			case SMALLINT -> "smallint";
			case INTEGER -> "integer";
			case BIGINT -> "int64";

			// Decimal types
			case FLOAT -> "decimal($p,2)";
			case REAL -> "decimal(7,2)";
			case DOUBLE -> "decimal(15,2)";
			case NUMERIC -> "decimal($p,$s)";
			case DECIMAL -> "decimal($p,$s)";

			// String types
			case CHAR -> "character($l)";
			case VARCHAR -> "character($l) varying";
			case LONGVARCHAR, LONG32VARCHAR -> "clob";

			// Unicode string types (Progress has limited Unicode support)
			case NCHAR -> "character($l)";
			case NVARCHAR -> "character($l) varying";
			case LONGNVARCHAR, LONG32NVARCHAR -> "clob";

			// Binary types
			case BINARY -> "raw($l)";
			case VARBINARY -> "raw($l)";
			case LONGVARBINARY, LONG32VARBINARY -> "blob";

			// LOB types
			case BLOB -> "blob";
			case CLOB -> "clob";
			case NCLOB -> "clob";

			// Date/Time types
			case DATE -> "date";
			case TIME -> "time";
			case TIMESTAMP -> "timestamp";
			case TIMESTAMP_WITH_TIMEZONE -> "timestamp with time zone";
			case TIME_WITH_TIMEZONE -> "time with time zone";

			// Default to parent implementation
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	protected String castType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
			case CHAR, NCHAR, VARCHAR, NVARCHAR -> "character";
			case LONGVARCHAR, LONG32VARCHAR, LONGNVARCHAR, LONG32NVARCHAR -> "clob";
			case BINARY, VARBINARY, LONGVARBINARY, LONG32VARBINARY -> "raw";
			case BOOLEAN -> "logical";
			case INTEGER -> "integer";
			case BIGINT -> "int64";
			case DECIMAL, NUMERIC -> "decimal";
			default -> super.castType( sqlTypeCode );
		};
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// Override boolean type with Progress logical
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( BOOLEAN, "logical", this ) );

		// Add Progress specific types
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( BIGINT, "int64", this ) );
	}

	@Override
	public int getMaxVarcharLength() {
		// Progress CHARACTER field maximum length
		return 32767;
	}

	@Override
	public int getMaxVarcharCapacity() {
		return getMaxVarcharLength();
	}

	@Override
	public int getMaxVarbinaryLength() {
		// Progress RAW field maximum length
		return 32767;
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	@Override
	public String currentDate() {
		return "current-date";
	}

	@Override
	public String currentTime() {
		return "time";
	}

	@Override
	public String currentTimestamp() {
		return "now";
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final CommonFunctionFactory functionFactory = new CommonFunctionFactory( functionContributions );

		// Progress specific math functions
		functionFactory.log();
		functionFactory.log10();
		functionFactory.sqrt();
		functionFactory.abs();
		functionFactory.ceiling();
		functionFactory.floor();
		functionFactory.round();

		// String functions
		functionFactory.length_characterLength();
		functionFactory.lowerUpper();
		functionFactory.substring();
		functionFactory.trim2();
		functionFactory.ltrimRtrim();
		functionFactory.concat_pipeOperator();

		// Date/time functions
		functionFactory.dateTrunc();

		// Aggregate functions
		functionFactory.aggregates( this, org.hibernate.sql.ast.SqlAstNodeRenderingMode.DEFAULT );
		functionFactory.windowFunctions();
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case YEAR -> "year(?2)";
			case MONTH -> "month(?2)";
			case DAY -> "day(?2)";
			case HOUR -> "hour(?2)";
			case MINUTE -> "minute(?2)";
			case SECOND -> "second(?2)";
			default -> super.extractPattern( unit );
		};
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		return switch (to) {
			case STRING -> "string(?1)";
			case INTEGER -> "integer(?1)";
			case LONG -> "int64(?1)";
			case BOOLEAN -> "logical(?1)";
			default -> super.castPattern( from, to );
		};
	}

	@Override
	@SuppressWarnings("deprecation")
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch (unit) {
			case YEAR -> "add-interval(?3, ?2, 'years')";
			case MONTH -> "add-interval(?3, ?2, 'months')";
			case DAY -> "add-interval(?3, ?2, 'days')";
			case HOUR -> "add-interval(?3, ?2, 'hours')";
			case MINUTE -> "add-interval(?3, ?2, 'minutes')";
			case SECOND -> "add-interval(?3, ?2, 'seconds')";
			default -> "(?3 + interval '?2 " + unit.toString().toLowerCase() + "')";
		};
	}

	@Override
	@SuppressWarnings("deprecation")
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch (unit) {
			case YEAR -> "interval(?3, ?2, 'years')";
			case MONTH -> "interval(?3, ?2, 'months')";
			case DAY -> "interval(?3, ?2, 'days')";
			case HOUR -> "interval(?3, ?2, 'hours')";
			case MINUTE -> "interval(?3, ?2, 'minutes')";
			case SECOND -> "interval(?3, ?2, 'seconds')";
			default -> "(?3 - ?2)";
		};
	}

	@Override
	public LimitHandler getLimitHandler() {
		// Progress supports TOP clause similar to SQL Server
		return TopLimitHandler.INSTANCE;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		// Progress has limited sequence support
		return NoSequenceSupport.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		// Progress doesn't have native identity/auto-increment columns
		return IdentityColumnSupportImpl.INSTANCE;
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		// Since Progress doesn't have sequences or identity, use TABLE strategy
		return GenerationType.TABLE;
	}

	@Override
	public boolean hasAlterTable() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	@Override
	public boolean supportsColumnCheck() {
		return true;
	}

	@Override
	public boolean supportsTableCheck() {
		return true;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public boolean supportsCommentOn() {
		return false; // Progress doesn't support COMMENT ON syntax
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now from sysprogress.syscalctable";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsUnionInSubquery() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public String getLowercaseFunction() {
		return "lc";
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return false; // Progress LIKE is case-sensitive by default
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return true;
	}

	@Override
	public boolean supportsBindAsCallableArgument() {
		return true;
	}

	@Override
	public int getInExpressionCountLimit() {
		// Progress has a limit on IN clause items
		return 1000;
	}

	@Override
	public boolean forceLobAsLastValue() {
		return false;
	}

	@Override
	public boolean supportsSubqueryOnMutatingTable() {
		return false; // Progress has restrictions on subqueries in DML
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public String getForUpdateString() {
		return ""; // Progress doesn't support FOR UPDATE
	}

	@Override
	public String getForUpdateString(String aliases) {
		return ""; // Progress doesn't support FOR UPDATE
	}

	@Override
	public String getWriteLockString(int timeout) {
		return ""; // Progress doesn't support row-level locking
	}

	@Override
	public String getReadLockString(int timeout) {
		return ""; // Progress doesn't support row-level locking
	}

	@Override
	public boolean supportsForUpdate() {
		return false;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean supportsNoWait() {
		return false;
	}

	@Override
	public boolean supportsSkipLocked() {
		return false;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool ? "yes" : "no" );
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendDateTimeLiteral(SqlAppender appender, TemporalAccessor temporalAccessor, 
			TemporalType precision, TimeZone jdbcTimeZone) {
		// Progress uses specific date/time literal formats
		switch (precision) {
			case DATE:
				appender.appendSql("date '");
				DateTimeUtils.appendAsDate(appender, temporalAccessor);
				appender.appendSql("'");
				break;
			case TIME:
				appender.appendSql("time '");
				DateTimeUtils.appendAsTime(appender, temporalAccessor, false, jdbcTimeZone);
				appender.appendSql("'");
				break;
			case TIMESTAMP:
				appender.appendSql("timestamp '");
				DateTimeUtils.appendAsTimestampWithNanos(appender, temporalAccessor, false, jdbcTimeZone);
				appender.appendSql("'");
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendDateTimeLiteral(SqlAppender appender, Date date, 
			TemporalType precision, TimeZone jdbcTimeZone) {
		// Progress uses specific date/time literal formats
		switch (precision) {
			case DATE:
				appender.appendSql("date '");
				DateTimeUtils.appendAsDate(appender, date);
				appender.appendSql("'");
				break;
			case TIME:
				appender.appendSql("time '");
				DateTimeUtils.appendAsLocalTime(appender, date);
				appender.appendSql("'");
				break;
			case TIMESTAMP:
				appender.appendSql("timestamp '");
				DateTimeUtils.appendAsTimestampWithNanos(appender, date, jdbcTimeZone);
				appender.appendSql("'");
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendDateTimeLiteral(SqlAppender appender, Calendar calendar, 
			TemporalType precision, TimeZone jdbcTimeZone) {
		// Progress uses specific date/time literal formats
		switch (precision) {
			case DATE:
				appender.appendSql("date '");
				DateTimeUtils.appendAsDate(appender, calendar);
				appender.appendSql("'");
				break;
			case TIME:
				appender.appendSql("time '");
				DateTimeUtils.appendAsLocalTime(appender, calendar);
				appender.appendSql("'");
				break;
			case TIMESTAMP:
				appender.appendSql("timestamp '");
				DateTimeUtils.appendAsTimestampWithMillis(appender, calendar, jdbcTimeZone);
				appender.appendSql("'");
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Progress constraint violation exception extractor
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState == null ) {
					return null;
				}
				// Progress error codes for constraint violations
				return switch ( sqlState ) {
					case "23000" -> { // Integrity constraint violation
						final String message = sqle.getMessage();
						if ( message != null ) {
							// Extract constraint name from Progress error message
							Pattern pattern = Pattern.compile("constraint \\[([^\\]]+)\\]");
							Matcher matcher = pattern.matcher(message);
							if ( matcher.find() ) {
								yield matcher.group(1);
							}
						}
						yield null;
					}
					default -> null;
				};
			});

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			if ( sqlState == null ) {
				return null;
			}
			// Convert Progress-specific SQLStates to appropriate Hibernate exceptions
			return switch ( sqlState ) {
				case "40001" -> new org.hibernate.exception.LockAcquisitionException( message, sqlException, sql );
				case "40003" -> new org.hibernate.exception.LockTimeoutException( message, sqlException, sql );
				case "57014" -> new org.hibernate.QueryTimeoutException( message, sqlException, sql );
				default -> null;
			};
		};
	}

	@Override
	public int getDefaultDecimalPrecision() {
		return 19; // Progress decimal default precision
	}

	@Override
	public int getFloatPrecision() {
		return 7; // Progress float precision
	}

	@Override
	public int getDoublePrecision() {
		return 15; // Progress double precision
	}

	@Override
	public String getSelectGUIDString() {
		throw new UnsupportedOperationException( "OpenEdge Progress does not support GUID generation" );
	}

	@Override
	public boolean isEmptyStringTreatedAsNull() {
		return false; // Progress distinguishes empty string from null
	}
}
