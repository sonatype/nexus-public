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
package org.sonatype.nexus.internal.security.apikey.orient;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.RandomBytesGeneratorImpl;
import org.sonatype.nexus.internal.security.apikey.DefaultApiKeyFactory;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKey;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;

import com.google.common.collect.Maps;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;

/**
 * Tests {@link OrientApiKeyStore}
 */
public class OrientApiKeyStoreTest
    extends TestSupport
{
  private static final String PRINCIPAL_A_NAME = "name-a";

  private static final String PRINCIPAL_A_DOMAIN = "foo";

  private static final String PRINCIPAL_B_NAME = "name-b";

  private static final String PRINCIPAL_B_DOMAIN = "bar";

  private static final boolean CANCELLED = true;

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Rule
  public DatabaseInstanceRule database2 = DatabaseInstanceRule.inMemory("nested");

  @Mock
  private UserPrincipalsHelper principalsHelper;

  private AtomicBoolean cancelled = new AtomicBoolean(!CANCELLED);

  private OrientApiKeyStore underTest;

  @Before
  public void setup() throws Exception {
    CancelableHelper.set(cancelled);
    underTest = new OrientApiKeyStore(
        database.getInstanceProvider(),
        new OrientApiKeyEntityAdapter(ClassLoader.getSystemClassLoader()),
        principalsHelper,
        Maps.newHashMap(),
        new DefaultApiKeyFactory(new RandomBytesGeneratorImpl(new CryptoHelperImpl()))
    );
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
      underTest = null;
    }
    CancelableHelper.remove();
  }

  /*
   * Can create and read an API key
   */
  @Test
  public void testCreateAndFetch() {
    PrincipalCollection p = makePrincipals("name");
    char[] key = underTest.createApiKey("foo", p);
    Optional<ApiKey> fetchedKey = underTest.getApiKey("foo", p);

    assertTrue(fetchedKey.isPresent());
    assertThat(fetchedKey.get().getApiKey(), equalTo(key));
  }

  /*
   * Can persist and read an API key with provided value
   */
  @Test
  public void testPersistAndRead() {
    PrincipalCollection p = makePrincipals("name");
    char[] key = new char[]{'a', 'b', 'c', 'd'};
    underTest.persistApiKey("foo", p, key);
    Optional<ApiKey> fetchedKey = underTest.getApiKey("foo", p);

    assertTrue(fetchedKey.isPresent());
    assertThat(fetchedKey.get().getApiKey(), equalTo(key));
  }

  /*
   * Cannot read cross-domain API key
   */
  @Test
    public void testCannotReadCrossDomain() {
    PrincipalCollection p = makePrincipals("name");
    underTest.createApiKey("foo", p);
    Optional<ApiKey> fetchedKey = underTest.getApiKey("bar", p);
    assertFalse(fetchedKey.isPresent());
  }

  /*
   * Can create and delete an API Key
   */
  @Test
  public void testCreateAndDelete() {
    PrincipalCollection p = makePrincipals("name");

    underTest.createApiKey("foo", p);
    underTest.deleteApiKey("foo", p);

    Optional<ApiKey> fetchedKey = underTest.getApiKey("foo", p);
    assertFalse(fetchedKey.isPresent());
  }

  /*
   * Find by principal name
   */
  @Test
  public void testFindByPrincipalName() {
    char[] alphakey = underTest.createApiKey("foo", makePrincipals("alpha"));
    underTest.createApiKey("foo", makePrincipals("beta"));
    underTest.createApiKey("foo", makePrincipals("gamma"));

    Optional<ApiKey> key = underTest.getApiKey("foo", makePrincipals("alpha"));

    assertTrue(key.isPresent());
    assertThat(key.get().getApiKey(), equalTo(alphakey));
  }

  /*
   * Find by api key
   */
  @Test
  public void testFindByApiKey() {
    char[] key = underTest.createApiKey("foo", makePrincipals("alpha"));
    underTest.createApiKey("foo", makePrincipals("beta"));
    underTest.createApiKey("foo", makePrincipals("gamma"));

    PrincipalCollection principals = underTest.getApiKeyByToken("foo", key).map(ApiKey::getPrincipals).orElse(null);

    assertThat(principals.getPrimaryPrincipal(), equalTo("alpha"));
  }

  @Test
  public void testDeleteAllApiKeys() {
    PrincipalCollection principalA = makePrincipals("name-a");
    PrincipalCollection principalB = makePrincipals("name-b");

    underTest.createApiKey("foo", principalA);
    underTest.createApiKey("bar", principalB);
    underTest.deleteApiKeys();

    assertThat(underTest.getApiKey("foo", principalA), equalTo(Optional.empty()));
    assertThat(underTest.getApiKey("bar", principalB), equalTo(Optional.empty()));
  }

  @Test
