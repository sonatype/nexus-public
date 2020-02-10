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

import org.sonatype.nexus.datastore.mybatis.CipherAwareTypeHandler;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * MyBatis {@link TypeHandler} that encrypts strings with the property that the same source string
 * will always be encrypted to the same database string, assuming the same database cipher settings.
 * To use this handler you must explicitly refer to it in the appropriate parameter/result mappings.
 *
 * This handler should only be used if you need to search on the encrypted string, otherwise prefer
 * using a character array as that has automatic encryption-at-rest along with some other benefits.
 *
 * @see PasswordCharacterArrayTypeHandler
 *
 * @since 3.21
 */
//not @Named because we register this manually
public class EncryptedStringTypeHandler
    extends CipherAwareTypeHandler<String>
{
  private static final Base64Variant BASE_64 = Base64Variants.getDefaultVariant();

  @Override
  public final void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final String parameter,
      final JdbcType jdbcType) throws SQLException
  {
    ps.setString(parameterIndex, encrypt(parameter));
  }

  @Override
  public final String getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return decrypt(rs.getString(columnName));
  }

  @Override
  public final String getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return decrypt(rs.getString(columnIndex));
  }

  @Override
  public final String getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return decrypt(cs.getString(columnIndex));
  }

  /**
   * Encrypt string using database cipher + Base64.
   */
  private String encrypt(final String value) {
    return BASE_64.encode(cipher().encrypt(value.getBytes(UTF_8)));
  }

  /**
   * Decrypt string using Base64 + database cipher.
   */
  private String decrypt(final String value) {
    return value != null ? new String(cipher().decrypt(BASE_64.decode(value)), UTF_8) : null;
  }
}
