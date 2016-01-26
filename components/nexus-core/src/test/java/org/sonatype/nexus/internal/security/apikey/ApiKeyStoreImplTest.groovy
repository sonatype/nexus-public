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

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl
import org.sonatype.nexus.crypto.internal.RandomBytesGeneratorImpl
import org.sonatype.nexus.orient.DatabaseInstanceRule
import org.sonatype.nexus.security.UserPrincipalsHelper

import com.google.common.collect.Maps
import com.google.inject.util.Providers
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.subject.SimplePrincipalCollection
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.nullValue
import static org.mockito.Mockito.mock

/**
 * Tests {@link ApiKeyStoreImpl}
 */
class ApiKeyStoreImplTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = new DatabaseInstanceRule('test')

  private ApiKeyStoreImpl underTest

  @Before
  void setup() {
    underTest = new ApiKeyStoreImpl(
        Providers.of(database.instance),
        new ApiKeyEntityAdapter(),
        mock(UserPrincipalsHelper.class),
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
  }

  @Test
  void 'Can create and read an API key'() {
    PrincipalCollection p = makePrincipals("name")
    char[] key = underTest.createApiKey('foo', p)
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

  private PrincipalCollection makePrincipals(String name) {
    return new SimplePrincipalCollection(name, "foo")
  }
}

