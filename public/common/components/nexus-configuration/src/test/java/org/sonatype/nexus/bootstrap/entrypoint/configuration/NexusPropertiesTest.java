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
package org.sonatype.nexus.bootstrap.entrypoint.configuration;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link NexusProperties#enforceCommunityEditionAnalytics()} method.
 * Uses the @VisibleForTesting constructor to bypass file-based initialization.
 */

class NexusPropertiesTest
{
  private static final String ANALYTICS_PROPERTY = "nexus.analytics.enabled";

  private static final String TRUE = "true";

  // Property keys for redaction tests
  private static final String MIGRATION_CIPHER_PASSWORD = "nexus.migrator.cipher.password";

  private static final String MIGRATION_CIPHER_EXPORT_PASSWORD = "nexus.migrator.cipher.export.password";

  private static final String DATABASE_PASSWORD = "database.password";

  private static final String API_SECRET = "api.secret";

  private static final String NEXUS_SECRET_KEY = "nexus.secret.key";

  private static final String BEARER_TOKEN = "bearer.token";

  private static final String ACCESS_TOKEN = "access_token";

  private static final String ENCRYPTION_CIPHER = "encryption.cipher";

  private static final String USER_CREDENTIAL = "user.credential";

  private static final String API_TOKEN = "api.token";

  private static final String DATABASE_SECRET = "database.secret";

  private static final String NORMAL_PROPERTY = "normal.property";

  private static final String APPLICATION_PORT = "application.port";

  private static final String ANOTHER_PASSWORD = "another.password";

  @BeforeEach
  void setUp() {
    // Clear the analytics system property before each test
    System.clearProperty(ANALYTICS_PROPERTY);
  }

  @AfterEach
  void tearDown() {
    // Clean up after tests
    System.clearProperty(ANALYTICS_PROPERTY);
  }

