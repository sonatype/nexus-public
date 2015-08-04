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
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.maven.maven1.M1LayoutedM2ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

public class M2LayoutedM1ShadowRepositoryTest
    extends AbstractShadowRepositoryTest
{
  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);

    return new M1TestsuiteEnvironmentBuilder(ss);
  }

  private void addShadowReposes()
      throws ConfigurationException, IOException, ComponentLookupException
  {
    String masterId = "repo1-m1";

    M2LayoutedM1ShadowRepository shadow =
        (M2LayoutedM1ShadowRepository) getContainer().lookup(ShadowRepository.class, "m1-m2-shadow");

    CRepository repoConf = new DefaultCRepository();

    repoConf.setProviderRole(ShadowRepository.class.getName());
    repoConf.setProviderHint("m1-m2-shadow");
    repoConf.setId(masterId + "-m2");
    repoConf.setIndexable(false);

    repoConf.setLocalStorage(new CLocalStorage());
    repoConf.getLocalStorage().setProvider("file");

    Xpp3Dom exRepo = new Xpp3Dom("externalConfiguration");
    repoConf.setExternalConfiguration(exRepo);
    M1LayoutedM2ShadowRepositoryConfiguration exRepoConf = new M1LayoutedM2ShadowRepositoryConfiguration(exRepo);
    exRepoConf.setMasterRepositoryId(masterId);

    shadow.configure(repoConf);

    shadow.synchronizeWithMaster();

    getRepositoryRegistry().addRepository(shadow);

  }

  @Test
  public void testProxyLastRequestedAttribute()
      throws Exception
  {
    addShadowReposes();

    testProxyLastRequestedAttribute(getRepositoryRegistry().getRepositoryWithFacet("repo1-m1-m2",
        ShadowRepository.class), "/activeio/activeio/2.1/activeio-2.1.pom", "/activeio/poms/activeio-2.1.pom");
  }
}