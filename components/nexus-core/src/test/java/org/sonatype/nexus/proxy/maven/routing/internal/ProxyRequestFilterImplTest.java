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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.util.Arrays;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ProxyRequestFilterImplTest
    extends TestSupport
{
  @Mock
  private EventBus eventBus;

  @Mock
  private SystemStatus systemStatus;

  @Mock
  private ApplicationStatusSource applicationStatusSource;

  @Mock
  private Manager wlManager;

  @Mock
  private MavenProxyRepository mavenProxyRepository;

  @Before
  public void prepare() {
    Mockito.when(systemStatus.isNexusStarted()).thenReturn(true);
    Mockito.when(applicationStatusSource.getSystemStatus()).thenReturn(systemStatus);
    Mockito.when(mavenProxyRepository.getId()).thenReturn("central");
    Mockito.when(mavenProxyRepository.getName()).thenReturn("Central Repository");
  }

  protected void doTestAllowed(final ProxyRequestFilterImpl filter, final String path,
                               final boolean shouldBeAllowed)
  {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest(path);
    assertThat(String.format("%s path is expected to return %s", path, shouldBeAllowed),
        filter.allowed(mavenProxyRepository, resourceStoreRequest), is(shouldBeAllowed));
  }

  @Test
  public void smoke() {
    // no WL exists at all, we must pass all
    final ProxyRequestFilterImpl filter =
        new ProxyRequestFilterImpl(eventBus, applicationStatusSource, wlManager);

    doTestAllowed(filter, "/some/path/and/file/at/the/end.txt", true);
    doTestAllowed(filter, "/foo/bar", true);
    doTestAllowed(filter, "/.meta/prefix.txt", true);
  }

  @Test
  public void withWl() {
    final PrefixSource entrySource = new ArrayListPrefixSource(Arrays.asList("/org/apache", "/org/sonatype"));
    Mockito.when(wlManager.getPrefixSourceFor(Mockito.any(MavenProxyRepository.class))).thenReturn(
        entrySource);

    // WL will be built, not every request should be allowed
    final ProxyRequestFilterImpl filter =
        new ProxyRequestFilterImpl(eventBus, applicationStatusSource, wlManager);

    // ping (this would happen on event)
    filter.buildPathMatcherFor(mavenProxyRepository);

    // +1
    doTestAllowed(filter, "/org/apache/maven/foo/1.0/foo-1.0.jar", true);
    // +1
    doTestAllowed(filter, "/org/sonatype/maven/foo/1.0/foo-1.0.jar", true);
    // -1 com
    doTestAllowed(filter, "/com/sonatype/maven/foo/1.0/foo-1.0.jar", false);
    // -1 not in WL
    doTestAllowed(filter, "/.meta/prefix.txt", false); // this file is handled in AbstractMavenRepository, using
    // UID attributes to test for IsHidden attribute
  }
}
