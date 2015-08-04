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
package org.sonatype.nexus.proxy.storage.remote.http;

import java.util.Arrays;
import java.util.Collections;

import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Tests for {@link QueryStringBuilder}.
 */
public class QueryStringBuilderTest
    extends TestSupport
{
  @Mock
  private ProxyRepository proxyRepository;

  @Test
  public void testNoQueryString() {
    QueryStringBuilder subject = new QueryStringBuilder(Collections.<QueryStringContributor>emptyList());
    Assert.assertNull(subject.getQueryString(getRemoteStorageContext(null), proxyRepository));
  }

  @Test
  public void testConfiguredStringOnly() {
    QueryStringBuilder subject = new QueryStringBuilder(Collections.<QueryStringContributor>emptyList());

    String configuredString = "configuredString";

    Assert.assertEquals(configuredString,
        subject.getQueryString(getRemoteStorageContext(configuredString), proxyRepository));
  }

  @Test
  public void testOneContributor() {
    final String contributedString = "contributedString";
    QueryStringContributor contributor = new QueryStringContributor()
    {
      @Override
      public String getQueryString(RemoteStorageContext ctx, ProxyRepository repository) {
        return contributedString;
      }
    };
    QueryStringBuilder subject = new QueryStringBuilder(Collections.singletonList(contributor));
    Assert.assertEquals(contributedString, subject.getQueryString(getRemoteStorageContext(null), proxyRepository));
  }

  @Test
  public void testTwoContributor() {
    QueryStringContributor contributor1 = new QueryStringContributor()
    {
      @Override
      public String getQueryString(RemoteStorageContext ctx, ProxyRepository repository) {
        return "contributedString1";
      }
    };
    QueryStringContributor contributor2 = new QueryStringContributor()
    {
      @Override
      public String getQueryString(RemoteStorageContext ctx, ProxyRepository repository) {
        return "contributedString2";
      }
    };

    QueryStringBuilder subject = new QueryStringBuilder(Arrays.asList(contributor1, contributor2));
    Assert.assertEquals("contributedString1&contributedString2",
        subject.getQueryString(getRemoteStorageContext(null), proxyRepository));
  }

  @Test
  public void testConfiguredStringAndOneContributor() {
    QueryStringContributor contributor = new QueryStringContributor()
    {
      @Override
      public String getQueryString(RemoteStorageContext ctx, ProxyRepository repository) {
        return "contributedString";
      }
    };
    QueryStringBuilder subject = new QueryStringBuilder(Collections.singletonList(contributor));
    Assert.assertEquals("configuredString&contributedString",
        subject.getQueryString(getRemoteStorageContext("configuredString"), proxyRepository));
  }

  private DefaultRemoteStorageContext getRemoteStorageContext(@Nullable String configuredString) {
    DefaultRemoteConnectionSettings settings = new DefaultRemoteConnectionSettings();
    settings.setQueryString(configuredString);
    DefaultRemoteStorageContext ctx = new DefaultRemoteStorageContext(null);
    ctx.setRemoteConnectionSettings(settings);
    return ctx;
  }
}
