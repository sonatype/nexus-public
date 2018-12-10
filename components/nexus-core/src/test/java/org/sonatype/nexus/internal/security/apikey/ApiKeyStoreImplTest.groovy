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
package org.sonatype.nexus.internal.security.apikey

import java.util.concurrent.atomic.AtomicBoolean

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl
import org.sonatype.nexus.crypto.internal.RandomBytesGeneratorImpl
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.orient.transaction.OrientFunction
import org.sonatype.nexus.scheduling.CancelableHelper
import org.sonatype.nexus.scheduling.TaskInterruptedException
import org.sonatype.nexus.security.UserPrincipalsHelper
import org.sonatype.nexus.security.user.UserNotFoundException
import org.sonatype.nexus.security.user.UserStatus

import com.google.common.collect.Maps
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.subject.SimplePrincipalCollection
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.nullValue
import static org.junit.Assert.fail
import static org.mockito.Mockito.when
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx

/**
 * Tests {@link ApiKeyStoreImpl}
 */
class ApiKeyStoreImplTest
    extends TestSupport
{
  private static final String PRINCIPAL_A_NAME = 'name-a'

  private static final String PRINCIPAL_A_DOMAIN = 'foo'

  private static final String PRINCIPAL_B_NAME = 'name-b'

  private static final String PRINCIPAL_B_DOMAIN = 'bar'

  private static final boolean CANCELLED = true

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  @Rule
  public DatabaseInstanceRule database2 = DatabaseInstanceRule.inMemory('nested')

  @Mock
  private UserPrincipalsHelper principalsHelper

  private AtomicBoolean cancelled = new AtomicBoolean(!CANCELLED)

  private ApiKeyStoreImpl underTest

  @Before
  void setup() {
    CancelableHelper.set(cancelled)
    underTest = new ApiKeyStoreImpl(
        database.instanceProvider,
        new ApiKeyEntityAdapter(ClassLoader.getSystemClassLoader()),
        principalsHelper,
        Maps.newHashMap(),
        new DefaultApiKeyFactory(new RandomBytesGeneratorImpl(new CryptoHelperImpl()))
    )
    underTest.start()
  }

  @After
  void tearDown() {
    if (underTest) {
      underTest.stop()
      underTest = null
    }
    CancelableHelper.remove()
  }

  @Test
  void 'Can create and read an API key'() {
    PrincipalCollection p = makePrincipals("name")
    char[] key = underTest.createApiKey('foo', p)
    char[] fetchedKey = underTest.getApiKey('foo', p)

    assertThat(fetchedKey, equalTo(key))
  }

  @Test
  void 'Can persist and read an API key with provided value'() {
    PrincipalCollection p = makePrincipals("name")
    char[] key = ['a', 'b', 'c', 'd'] as char[]
    underTest.persistApiKey('foo', p, key)
    char[] fetchedKey = underTest.getApiKey('foo', p)

    assertThat(fetchedKey, equalTo(key))
  }

  @Test
  void 'Cannot read cross-domain API key'() {
    PrincipalCollection p = makePrincipals("name")
    underTest.createApiKey('foo', p)
    char[] fetchedKey = underTest.getApiKey('bar', p)
    assertThat(fetchedKey, nullValue())
  }

  @Test
  void 'Can create and delete an API Key'() {
    PrincipalCollection p = makePrincipals("name")

    underTest.createApiKey('foo', p)
    underTest.deleteApiKey('foo', p)

    char[] key = underTest.getApiKey('foo', p)
    assertThat(key, equalTo(null))
  }

  @Test
  void 'Find by principal name'() {
    char[] alphakey = underTest.createApiKey('foo', makePrincipals("alpha"))
    underTest.createApiKey('foo', makePrincipals("beta"))
    underTest.createApiKey('foo', makePrincipals("gamma"))

    char[] key = underTest.getApiKey('foo', makePrincipals("alpha"))

    assertThat(key, equalTo(alphakey))
  }

  @Test
  void 'Find by api key'() {
    char[] key = underTest.createApiKey('foo', makePrincipals("alpha"))
    underTest.createApiKey('foo', makePrincipals("beta"))
    underTest.createApiKey('foo', makePrincipals("gamma"))

    PrincipalCollection principals = underTest.getPrincipals('foo', key)

    assertThat(principals.primaryPrincipal, equalTo("alpha"))
  }

  @Test
  void 'Can delete all API Keys'() {
    PrincipalCollection principalA = makePrincipals("name-a")
    PrincipalCollection principalB = makePrincipals("name-b")

    underTest.createApiKey('foo', principalA)
    underTest.createApiKey('bar', principalB)
    underTest.deleteApiKeys()

    assertThat(underTest.getApiKey('foo', principalA), equalTo(null))
    assertThat(underTest.getApiKey('bar', principalB), equalTo(null))
  }

  @Test
  void 'Get api key if already exists'() {
    PrincipalCollection principalA = makePrincipals("name-a")

    def key = underTest.createApiKey('foo', principalA)
    def key2 = underTest.createApiKey('foo', principalA)

    assertThat(key2, equalTo(key))
  }

  @Test
  void 'Can purge orphaned API keys'() {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME)
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME)
    char[] apiKeyForPrincipalA = underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA)
    underTest.createApiKey(PRINCIPAL_B_NAME, principalB)
    when(principalsHelper.getUserStatus(principalA)).thenReturn(UserStatus.disabled)
    when(principalsHelper.getUserStatus(principalB)).
        thenThrow(new UserNotFoundException(principalB.getPrimaryPrincipal()))

    underTest.purgeApiKeys()

    //Verify that api keys that belong to non-existent users are purged
    assertThat(underTest.getApiKey(PRINCIPAL_A_DOMAIN, principalA), equalTo(apiKeyForPrincipalA))
    assertThat(underTest.getApiKey(PRINCIPAL_B_DOMAIN, principalB), nullValue())
  }

  @Test
  void 'Purge orphaned API keys is cancelable'() {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME)
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME)
    char[] apiKeyForPrincipalA = underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA)
    char[] apiKeyForPrincipalB = underTest.createApiKey(PRINCIPAL_B_DOMAIN, principalB)
    when(principalsHelper.getUserStatus(principalA)).
        thenThrow(new UserNotFoundException(principalA.getPrimaryPrincipal()))
    when(principalsHelper.getUserStatus(principalB)).
        thenThrow(new UserNotFoundException(principalB.getPrimaryPrincipal()))
    cancelled.set(CANCELLED)

    try {
      underTest.purgeApiKeys()
      fail("Expected exception to be thrown")
    }
    catch (TaskInterruptedException expected) {
    }

    //Verify that no api keys were purged even though they belong to non-existent users
    assertThat(underTest.getApiKey(PRINCIPAL_A_DOMAIN, principalA), equalTo(apiKeyForPrincipalA))
    assertThat(underTest.getApiKey(PRINCIPAL_B_DOMAIN, principalB), equalTo(apiKeyForPrincipalB))
  }

  @Test
  void 'Purge orphaned API keys pauses/resumes TX'() {
    PrincipalCollection principalA = makePrincipals(PRINCIPAL_A_NAME)
    PrincipalCollection principalB = makePrincipals(PRINCIPAL_B_NAME)
    char[] apiKeyForPrincipalA = underTest.createApiKey(PRINCIPAL_A_DOMAIN, principalA)
    char[] apiKeyForPrincipalB = underTest.createApiKey(PRINCIPAL_B_DOMAIN, principalB)

    // exercise nested DB to make sure we're isolated from any surrounding API-Key transaction
    when(principalsHelper.getUserStatus(principalA)).
        thenAnswer({ invocation ->
          inTx(database2.instanceProvider).throwing(Exception.class).call((OrientFunction) { db ->
            assertThat(db.name, equalTo('nested'))
            UserStatus.active
          })
        })
    when(principalsHelper.getUserStatus(principalB)).
        thenAnswer({ invocation ->
          inTx(database2.instanceProvider).throwing(Exception.class).call((OrientFunction) { db ->
            assertThat(db.name, equalTo('nested'))
            throw new UserNotFoundException(principalB.getPrimaryPrincipal())
          })
        })

    underTest.purgeApiKeys()

    // verify that api keys that belong to non-existent users are purged
    assertThat(underTest.getApiKey(PRINCIPAL_A_DOMAIN, principalA), equalTo(apiKeyForPrincipalA))
    assertThat(underTest.getApiKey(PRINCIPAL_B_DOMAIN, principalB), nullValue())
  }

  private PrincipalCollection makePrincipals(String name) {
    return new SimplePrincipalCollection(name, "foo")
  }
}
