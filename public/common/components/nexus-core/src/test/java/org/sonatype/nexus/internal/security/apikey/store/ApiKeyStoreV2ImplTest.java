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
package org.sonatype.nexus.internal.security.apikey.store;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.datastore.mybatis.handlers.SecretTypeHandler;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;
import org.sonatype.nexus.security.NexusSimplePrincipalCollection;
import org.sonatype.nexus.security.RealmCaseMapping;
import org.sonatype.nexus.security.anonymous.AnonymousPrincipalCollection;
import org.sonatype.nexus.testdb.DataSessionRule;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.assertj.db.type.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static org.assertj.db.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl.accessKey;
import static org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl.secret;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiKeyStoreV2ImplTest
{
  private static final String BOB = "bob";

  private static final char[] KEY = "abcdefghijklm".toCharArray();

  private static final char[] KEY2 = "mlkjihgfedcba".toCharArray();

  private static final String NPM = "npm";

  private static final String NUGET = "NuGet";

  private static final PrincipalCollection PRINCIPALS = new SimplePrincipalCollection(BOB, "ldap");

  private static final PrincipalCollection PRINCIPALS2 = new SimplePrincipalCollection("john", "ldap");

  private static final PrincipalCollection LEGACY_PRINCIPALS =
      new NexusSimplePrincipalCollection(BOB, new RealmCaseMapping("ldap", true));

  private Map<Integer, Secret> secrets = new HashMap<>();

  private SecretsFactory factory = mock(SecretsFactory.class);

  @Mock
  private EventManager eventManager;

  @Mock
  private SecretsService secretsService;

  @Rule
  public DataSessionRule sessionRule =
      new DataSessionRule().access(ApiKeyV2DAO.class).handle(new SecretTypeHandler(factory));

  private ApiKeyStoreV2Impl underTest;

  private ListAppender<ILoggingEvent> logCaptor;

  @Before
  public void setup() {
    underTest = new ApiKeyStoreV2Impl(sessionRule, secretsService);
    underTest.setDependencies(eventManager);

    when(secretsService.encrypt(any(), any(), any()))
        .thenAnswer(invocation -> {
          Secret secret = mock(Secret.class);
          when(secret.getId()).thenReturn("" + secrets.size());
          when(secret.decrypt()).thenReturn(invocation.getArgument(1, char[].class));
          secrets.put(secrets.size(), secret);
          return secret;
        });
    when(factory.from(any()))
        .thenAnswer(invocation -> secrets.get(Integer.valueOf(invocation.getArgument(0, String.class))));

    logCaptor = new ListAppender<>();
    logCaptor.start();
    ((Logger) LoggerFactory.getLogger(ApiKeyStoreV2Impl.class)).addAppender(logCaptor);
  }

  @After
  public void tearDown() {
    ((Logger) LoggerFactory.getLogger(ApiKeyStoreV2Impl.class)).detachAppender(logCaptor);
  }

  @Test
  public void testGetApiKey_returnsTheOldestRowWhenLegacyAndModernRowsCoexist() {
    OffsetDateTime now = OffsetDateTime.now();
    underTest.persistApiKey(NUGET, LEGACY_PRINCIPALS, KEY, now.minusYears(1));
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, now);

    assertThat(table()).hasNumberOfRows(2);

    // repeated to catch nondeterministic ordering
    for (int i = 0; i < 5; i++) {
      ApiKeyV2Data found = (ApiKeyV2Data) underTest.getApiKey(NUGET, PRINCIPALS).orElseThrow();
      assertThat(found.getAccessKey(), is(accessKey(KEY)));
    }
  }

  @Test
  public void testGetApiKey_returnsTheOldestRowRegardlessOfInsertionOrder() {
    OffsetDateTime now = OffsetDateTime.now();
    // newer row inserted first
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, now);
    underTest.persistApiKey(NUGET, LEGACY_PRINCIPALS, KEY, now.minusYears(1));

    ApiKeyV2Data found = (ApiKeyV2Data) underTest.getApiKey(NUGET, PRINCIPALS).orElseThrow();

    assertThat(found.getAccessKey(), is(accessKey(KEY)));
  }

  @Test
  public void testGetApiKey_warnsWhenDuplicateRowsExistWithoutLeakingKeyMaterial() {
    OffsetDateTime now = OffsetDateTime.now();
    underTest.persistApiKey(NUGET, LEGACY_PRINCIPALS, KEY, now.minusYears(1));
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, now);

    underTest.getApiKey(NUGET, PRINCIPALS);

    List<ILoggingEvent> warnings = logCaptor.list.stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .collect(Collectors.toList());
    assertThat(warnings, hasSize(1));

    String message = warnings.get(0).getFormattedMessage();
    assertThat(message, containsString(BOB));
    assertThat(message, containsString(NUGET));
    assertThat(message, containsString("2"));
    assertFalse(message.contains(new String(KEY)));
    assertFalse(message.contains(accessKey(KEY)));
    assertFalse(message.contains(accessKey(KEY2)));
  }

  @Test
  public void testDeleteApiKey() {
    // test with no records
    assertThat(underTest.deleteApiKey(NUGET, PRINCIPALS), is(0));

    OffsetDateTime keyCreated = OffsetDateTime.now();
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, keyCreated);

    assertThat(underTest.deleteApiKey(NUGET, PRINCIPALS), is(1));
    // a secret associated the removed token should be cleaned up
    verify(secretsService).remove(any(Secret.class));
  }

  @Test
  public void testDeleteApiKey_removesEveryDuplicateRowNotJustTheOldest() {
    OffsetDateTime now = OffsetDateTime.now();
    underTest.persistApiKey(NUGET, LEGACY_PRINCIPALS, KEY, now.minusYears(1));
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, now);
    assertThat(table()).hasNumberOfRows(2);

    assertThat(underTest.deleteApiKey(NUGET, PRINCIPALS), is(2));

    assertThat(table()).hasNumberOfRows(0);
    assertFalse(underTest.getApiKey(NUGET, PRINCIPALS).isPresent());
    verify(secretsService, times(2)).remove(any(Secret.class));
  }

  @Test
  public void testDeleteApiKeys_byExpiration() {
    // test with no records
    assertThat(underTest.deleteApiKeys(OffsetDateTime.now()), is(0));

    // Test with two records, one matches
    OffsetDateTime keyCreated = OffsetDateTime.now();
    underTest.persistApiKey(NPM, PRINCIPALS, KEY, keyCreated.minusYears(1));
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, keyCreated);

    assertThat(underTest.deleteApiKeys(keyCreated.minusDays(1)), is(1));
    // a secret associated the removed token should be cleaned up
    verify(secretsService).remove(any(Secret.class));

    // ensure the correct key (older one) was deleted
    assertThat(table()).hasNumberOfRows(1);
    assertTrue(underTest.getApiKey(NUGET, PRINCIPALS).isPresent());
  }

  @Test
  public void testDeleteApiKeys_byDomain() {
    // test with no records
    assertThat(underTest.deleteApiKeys(NPM), is(0));

    // Test with two records, one matches
    underTest.persistApiKey(NPM, PRINCIPALS, KEY, null);
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, null);

    assertThat(underTest.deleteApiKeys(NPM), is(1));
    // a secret associated the removed token should be cleaned up
    verify(secretsService).remove(any(Secret.class));

    // ensure the correct key (older one) was deleted
    assertThat(table()).hasNumberOfRows(1);
    assertTrue(underTest.getApiKey(NUGET, PRINCIPALS).isPresent());
  }

  @Test
  public void testDeleteApiKeys_byUser() {
    // test with no records
    assertThat(underTest.deleteApiKeys(PRINCIPALS), is(0));

    // Test with two records, one matches
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, null);
    underTest.persistApiKey(NUGET, PRINCIPALS2, KEY2, null);

    assertThat(underTest.deleteApiKeys(PRINCIPALS), is(1));
    // a secret associated the removed token should be cleaned up
    verify(secretsService).remove(any(Secret.class));

    // ensure the correct key (older one) was deleted
    assertThat(table()).hasNumberOfRows(1);
    assertTrue(underTest.getApiKey(NUGET, PRINCIPALS2).isPresent());

  }

  @Test
  public void testGetApiKey() {
    // test with no records
    assertFalse(underTest.getApiKey(NUGET, PRINCIPALS).isPresent());

    // Test with two records, one matches
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, null);
    underTest.persistApiKey(NUGET, PRINCIPALS2, KEY2, null);

    Optional<ApiKeyV2Data> token = underTest.getApiKey(NUGET, PRINCIPALS).map(ApiKeyV2Data.class::cast);
    assertTrue(token.isPresent());
    assertThat(token.get().getApiKey(), is(KEY));
    assertThat(token.get().getCreated(), notNullValue());
    assertThat(token.get().getDomain(), is(NUGET));
    assertThat(token.get().getPrimaryPrincipal(), is(PRINCIPALS.getPrimaryPrincipal()));
    assertThat(token.get().getPrincipals().getRealmNames(), contains("ldap"));
  }

  @Test
  public void testGetApiByToken() {
    // test with no records
    assertFalse(underTest.getApiKey(NUGET, PRINCIPALS).isPresent());

    // Test with two records, one matches
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, null);
    underTest.persistApiKey(NUGET, PRINCIPALS2, KEY2, null);

    Optional<ApiKeyInternal> token = underTest.getApiKeyByToken(NUGET, KEY);
    assertTrue(token.isPresent());

    // mutate the secret part to verify we use it when validating the login
    char[] copiedKey = new char[KEY.length];
    System.arraycopy(KEY, 0, copiedKey, 0, KEY.length);
    copiedKey[KEY.length - 1]++;

    assertFalse(underTest.getApiKeyByToken(NUGET, copiedKey).isPresent());
  }

  @Test
  public void testPersistApiKey() {
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, null);

    assertThat(table()).hasNumberOfRows(1)
        .column("domain")
        .hasValues(NUGET)
        .column("username")
        .hasValues(BOB)
        .column("access_key")
        .hasValues(accessKey(KEY))
        .column("secret")
        .hasValues("0");

    verify(secretsService).encrypt(NUGET, secret(KEY), BOB);
  }

  @Test
  public void testPersistApiKey_replace() {
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, null);
    // sanity check
    assertThat(table()).hasNumberOfRows(1);

    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, null);

    assertThat(table()).hasNumberOfRows(1)
        .column("domain")
        .hasValues(NUGET)
        .column("username")
        .hasValues(BOB)
        .column("access_key")
        .hasValues(accessKey(KEY2))
        .column("secret")
        .hasValues("1");
  }

  @Test
  public void testPersistApiKey_failure() {
    OffsetDateTime now = OffsetDateTime.now();
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, now);
    // sanity check
    assertThat(table()).hasNumberOfRows(1);

    // Attempts to overwrite with an older key should fail
    assertThrows(DuplicateKeyException.class, () -> underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, now.minusDays(1)));

    verify(secretsService).remove(secrets.get(1));
  }

  @Test
  public void testUpdateApiKeyRealm() {
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY);
    reset(secretsService);
    // sanity check
    assertThat(table()).hasNumberOfRows(1);

    ApiKeyInternal key = underTest.getApiKey(NUGET, PRINCIPALS).get();

    underTest.updateApiKey(key, new SimplePrincipalCollection(BOB, "saml"));
    verifyNoInteractions(secretsService);

    assertThat(table()).hasNumberOfRows(1)
        .column("domain")
        .hasValues(NUGET)
        .column("username")
        .hasValues(BOB)
        .column("access_key")
        .hasValues(accessKey(KEY))
        .column("secret")
        .hasValues("0");
  }

  @Test
  public void testAccessKey() {
    // Reminder changes here should also affect testSecret below
    // even number
    assertThat(ApiKeyStoreV2Impl.accessKey("abcdef".toCharArray()), is("abc"));
    // odd number
    assertThat(ApiKeyStoreV2Impl.accessKey("abcdefg".toCharArray()), is("abc"));
  }

  @Test
  public void testSecret() {
    // Reminder changes here should also affect testAccessKey above
    // even number
    assertThat(ApiKeyStoreV2Impl.secret("abcdef".toCharArray()), is("def".toCharArray()));
    // odd number
    assertThat(ApiKeyStoreV2Impl.secret("abcdefg".toCharArray()), is("defg".toCharArray()));
  }

  @Test
  public void testPersistApiKeyWithCreatorTracking() {
    underTest.persistApiKey(NPM, PRINCIPALS, KEY, OffsetDateTime.now(), "adminUser", "AdminRealm");

    Optional<ApiKeyInternal> result = underTest.getApiKey(NPM, PRINCIPALS);

    assertTrue(result.isPresent());
    assertThat(result.get().getCreatedByUserId(), is("adminUser"));
    assertThat(result.get().getCreatedByRealm(), is("AdminRealm"));
  }

  @Test
  public void testPersistApiKeyWithoutCreatorTracking() {
    underTest.persistApiKey(NPM, PRINCIPALS, KEY, OffsetDateTime.now(), null, null);

    Optional<ApiKeyInternal> result = underTest.getApiKey(NPM, PRINCIPALS);

    assertTrue(result.isPresent());
    assertThat(result.get().getCreatedByUserId(), is((String) null));
    assertThat(result.get().getCreatedByRealm(), is((String) null));
  }

  @Test
  public void testGetApiKey_findsRowStoredWithLegacyOrientPrincipals() {
    underTest.persistApiKey(NUGET, LEGACY_PRINCIPALS, KEY, OffsetDateTime.now());

    Optional<ApiKeyInternal> found = underTest.getApiKey(NUGET, PRINCIPALS);

    assertTrue(found.isPresent());
    assertThat(((ApiKeyV2Data) found.get()).getAccessKey(), is(accessKey(KEY)));
    // no replacement row was created
    assertThat(table()).hasNumberOfRows(1);
  }

  @Test
  public void testDeleteApiKey_removesRowStoredWithLegacyOrientPrincipals() {
    underTest.persistApiKey(NUGET, LEGACY_PRINCIPALS, KEY, OffsetDateTime.now());

    assertThat(underTest.deleteApiKey(NUGET, PRINCIPALS), is(1));

    assertThat(table()).hasNumberOfRows(0);
    verify(secretsService).remove(any(Secret.class));
  }

  @Test
  public void testGetApiKey_doesNotMatchTheSameUsernameInAnotherRealm() {
    PrincipalCollection ldap = new SimplePrincipalCollection(BOB, "ldap");
    PrincipalCollection local = new SimplePrincipalCollection(BOB, "local");
    underTest.persistApiKey(NUGET, ldap, KEY, OffsetDateTime.now());
    underTest.persistApiKey(NUGET, local, KEY2, OffsetDateTime.now());

    assertThat(((ApiKeyV2Data) underTest.getApiKey(NUGET, ldap).orElseThrow()).getAccessKey(),
        is(accessKey(KEY)));
    assertThat(((ApiKeyV2Data) underTest.getApiKey(NUGET, local).orElseThrow()).getAccessKey(),
        is(accessKey(KEY2)));
  }

  @Test
  public void testGetApiKey_multiRealmRowIsNotFoundBySingleRealmLookup() {
    SimplePrincipalCollection multiRealm = new SimplePrincipalCollection(BOB, "rut");
    multiRealm.add(BOB, "ldap");
    underTest.persistApiKey(NUGET, multiRealm, KEY, OffsetDateTime.now());

    assertFalse(underTest.getApiKey(NUGET, new SimplePrincipalCollection(BOB, "rut")).isPresent());
  }

  @Test
  public void testGetApiKey_anonymousPrincipalsStillMatch() {
    underTest.persistApiKey(NUGET, new AnonymousPrincipalCollection(BOB, "anon"), KEY, OffsetDateTime.now());

    assertTrue(underTest.getApiKey(NUGET, new SimplePrincipalCollection(BOB, "anon")).isPresent());
  }

  private Table table() {
    DataStore<?> dataStore = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).orElseThrow(RuntimeException::new);
    return new Table(dataStore.getDataSource(), "api_key_v2");
  }
}
