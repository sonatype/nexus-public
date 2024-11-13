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

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.REPLACEMENT;

/**
 * Tests for {@link PasswordSanitizing}
 */
public class PasswordSanitizingTest
{
  private final String PASSWORD = "admin123";

  private final Map<String, String> SENSITIVE_DATA = Stream.of(
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
          "NEXUS_SECURITY_INITIAL_PASSWORD")
      .collect(Collectors.toMap(key -> key, key -> PASSWORD));

  @Test
  public void testSanitizingSensitiveData() {
    PasswordSanitizing<Map<String, String>> passwordSanitizing = new PasswordSanitizing<>();
    Map<String, String> sanitized = passwordSanitizing.transform(SENSITIVE_DATA);
    assertNotNull(sanitized);
    assertThat(sanitized.toString(), not(containsString(PASSWORD)));
  }

  @Test
  public void testPlainDataIsNotChanged() {
    PasswordSanitizing<Map<String, String>> passwordSanitizing = new PasswordSanitizing<>();
    Map<String, String> sensitiveData = ImmutableMap.of(
        "name", "John",
        "lastName", "Doe",
        "password", PASSWORD);
    Map<String, String> sanitized = passwordSanitizing.transform(sensitiveData);
    assertNotNull(sanitized);
    Map<String, String> expected = ImmutableMap.of(
        "name", "John",
        "lastName", "Doe",
        "password", REPLACEMENT);
    assertThat(sanitized, is(expected));
  }
}
