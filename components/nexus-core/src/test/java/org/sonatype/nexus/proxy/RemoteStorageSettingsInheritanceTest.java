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
package org.sonatype.nexus.proxy;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.DefaultRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Here we test how RemoteStorageContext changes (http proxy, connection settings) are propagating, and are they
 * propagating correctly from proxy repo to global if set/changed.
 *
 * @author cstamas
 */
public class RemoteStorageSettingsInheritanceTest
    extends NexusAppTestSupport
{

  protected ApplicationConfiguration applicationConfiguration;

  protected ProxyRepository aProxyRepository;

  public void setUp()
      throws Exception
  {
    // loads up config, defaults
    startNx();

    super.setUp();

    applicationConfiguration = lookup(ApplicationConfiguration.class);
    aProxyRepository = lookup(RepositoryRegistry.class).getRepositoryWithFacet("central", ProxyRepository.class);
  }

  @Test
  public void testNEXUS3064Global()
      throws Exception
  {
    int rscChange = aProxyRepository.getRemoteStorageContext().getGeneration();

    // and the problem now
    // change the global proxy
    final DefaultRemoteHttpProxySettings httpProxySettings = new DefaultRemoteHttpProxySettings();
    httpProxySettings.setHostname("192.168.1.1");
    httpProxySettings.setPort(1234);

    RemoteProxySettings proxy = applicationConfiguration.getGlobalRemoteStorageContext().getRemoteProxySettings();
    proxy.setHttpProxySettings(httpProxySettings);

    // TODO: this is the spurious part!!! Make it not needed! Config framework DOES know it changed!
    // If you remove this, test will fail
    applicationConfiguration.getGlobalRemoteStorageContext().setRemoteProxySettings(proxy);
    applicationConfiguration.saveConfiguration();

    assertThat("The change should be detected", aProxyRepository.getRemoteStorageContext().getGeneration(),
        greaterThan(rscChange));
  }
}
