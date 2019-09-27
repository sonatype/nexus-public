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
import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import static com.google.common.base.Preconditions.checkState;

/**
 * MyBatis {@link TypeHandler} that maps UUID-backed {@link EntityId}s to/from SQL.
 *
 * @since 3.19
 */
@Named
@Singleton
public class EntityUUIDTypeHandler
    extends BaseTypeHandler<EntityId>
{
  @Override
  public void setNonNullParameter(final PreparedStatement ps,
                                  final int parameterIndex,
                                  final EntityId parameter,
                                  final JdbcType jdbcType) throws SQLException
  {
    checkState(parameter instanceof EntityUUID, "Expecting EntityUUID");
    ps.setObject(parameterIndex, ((EntityUUID) parameter).uuid());
  }

  @Override
  public EntityId getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return nullableEntityUUID(rs.getObject(columnName, UUID.class));
  }

  @Override
  public EntityId getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return nullableEntityUUID(rs.getObject(columnIndex, UUID.class));
  }

  @Override
  public EntityId getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return nullableEntityUUID(cs.getObject(columnIndex, UUID.class));
  }

  private EntityId nullableEntityUUID(final UUID uuid) {
    return uuid != null ? new EntityUUID(uuid) : null;
  }
}
