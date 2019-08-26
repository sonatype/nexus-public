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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityId;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * MyBatis {@link TypeHandler} that maps {@link EntityId} to/from SQL strings.
 *
 * @since 3.next
 */
@Named
@Singleton
public class EntityIdTypeHandler
    extends BaseTypeHandler<EntityId>
{
  @Override
  public void setNonNullParameter(final PreparedStatement ps,
                                  final int parameterIndex,
                                  final EntityId parameter,
                                  final JdbcType jdbcType) throws SQLException
  {
    ps.setString(parameterIndex, parameter.getValue());
  }

  @Override
  public EntityId getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return nullableEntityId(rs.getString(columnName));
  }

  @Override
  public EntityId getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return nullableEntityId(rs.getString(columnIndex));
  }

  @Override
  public EntityId getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return nullableEntityId(cs.getString(columnIndex));
  }

  private EntityId nullableEntityId(final String value) {
    return value != null ? EntityId.of(value) : null;
  }
}
