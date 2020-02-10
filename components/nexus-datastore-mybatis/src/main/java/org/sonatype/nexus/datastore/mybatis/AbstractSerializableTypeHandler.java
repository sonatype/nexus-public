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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.sonatype.nexus.common.io.ObjectInputStreamWithClassLoader;
import org.sonatype.nexus.common.io.ObjectInputStreamWithClassLoader.LoadingFunction;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract {@link TypeHandler} that supports serializing Java objects to the database.
 *
 * The resulting bytes are automatically encrypted at-rest with the database cipher.
 *
 * @since 3.21
 */
public abstract class AbstractSerializableTypeHandler<T>
    extends CipherAwareTypeHandler<T>
{
  private static final Logger log = LoggerFactory.getLogger(AbstractSerializableTypeHandler.class);

  private final LoadingFunction classLoading;

  protected AbstractSerializableTypeHandler() {
    ClassLoader loader = getClass().getClassLoader();
    this.classLoading = name -> {
      try {
        return Class.forName(name, false, loader);
      }
      catch (Exception | LinkageError e) {
        log.debug("Handler cannot see serialized type, trying thread context loader...", e);
        return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
      }
    };
  }

  protected AbstractSerializableTypeHandler(final LoadingFunction classLoading) {
    this.classLoading = checkNotNull(classLoading); // custom class loading
  }

  @Override
  public final void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final T parameter,
      final JdbcType jdbcType) throws SQLException
  {
    ps.setBytes(parameterIndex, serialize(parameter));
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return deserialize(rs.getBytes(columnName));
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return deserialize(rs.getBytes(columnIndex));
  }

  @Override
  public final T getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return deserialize(cs.getBytes(columnIndex));
  }

  /**
   * Serialize the Java object to bytes and then encrypt them using the database cipher.
   */
  private byte[] serialize(final T object) throws SQLException {
    try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(buf)) {
      out.writeObject(object);
      return cipher().encrypt(buf.toByteArray());
    }
    catch (IOException e) {
      throw new SQLException("Problem serializing: " + object.getClass().getName(), e);
    }
  }

  /**
   * Decrypt persisted bytes using the database cipher and deserialize them to a Java object.
   */
  @SuppressWarnings("unchecked")
  private T deserialize(final byte[] bytes) throws SQLException {
    if (bytes == null) {
      return null;
    }

    byte[] decrypted = cipher().decrypt(bytes);
    try (ByteArrayInputStream buf = new ByteArrayInputStream(decrypted);
        ObjectInputStream in = new ObjectInputStreamWithClassLoader(buf, classLoading)) {
      return (T) in.readObject();
    }
    catch (IOException | ClassNotFoundException e) {
      throw new SQLException("Problem deserializing: " + getRawType(), e);
    }
  }
}