//  void 'Get api key if already exists'() {
  public void testGetApiKey_existing() {
    PrincipalCollection principalA = makePrincipals("name-a");

    char[] key = underTest.createApiKey("foo", principalA);
    char[] key2 = underTest.createApiKey("foo", principalA);

    assertThat(key2, equalTo(key));
  }

  @Test
  public void testCanPurgeOrphanedApiKeys() throws UserNotFoundException {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME);
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME);
    char[] apiKeyForPrincipalA = underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA);
    underTest.createApiKey(PRINCIPAL_B_NAME, principalB);
    when(principalsHelper.getUserStatus(principalA)).thenReturn(UserStatus.disabled);
    when(principalsHelper.getUserStatus(principalB)).
        thenThrow(new UserNotFoundException(principalB.getPrimaryPrincipal().toString()));

    underTest.purgeApiKeys();

    //Verify that api keys that belong to non-existent users are purged
    assertThat(underTest.getApiKey(PRINCIPAL_A_DOMAIN, principalA).get().getApiKey(), equalTo(apiKeyForPrincipalA));
    assertThat(underTest.getApiKey(PRINCIPAL_B_DOMAIN, principalB), is(Optional.empty()));
  }

  @Test
  public void testPurgeOrphanedApiKeys_cancelable() throws UserNotFoundException {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME);
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME);
    char[] apiKeyForPrincipalA = underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA);
    char[] apiKeyForPrincipalB = underTest.createApiKey(PRINCIPAL_B_DOMAIN, principalB);
    when(principalsHelper.getUserStatus(principalA)).
        thenThrow(new UserNotFoundException(principalA.getPrimaryPrincipal().toString()));
    when(principalsHelper.getUserStatus(principalB)).
        thenThrow(new UserNotFoundException(principalB.getPrimaryPrincipal().toString()));
    cancelled.set(CANCELLED);

    try {
      underTest.purgeApiKeys();
      fail("Expected exception to be thrown");
    }
    catch (TaskInterruptedException expected) {
    }

    //Verify that no api keys were purged even though they belong to non-existent users
    assertThat(underTest.getApiKey(PRINCIPAL_A_DOMAIN, principalA).get().getApiKey(), equalTo(apiKeyForPrincipalA));
    assertThat(underTest.getApiKey(PRINCIPAL_B_DOMAIN, principalB).get().getApiKey(), equalTo(apiKeyForPrincipalB));
  }

  @Test
  public void testPurgeOrphanedApiKeys_pauseAndResumesTX() throws UserNotFoundException {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME);
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME);
    char[] apiKeyForPrincipalA = underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA);
    char[] apiKeyForPrincipalB = underTest.createApiKey(PRINCIPAL_B_DOMAIN, principalB);

    // exercise nested DB to make sure we're isolated from any surrounding API-Key transaction
    when(principalsHelper.getUserStatus(principalA)).
        thenAnswer(invocation ->
          inTx(database2.getInstanceProvider()).throwing(Exception.class).call(db -> {
            assertThat(db.getName(), equalTo("nested"));
            return UserStatus.active;
          })
        );
    when(principalsHelper.getUserStatus(principalB)).
        thenAnswer(invocation ->
          inTx(database2.getInstanceProvider()).throwing(Exception.class).call(db -> {
            assertThat(db.getName(), equalTo("nested"));
            throw new UserNotFoundException(principalB.getPrimaryPrincipal().toString());
          })
        );

    underTest.purgeApiKeys();

    // verify that api keys that belong to non-existent users are purged
    assertThat(underTest.getApiKey(PRINCIPAL_A_DOMAIN, principalA).get().getApiKey(), equalTo(apiKeyForPrincipalA));
    assertThat(underTest.getApiKey(PRINCIPAL_B_DOMAIN, principalB), is(Optional.empty()));
  }

  @Test
  public void testCount() {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME);
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME);
    underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA);
    underTest.createApiKey(PRINCIPAL_B_DOMAIN, principalB);

    assertThat(underTest.count(PRINCIPAL_A_DOMAIN), is(1));
  }

  @Test
  public void testBrowse() {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME);
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME);
    underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA);
    underTest.createApiKey(PRINCIPAL_B_DOMAIN, principalB);

    Collection<ApiKey> keys = underTest.browse(PRINCIPAL_A_DOMAIN);
    assertThat(keys, hasSize(1));
  }

  @Test
  public void testBrowseByCreatedDate() throws InterruptedException {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME);
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME);
    PrincipalCollection principalC = makePrincipals(PRINCIPAL_A_NAME);
    underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA);
    OffsetDateTime created = OffsetDateTime.now();
    Thread.sleep(100L);
    underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalB);
    underTest.createApiKey(PRINCIPAL_B_DOMAIN, principalC);

    Collection<ApiKey> keys = underTest.browseByCreatedDate(PRINCIPAL_A_DOMAIN, created);
    assertThat(keys, hasSize(1));
  }

  private PrincipalCollection makePrincipals(final String name) {
    return new SimplePrincipalCollection(name, "foo");
  }
}
