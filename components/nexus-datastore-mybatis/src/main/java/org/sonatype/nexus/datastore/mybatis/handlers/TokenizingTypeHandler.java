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
import java.util.Collection;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Simplistic tokenizer which splits strings using the following characters: . - /
 */
public class TokenizingTypeHandler
    extends BaseTypeHandler<Collection<String>>
{
  private static final String SEPARATOR = " ";

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final Collection<String> parameter,
      final JdbcType jdbcType) throws SQLException
  {
    ps.setString(parameterIndex, tokenize(toString(parameter)));
  }

  private static String tokenize(final String token) {
    return token.replaceAll("[.-/]", SEPARATOR);
  }

  private static String toString(final Collection<String> values) {
    if (values.isEmpty()) {
      return "";
    }
    else if (values.size() == 1) {
      return Iterables.getOnlyElement(values);
    }
    return values.stream().collect(Collectors.joining(SEPARATOR));
  }

  @Override
  public Collection<String> getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<String> getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<String> getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    throw new UnsupportedOperationException();
  }
}
