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
package org.sonatype.nexus.repository.npm.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.InvalidContentException;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;

/**
 * npm helper for serializing JSON npm metadata.
 *
 * @since 3.0
 */
public final class NpmJsonUtils
{
  public static final TypeReference<HashMap<String, Object>> rawMapJsonTypeRef;

  static final TypeReference<List<Object>> rawListJsonTypeRef;

  public static final ObjectMapper mapper;

  static {
    rawMapJsonTypeRef = new TypeReference<HashMap<String, Object>>() {
      // nop
    };
    rawListJsonTypeRef = new TypeReference<List<Object>>() {
      // nop
    };

    mapper = new ObjectMapper();
    mapper.disable(Feature.AUTO_CLOSE_TARGET);
  }

  private NpmJsonUtils() {
    // nop
  }

  /**
   * Parses JSON content into map.
   */
  @Nonnull
  public static NestedAttributesMap parse(final Supplier<InputStream> streamSupplier) throws IOException {
    try {
      final Map<String, Object> backing =
          mapper.<HashMap<String, Object>>readValue(streamSupplier.get(), rawMapJsonTypeRef);
      return new NestedAttributesMap(String.valueOf(backing.get(NpmMetadataUtils.NAME)), backing);
    }
    catch (JsonParseException e) {
      // fallback
      if (e.getMessage().contains("Invalid UTF-8")) {
        // try again, but assume ISO8859-1 encoding now, that is illegal for JSON
        final Map<String, Object> backing =
            mapper.<HashMap<String, Object>>readValue(
                new InputStreamReader(streamSupplier.get(), StandardCharsets.ISO_8859_1),
                rawMapJsonTypeRef
            );
        return new NestedAttributesMap(String.valueOf(backing.get(NpmMetadataUtils.NAME)), backing);
      }
      throw new InvalidContentException("Invalid JSON input", e);
    }
  }

  /**
   * Serializes input map as JSON into given {@link Writer}.
   */
  public static void serialize(final Writer out, final NestedAttributesMap packageRoot) {
    try {
      mapper.writeValue(out, packageRoot.backing());
    }
    catch (IOException e) {
      // mapping broken? we do not use mapping
      throw new RuntimeException(e);
    }
  }

  /**
   * Serializes input map as JSON into byte array.
   */
  @Nonnull
  public static byte[] bytes(final NestedAttributesMap packageRoot) {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8);
    serialize(writer, packageRoot);
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Creates input stream supplier out of passed in byte array content.
   */
  @Nonnull
  static Supplier<InputStream> supplier(final byte[] content) throws IOException {
    return () -> new ByteArrayInputStream(content);
  }
}
