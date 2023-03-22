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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Intended for use for reading from Quartz
 */
public abstract class AbstractBytesTypeHandler<T>
    extends BaseTypeHandler<T>
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Supplier<T> defaultValueSupplier;

  /**
   * @param defaultValueSupplier a Supplier to use when the database has a null value stored
   */
  protected AbstractBytesTypeHandler(final Supplier<T> defaultValueSupplier) {
    this.defaultValueSupplier = checkNotNull(defaultValueSupplier);
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return deserialize(Optional.ofNullable(rs.getBytes(columnName)));
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return deserialize(Optional.ofNullable(rs.getBytes(columnIndex)));
  }

  @Override
  public final T getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return deserialize(Optional.ofNullable(cs.getBytes(columnIndex)));
  }

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int i,
      final T parameter,
      final JdbcType jdbcType) throws SQLException
  {
    throw new UnsupportedOperationException("Unimplemented");
  }

  /**
   * @param in a non-null InputStream, MUST be closed by the implementer
   */
  protected abstract T deserialize(InputStream in);

  private T deserialize(final Optional<byte[]> blob) {
    return blob.map(ByteArrayInputStream::new)
        .map(this::deserialize)
        .orElseGet(defaultValueSupplier::get);
  }
}
