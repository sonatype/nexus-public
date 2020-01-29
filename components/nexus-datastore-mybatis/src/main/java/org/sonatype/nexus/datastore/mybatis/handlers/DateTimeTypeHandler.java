/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.datastore.mybatis.handlers;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.annotation.Nullable;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.joda.time.DateTime;

import static org.joda.time.DateTimeZone.UTC;

/**
 * MyBatis {@link TypeHandler} that maps Joda {@link DateTime} values to/from SQL.
 *
 * @since 3.20
 */
// not @Named because we register this manually
public class DateTimeTypeHandler
    extends BaseTypeHandler<DateTime>
{
  @Override
  public void setNonNullParameter(final PreparedStatement ps,
                                  final int parameterIndex,
                                  final DateTime parameter,
                                  final JdbcType jdbcType)
      throws SQLException
  {
    ps.setTimestamp(parameterIndex, new Timestamp(parameter.getMillis()));
  }

  @Override
  public DateTime getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return nullableDateTime(rs.getTimestamp(columnName));
  }

  @Override
  public DateTime getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return nullableDateTime(rs.getTimestamp(columnIndex));
  }

  @Override
  public DateTime getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return nullableDateTime(cs.getTimestamp(columnIndex));
  }

  @Nullable
  private DateTime nullableDateTime(@Nullable final Timestamp timestamp) {
    return timestamp != null ? new DateTime(timestamp.getTime(), UTC) : null;
  }
}
