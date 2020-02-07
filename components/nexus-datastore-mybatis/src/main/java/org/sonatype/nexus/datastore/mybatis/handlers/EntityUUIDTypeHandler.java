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

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import static java.util.UUID.fromString;

/**
 * MyBatis {@link TypeHandler} that maps UUID-backed {@link EntityId}s to/from SQL.
 *
 * @since 3.19
 */
// not @Named because we register this manually
public class EntityUUIDTypeHandler
    extends BaseTypeHandler<EntityId>
{
  private final boolean lenient;

  public EntityUUIDTypeHandler(final boolean lenient) {
    this.lenient = lenient; // pass UUIDs by string when we're being lenient
  }

  @Override
  public void setNonNullParameter(final PreparedStatement ps,
                                  final int parameterIndex,
                                  final EntityId parameter,
                                  final JdbcType jdbcType)
      throws SQLException
  {
    UUID uuid;
    if (parameter instanceof EntityUUID) {
      uuid = ((EntityUUID) parameter).uuid();
    }
    else {
      uuid = UUID.fromString(parameter.getValue());
    }

    if (lenient) {
      ps.setString(parameterIndex, uuid.toString());
    }
    else {
      ps.setObject(parameterIndex, uuid);
    }
  }

  @Override
  public EntityUUID getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return nullableEntityUUID(rs.getObject(columnName));
  }

  @Override
  public EntityUUID getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return nullableEntityUUID(rs.getObject(columnIndex));
  }

  @Override
  public EntityUUID getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return nullableEntityUUID(cs.getObject(columnIndex));
  }

  @Nullable
  private EntityUUID nullableEntityUUID(@Nullable final Object uuid) {
    // expect UUIDs but also accept strings
    if (uuid instanceof UUID) {
      return new EntityUUID((UUID) uuid);
    }
    if (uuid instanceof String) {
      return new EntityUUID(fromString((String) uuid));
    }
    return null;
  }
}
