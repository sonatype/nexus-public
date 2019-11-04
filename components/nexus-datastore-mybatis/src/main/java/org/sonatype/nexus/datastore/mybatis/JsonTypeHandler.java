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
package org.sonatype.nexus.datastore.mybatis;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import static com.google.common.base.Charsets.UTF_8;

/**
 * MyBatis {@link TypeHandler} that maps objects to/from SQL as JSON.
 */
public abstract class JsonTypeHandler<T>
    extends BaseTypeHandler<T>
{
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final JavaType jsonType = OBJECT_MAPPER.constructType(getRawType());

  @Override
  public final void setNonNullParameter(final PreparedStatement ps,
                                        final int parameterIndex,
                                        final T parameter,
                                        final JdbcType jdbcType)
      throws SQLException
  {
    byte[] json = writeToJson(parameter);
    if (ps.isWrapperFor(org.h2.jdbc.JdbcPreparedStatement.class)) {
      // H2 only accepts JSON passed as UTF8 byte array
      ps.setBytes(parameterIndex, json);
    }
    else {
      // while PostgreSQL and other DBs want a UTF8 string
      ps.setString(parameterIndex, new String(json, UTF_8));
    }
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return readFromJson(rs.getBytes(columnName)); // works for both byte[] and UTF8 strings
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return readFromJson(rs.getBytes(columnIndex)); // works for both byte[] and UTF8 strings
  }

  @Override
  public final T getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return readFromJson(cs.getBytes(columnIndex)); // works for both byte[] and UTF8 strings
  }

  private byte[] writeToJson(final Object value) throws SQLException {
    try {
      return OBJECT_MAPPER.writeValueAsBytes(value);
    }
    catch (IOException e) {
      throw new SQLException(e);
    }
  }

  @Nullable
  private T readFromJson(@Nullable final byte[] json) throws SQLException {
    try {
      return json != null ? OBJECT_MAPPER.readValue(json, jsonType) : null;
    }
    catch (IOException e) {
      throw new SQLException(e);
    }
  }
}
