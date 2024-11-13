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

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Abstract {@link TypeHandler} that supports mapping of Java objects to/from SQL as JSON.
 *
 * Automatically encrypts/decrypts sensitive fields as they are persisted to/from the config store.
 * {@link AbstractRawJsonTypeHandler} should be used if the Java object handles encryption itself.
 *
 * Sub-classes can also choose to encrypt the entire JSON document at-rest with the database cipher.
 *
 * @since 3.21
 */
@SuppressWarnings("unchecked")
public abstract class AbstractJsonTypeHandler<T>
    extends CipherAwareTypeHandler<T>
{
  private static final Base64Variant BASE_64 = Base64Variants.getDefaultVariant();

  private final ObjectMapper objectMapper;

  private final JavaType jsonType;

  protected AbstractJsonTypeHandler() {
    this.objectMapper = buildObjectMapper(() -> new ObjectMapper());
    this.jsonType = objectMapper.constructType(getJsonType()); // NOSONAR
  }

  /**
   * Override this method to customize the {@link ObjectMapper} used for JSON serialization.
   *
   * @param mapperFactory Factory that supplies prototype mappers for further customization
   */
  protected ObjectMapper buildObjectMapper(final Supplier<ObjectMapper> mapperFactory) {
    return mapperFactory.get().registerModule(new JavaTimeModule());
  }

  /**
   * Override this method to customize the Java type used to select the JSON serialization.
   */
  protected Type getJsonType() {
    return getRawType();
  }

  /**
   * Override this method to call {@link #writeToEncryptedJson} if you want to store encrypted JSON.
   */
  protected byte[] writeToJson(final Object value) throws SQLException {
    return writeToPlainJson(value);
  }

  /**
   * Override this method to call {@link #readFromEncryptedJson} if you want to store encrypted JSON.
   */
  @Nullable
  protected Object readFromJson(@Nullable final byte[] json) throws SQLException {
    return readFromPlainJson(json);
  }

  /**
   * Override this method if you want to customize exactly how the object is serialized to plain JSON.
   */
  protected byte[] writeToPlainJson(final Object value) throws SQLException {
    try {
      return objectMapper.writeValueAsBytes(value);
    }
    catch (IOException e) {
      throw new SQLException(e);
    }
  }

  /**
   * Override this method if you want to customize exactly how the object is deserialized from plain JSON.
   */
  @Nullable
  protected Object readFromPlainJson(@Nullable final byte[] json) throws SQLException {
    try {
      return json != null ? objectMapper.readValue(json, jsonType) : null;
    }
    catch (IOException e) {
      throw new SQLException(e);
    }
  }

  /**
   * Calls {@link #writeToPlainJson} before encrypting the contents.
   */
  protected final byte[] writeToEncryptedJson(final Object value) throws SQLException {
    return BASE_64.encode(cipher().encrypt(writeToPlainJson(value)), true).getBytes(UTF_8);
  }

  /**
   * Calls {@link #readFromPlainJson} after decrypting the contents.
   */
  @Nullable
  protected final Object readFromEncryptedJson(@Nullable final byte[] json) throws SQLException {
    byte[] plain = json;
    if (json != null) {
      checkState(json.length >= 6 && json[0] == '\"', "Expected a Base64 JSON string");
      plain = cipher().decrypt(BASE_64.decode(new String(json, 1, json.length - 2, UTF_8)));
    }
    return readFromPlainJson(plain);
  }

  @Override
  public final void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final T parameter,
      final JdbcType jdbcType) throws SQLException
  {
    byte[] json = writeToJson(parameter);
    if (ps.isWrapperFor(org.h2.jdbc.JdbcPreparedStatement.class)) {
      // H2 only accepts JSON passed as UTF8 byte array
      ps.setBytes(parameterIndex, json);
    }
    else {
      // while PostgreSQL and other DBs want a UTF8 string
      ps.setString(parameterIndex, new String(json, UTF_8));
    }
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return (T) readFromJson(rs.getBytes(columnName)); // works for both byte[] and UTF8 strings
  }

  @Override
  public final T getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return (T) readFromJson(rs.getBytes(columnIndex)); // works for both byte[] and UTF8 strings
  }

  @Override
  public final T getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
    return (T) readFromJson(cs.getBytes(columnIndex)); // works for both byte[] and UTF8 strings
  }
}
