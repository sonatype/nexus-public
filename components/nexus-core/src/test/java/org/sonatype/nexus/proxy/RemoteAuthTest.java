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

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;

import com.google.common.base.Throwables;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Test;

public class RemoteAuthTest
    extends AbstractProxyTestEnvironment
{

  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(ss)
    {
      @Override
      public void buildEnvironment(AbstractProxyTestEnvironment env)
          throws ConfigurationException, IOException, ComponentLookupException
      {
        super.buildEnvironment(env);

        // setting up auths before test starts: reason is that repo with wrong auth
        // settings will become auto blocked (and that's okay), but since NEXUS-5472 reposes
        // will "ping" remote immediately they were created, and in this test, proxy2 and proxy3
        // would become auto blocked. Since this test is just a "smoke test" for supported
        // auth mechanisms for proxy reposes, the fact that we set auth here and
        // not where originally was (just before the retrieve invocation on given test
        // does not change the test meaning.

        try {
          // remote target of repo2 is protected with HTTP BASIC
          UsernamePasswordRemoteAuthenticationSettings settings2 =
              new UsernamePasswordRemoteAuthenticationSettings("cstamas", "cstamas123");
          env.getRepositoryRegistry().getRepositoryWithFacet("repo2", ProxyRepository.class).getRemoteStorageContext()
              .setRemoteAuthenticationSettings(
                  settings2);
          // remote target of repo3 is protected with HTTP DIGEST
          UsernamePasswordRemoteAuthenticationSettings settings3 =
              new UsernamePasswordRemoteAuthenticationSettings("brian", "brian123");
          env.getRepositoryRegistry().getRepositoryWithFacet("repo3", ProxyRepository.class).getRemoteStorageContext()
              .setRemoteAuthenticationSettings(
                  settings3);
        }
        catch (NoSuchRepositoryException e) {
          Throwables.propagate(e);
        }
      }
    };

    return jettyTestsuiteEnvironmentBuilder;
  }

  @Test
  public void testHttpAuths()
      throws Exception
  {
    // remote target of repo1 is not protected
    StorageItem item;

    item =
        getRepositoryRegistry().getRepository("repo1").retrieveItem(
            new ResourceStoreRequest("/repo1.txt", false));
    checkForFileAndMatchContents(item);

    item =
        getRepositoryRegistry().getRepository("repo2").retrieveItem(
            new ResourceStoreRequest("/repo2.txt", false));
    checkForFileAndMatchContents(item);

    item =
        getRepositoryRegistry().getRepository("repo3").retrieveItem(
            new ResourceStoreRequest("/repo3.txt", false));
    checkForFileAndMatchContents(item);
  }
}
