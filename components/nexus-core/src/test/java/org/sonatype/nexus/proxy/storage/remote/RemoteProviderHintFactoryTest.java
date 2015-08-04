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
package org.sonatype.nexus.proxy.storage.remote;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.proxy.storage.remote.DefaultRemoteProviderHintFactory.DEFAULT_HTTP_PROVIDER_KEY;
import static org.sonatype.nexus.proxy.storage.remote.httpclient.HttpClientRemoteStorage.PROVIDER_STRING;

/**
 * Tests for {@link DefaultRemoteProviderHintFactory}.
 */
public class RemoteProviderHintFactoryTest
    extends TestSupport
{
  private static final String FAKE_VALUE = "Foo-Bar";

  private DefaultRemoteProviderHintFactory underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new DefaultRemoteProviderHintFactory();
  }

  @After
  public void tearDown() throws Exception {
    // clear the property
    System.clearProperty(DEFAULT_HTTP_PROVIDER_KEY);
  }

  @Test
  public void testIt() throws Exception {
    // clear the property
    System.clearProperty(DEFAULT_HTTP_PROVIDER_KEY);

    // nothing set
    assertThat(underTest.getDefaultHttpRoleHint(), is(PROVIDER_STRING));

    System.setProperty(DEFAULT_HTTP_PROVIDER_KEY, FAKE_VALUE);
    assertThat(underTest.getDefaultHttpRoleHint(), is(FAKE_VALUE));
  }
}