  @Test
  void enforceCommunityEditionAnalytics_SetsPropertyToTrue() {
    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    assertThat(nexusProperties.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  @Test
  void enforceCommunityEditionAnalytics_SetsSystemPropertyToTrue() {
    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    assertThat(System.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  @Test
  void enforceCommunityEditionAnalytics_OverridesExistingFalseValue() {
    // Set the system property to false initially
    System.setProperty(ANALYTICS_PROPERTY, "false");

    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    // Both the NexusProperties and System property should now be true
    assertThat(nexusProperties.getProperty(ANALYTICS_PROPERTY), is(TRUE));
    assertThat(System.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  /**
   * Helper method to create a NexusProperties instance for testing.
   * Uses the @VisibleForTesting constructor that bypasses file loading.
   */
  private NexusProperties createNexusProperties() {
    // Use the test constructor that skips file-based initialization
    return new NexusProperties(new PropertyMap());
  }

  // Tests for property redaction functionality

  @Test
  void isSensitiveProperty_RedactsPasswordProperties() {
    assertThat(NexusProperties.isSensitiveProperty(MIGRATION_CIPHER_PASSWORD), is(true));
    assertThat(NexusProperties.isSensitiveProperty(MIGRATION_CIPHER_EXPORT_PASSWORD), is(true));
    assertThat(NexusProperties.isSensitiveProperty(DATABASE_PASSWORD), is(true));
    assertThat(NexusProperties.isSensitiveProperty("PASSWORD"), is(true));
  }

  @Test
  void isSensitiveProperty_RedactsSecretProperties() {
    assertThat(NexusProperties.isSensitiveProperty(API_SECRET), is(true));
    assertThat(NexusProperties.isSensitiveProperty(NEXUS_SECRET_KEY), is(true));
    assertThat(NexusProperties.isSensitiveProperty("SECRET_VALUE"), is(true));
  }

  @Test
  void isSensitiveProperty_RedactsTokenProperties() {
    assertThat(NexusProperties.isSensitiveProperty(BEARER_TOKEN), is(true));
    assertThat(NexusProperties.isSensitiveProperty(ACCESS_TOKEN), is(true));
    assertThat(NexusProperties.isSensitiveProperty("TOKEN"), is(true));
  }

  @Test
  void isSensitiveProperty_RedactsCipherProperties() {
    assertThat(NexusProperties.isSensitiveProperty(ENCRYPTION_CIPHER), is(true));
    assertThat(NexusProperties.isSensitiveProperty("CIPHER_KEY"), is(true));
  }

  @Test
  void isSensitiveProperty_RedactsCredentialProperties() {
    assertThat(NexusProperties.isSensitiveProperty(USER_CREDENTIAL), is(true));
    assertThat(NexusProperties.isSensitiveProperty("CREDENTIALS"), is(true));
  }

  @Test
  void isSensitiveProperty_AllowsNonSensitiveProperties() {
    assertThat(NexusProperties.isSensitiveProperty(ANALYTICS_PROPERTY), is(false));
    assertThat(NexusProperties.isSensitiveProperty(APPLICATION_PORT), is(false));
    assertThat(NexusProperties.isSensitiveProperty("karaf.home"), is(false));
    assertThat(NexusProperties.isSensitiveProperty("java.version"), is(false));
  }

  @Test
  void isSensitiveProperty_HandlesNullKey() {
    assertThat(NexusProperties.isSensitiveProperty(null), is(false));
  }

  @Test
  void isSensitiveProperty_CaseInsensitive() {
    assertThat(NexusProperties.isSensitiveProperty("PaSsWoRd"), is(true));
    assertThat(NexusProperties.isSensitiveProperty("SeCrEt"), is(true));
    assertThat(NexusProperties.isSensitiveProperty("ToKeN"), is(true));
  }

  @Test
  void redactSensitiveProperties_RedactsPasswordValues() {
    PropertyMap props = new PropertyMap();
    props.put(MIGRATION_CIPHER_PASSWORD, "EZTQdWGtzkUS0r0LbSCAB8iYMxM008hnRM6o8akSkds=");
    props.put(NORMAL_PROPERTY, "normalValue");

    Map<String, String> redacted = NexusProperties.redactSensitiveProperties(props);

    assertThat(redacted.get(MIGRATION_CIPHER_PASSWORD), is(NexusProperties.REDACTED));
    assertThat(redacted.get(NORMAL_PROPERTY), is("normalValue"));
  }

  @Test
  void redactSensitiveProperties_RedactsMultipleSensitiveProperties() {
    PropertyMap props = new PropertyMap();
    props.put(MIGRATION_CIPHER_PASSWORD, "secret123");
    props.put(MIGRATION_CIPHER_EXPORT_PASSWORD, "secret456");
    props.put(API_TOKEN, "token789");
    props.put(DATABASE_SECRET, "dbsecret");
    props.put(NORMAL_PROPERTY, "normalValue");
    props.put(APPLICATION_PORT, "8081");

    Map<String, String> redacted = NexusProperties.redactSensitiveProperties(props);

    assertThat(redacted.get(MIGRATION_CIPHER_PASSWORD), is(NexusProperties.REDACTED));
    assertThat(redacted.get(MIGRATION_CIPHER_EXPORT_PASSWORD), is(NexusProperties.REDACTED));
    assertThat(redacted.get(API_TOKEN), is(NexusProperties.REDACTED));
    assertThat(redacted.get(DATABASE_SECRET), is(NexusProperties.REDACTED));
    assertThat(redacted.get(NORMAL_PROPERTY), is("normalValue"));
    assertThat(redacted.get(APPLICATION_PORT), is("8081"));
  }

  @Test
  void redactSensitiveProperties_DoesNotModifyOriginalMap() {
    PropertyMap props = new PropertyMap();
    props.put(MIGRATION_CIPHER_PASSWORD, "secret123");
    props.put(NORMAL_PROPERTY, "normalValue");

    Map<String, String> redacted = NexusProperties.redactSensitiveProperties(props);

    // Original map should be unchanged
    assertThat(props.get(MIGRATION_CIPHER_PASSWORD), is("secret123"));
    assertThat(props.get(NORMAL_PROPERTY), is("normalValue"));

    // Redacted map should have redacted value
    assertThat(redacted.get(MIGRATION_CIPHER_PASSWORD), is(NexusProperties.REDACTED));
    assertThat(redacted.get(NORMAL_PROPERTY), is("normalValue"));
  }

  @Test
  void redactSensitiveProperties_HandlesEmptyMap() {
    PropertyMap props = new PropertyMap();

    Map<String, String> redacted = NexusProperties.redactSensitiveProperties(props);

    assertThat(redacted.isEmpty(), is(true));
  }

  @Test
  void redactSensitiveProperties_PreservesAllKeys() {
    PropertyMap props = new PropertyMap();
    props.put(MIGRATION_CIPHER_PASSWORD, "secret123");
    props.put(NORMAL_PROPERTY, "normalValue");
    props.put(ANOTHER_PASSWORD, "secret456");

    Map<String, String> redacted = NexusProperties.redactSensitiveProperties(props);

    assertThat(redacted.size(), is(props.size()));
    assertThat(redacted.containsKey(MIGRATION_CIPHER_PASSWORD), is(true));
    assertThat(redacted.containsKey(NORMAL_PROPERTY), is(true));
    assertThat(redacted.containsKey(ANOTHER_PASSWORD), is(true));
  }
}
