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

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Type handler for use with {@code ::tsvector}<br/>
 *
 * Not @Component to avoid injection
 */
public class QuotingTypeHandler
    extends BaseTypeHandler<String>
{
  private static final String QUOTE = "'";

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final String parameter,
      final JdbcType jdbcType) throws SQLException
  {
    ps.setString(parameterIndex, tsEscape(parameter));
  }

  @Override
  public String getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    throw new UnsupportedOperationException();
  }

  public static String tsEscape(final String term) {
    // NEXUS-54105 defense in depth: PostgreSQL cannot encode 0x00 as a bind parameter
    // for ::tsvector. Strip NUL bytes so this handler never forwards them to the driver.
    String sanitized = term.replace("\u0000", "");
    return QUOTE + sanitized.replace("\\", "\\\\").replace(QUOTE, "\\'") + QUOTE;
  }
}
