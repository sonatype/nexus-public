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

import org.sonatype.nexus.security.PasswordHelper;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MyBatis {@link TypeHandler} that treats character arrays as passwords and encrypts them before persisting.
 *
 * @since 3.21
 */
// not @Named because we register this manually
public class PasswordCharacterArrayTypeHandler
    extends BaseTypeHandler<char[]>
{
  private final PasswordHelper passwordHelper;

  public PasswordCharacterArrayTypeHandler(final PasswordHelper passwordHelper) {
    this.passwordHelper = checkNotNull(passwordHelper);
  }

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final char[] parameter,
      final JdbcType jdbcType) throws SQLException
  {
    ps.setString(parameterIndex, passwordHelper.encryptChars(parameter));
  }

  @Override
  public char[] getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return passwordHelper.decryptChars(rs.getString(columnName));
  }

  @Override
  public char[] getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return passwordHelper.decryptChars(rs.getString(columnIndex));
  }

  @Override
  public char[] getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return passwordHelper.decryptChars(cs.getString(columnIndex));
  }
}
