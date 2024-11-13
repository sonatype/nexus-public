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
package org.sonatype.nexus.supportzip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.io.SanitizingJsonOutputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;

/**
 * Should be used to hide sensitive data like password in the {@link Map}.
 *
 * @since 3.29
 */
@Named
@Singleton
public class PasswordSanitizing<T extends Map<String, ?>>
    extends ComponentSupport
{
  public static final List<String> SENSITIVE_FIELD_NAMES = ImmutableList.of(
      "applicationPassword",
      "password",
      "systemPassword",
      "keyStorePassword",
      "secret",
      "accessKeyId",
      "secretAccessKey",
      "sessionToken",
      "auth_account_key",
      "aptSigning",
      "bearerToken",
      "yumSigning",
      "accountKey",
      "destinationInstancePassword",
      "NEXUS_DATASTORE_NEXUS_PASSWORD",
      "NEXUS_SECURITY_INITIAL_PASSWORD",
      "nexus.datastore.nexus.password");

  public static final String REPLACEMENT = "**REDACTED**";

  private final TypeReference<T> typeReference = new TypeReference<T>() { };

  private final ObjectWriter attributesJsonWriter = new ObjectMapper().writerFor(typeReference);

  private final ObjectReader attributesJsonReader = new ObjectMapper().readerFor(typeReference);

  /**
   * Replace sensitive data by {@code REPLACEMENT}.
   *
   * @param value sensitive data.
   * @return transformed data.
   */
  @Nullable
  public T transform(T value) {
    try (ByteArrayOutputStream obfuscatedAttrs = new ByteArrayOutputStream()) {
      try (SanitizingJsonOutputStream sanitizer = new SanitizingJsonOutputStream(
          obfuscatedAttrs,
          SENSITIVE_FIELD_NAMES,
          REPLACEMENT)) {
        sanitizer.write(attributesJsonWriter.writeValueAsBytes(value));
      }

      Object result = attributesJsonReader.readValue(obfuscatedAttrs.toByteArray());
      return result != null ? (T) result : null;
    }
    catch (IOException e) {
      log.error("Error obfuscating attributes", e);
    }

    return null;
  }
}
