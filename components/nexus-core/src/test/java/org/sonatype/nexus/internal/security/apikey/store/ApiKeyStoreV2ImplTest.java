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
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.datastore.mybatis.handlers.SecretTypeHandler;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.assertj.db.type.Table;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.db.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl.accessKey;
import static org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl.secret;

public class ApiKeyStoreV2ImplTest
    extends TestSupport
{
  private static final String BOB = "bob";

  private static final char[] KEY = "abcdefghijklm".toCharArray();

  private static final char[] KEY2 = "mlkjihgfedcba".toCharArray();

  private static final String NPM = "npm";

  private static final String NUGET = "NuGet";

  private static final PrincipalCollection PRINCIPALS = new SimplePrincipalCollection(BOB, "ldap");

  private static final PrincipalCollection PRINCIPALS2 = new SimplePrincipalCollection("john", "ldap");

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

  @Before
  public void setup() {
    underTest = create(ApiKeyStoreV2Impl.class);

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
    assertThat(token.get().getRealm(), is("ldap"));
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
        .column("domain").hasValues(NUGET)
        .column("username").hasValues(BOB)
        .column("realm").hasValues("ldap")
        .column("access_key").hasValues(accessKey(KEY))
        .column("secret").hasValues("0");

    verify(secretsService).encrypt(NUGET, secret(KEY), BOB);
  }

  @Test
  public void testPersistApiKey_replace() {
    underTest.persistApiKey(NUGET, PRINCIPALS, KEY, null);
    // sanity check
    assertThat(table()).hasNumberOfRows(1);

    underTest.persistApiKey(NUGET, PRINCIPALS, KEY2, null);

    assertThat(table()).hasNumberOfRows(1)
        .column("domain").hasValues(NUGET)
        .column("username").hasValues(BOB)
        .column("realm").hasValues("ldap")
        .column("access_key").hasValues(accessKey(KEY2))
        .column("secret").hasValues("1");
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
        .column("domain").hasValues(NUGET)
        .column("username").hasValues(BOB)
        .column("realm").hasValues("saml")
        .column("access_key").hasValues(accessKey(KEY))
        .column("secret").hasValues("0");
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

  private <T> T create(final Class<T> clazz) {
    return Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      EventManager getEventManager() {
        return eventManager;
      }

      @Provides
      SecretsService getSecretsService() {
        return secretsService;
      }
    }).getInstance(clazz);

  }

  private Table table() {
    DataStore<?> dataStore = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).orElseThrow(RuntimeException::new);
    return new Table(dataStore.getDataSource(), "api_key_v2");
  }
}
