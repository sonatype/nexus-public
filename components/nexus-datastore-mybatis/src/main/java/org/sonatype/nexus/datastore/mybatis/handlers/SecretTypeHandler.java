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
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class SecretTypeHandler
    extends BaseTypeHandler<Secret>
{
  private final SecretsFactory factory;

  @Inject
  public SecretTypeHandler( final SecretsFactory factory) {
    this.factory = checkNotNull(factory);
  }

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int i,
      final Secret parameter,
      final JdbcType jdbcType) throws SQLException
  {
    ps.setString(i, parameter.getId());
  }

  @Override
  public Secret getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return from(rs.getString(columnName));
  }

  @Override
  public Secret getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return from(rs.getString(columnIndex));
  }

  @Override
  public Secret getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return from(cs.getString(columnIndex));
  }

  @Nullable
  private Secret from(@Nullable final String secret) {
    return Optional.ofNullable(secret)
        .map(factory::from)
        .orElse(null);
  }
}
