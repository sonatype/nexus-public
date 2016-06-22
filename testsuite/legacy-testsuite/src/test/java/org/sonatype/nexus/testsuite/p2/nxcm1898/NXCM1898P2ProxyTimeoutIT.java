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
package org.sonatype.nexus.testsuite.p2.nxcm1898;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.test.http.RemoteRepositories;
import org.sonatype.nexus.test.http.RemoteRepositories.RemoteRepository;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;
import org.sonatype.sisu.goodies.common.Time;
import org.sonatype.tests.http.server.fluent.Behaviours;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public abstract class NXCM1898P2ProxyTimeoutIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1898P2ProxyTimeoutIT() {
    super("nxcm1898");
    // System.setProperty( "org.eclipse.ecf.provider.filetransfer.retrieve.readTimeout", "30000" );
  }

  @Override
  @Before
  public void startProxy() throws Exception {
    remoteRepositories = RemoteRepositories.builder()
        .repo(RemoteRepository.repo("remote").behave(Behaviours.pause(Time.millis(500))).resourceBase(TestProperties.getString("proxy-repo-target-dir")).build())
        .build();
    remoteRepositories.start();
  }

  @Test
  @Ignore
  protected void test(final int timeout)
      throws IOException, Exception
  {
    final String nexusTestRepoUrl = getNexusTestRepoUrl();

    final File installDir = new File("target/eclipse/nxcm1898");

    // give it a good amount of time
    final GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();
    settings.getGlobalConnectionSettings().setConnectionTimeout(timeout);
    SettingsMessageUtil.save(settings);

    installAndVerifyP2Feature();
  }

}