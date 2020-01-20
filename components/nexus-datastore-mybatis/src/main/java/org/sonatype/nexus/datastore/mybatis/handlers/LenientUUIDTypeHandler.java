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
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import static java.util.UUID.fromString;

/**
 * Lenient MyBatis {@link TypeHandler} that maps UUID values to/from SQL strings.
 *
 * This is only registered when MyBatis is using a database that's not H2 or PostgreSQL.
 *
 * @since 3.20
 */
// not @Named because we register this manually
public class LenientUUIDTypeHandler
    extends BaseTypeHandler<UUID>
{
  @Override
  public void setNonNullParameter(final PreparedStatement ps,
                                  final int parameterIndex,
                                  final UUID parameter,
                                  final JdbcType jdbcType)
      throws SQLException
  {
    // always convert UUIDs to strings before they hit the database
    ps.setString(parameterIndex, parameter.toString());
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return nullableUUID(rs.getObject(columnName));
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return nullableUUID(rs.getObject(columnIndex));
  }

  @Override
  public UUID getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return nullableUUID(cs.getObject(columnIndex));
  }

  @Nullable
  private UUID nullableUUID(@Nullable final Object uuid) {
    // expect strings but also accept UUIDs
    if (uuid instanceof String) {
      return fromString((String) uuid);
    }
    if (uuid instanceof UUID) {
      return (UUID) uuid;
    }
    return null;
  }
}
