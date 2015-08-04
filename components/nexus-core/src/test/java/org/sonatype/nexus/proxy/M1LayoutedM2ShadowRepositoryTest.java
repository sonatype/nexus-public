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
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.maven.maven1.M1LayoutedM2ShadowRepository;
import org.sonatype.nexus.proxy.maven.maven1.M1LayoutedM2ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

public class M1LayoutedM2ShadowRepositoryTest
    extends AbstractShadowRepositoryTest
{
  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);

    return new M2TestsuiteEnvironmentBuilder(ss);
  }

  private void addShadowReposes()
      throws ConfigurationException, IOException, ComponentLookupException
  {
    for (ProxyRepository master : getRepositoryRegistry().getRepositoriesWithFacet(ProxyRepository.class)) {
      M1LayoutedM2ShadowRepository shadow =
          (M1LayoutedM2ShadowRepository) getContainer().lookup(ShadowRepository.class, "m2-m1-shadow");

      CRepository repoConf = new DefaultCRepository();

      repoConf.setProviderRole(ShadowRepository.class.getName());
      repoConf.setProviderHint("m2-m1-shadow");
      repoConf.setId(master.getId() + "-m1");
      repoConf.setIndexable(false);

      repoConf.setLocalStorage(new CLocalStorage());
      repoConf.getLocalStorage().setProvider("file");

      Xpp3Dom exRepo = new Xpp3Dom("externalConfiguration");
      repoConf.setExternalConfiguration(exRepo);
      M1LayoutedM2ShadowRepositoryConfiguration exRepoConf =
          new M1LayoutedM2ShadowRepositoryConfiguration(exRepo);
      exRepoConf.setMasterRepositoryId(master.getId());

      shadow.configure(repoConf);

      shadow.synchronizeWithMaster();

      getRepositoryRegistry().addRepository(shadow);
    }

  }

  @Test
  public void testM1Shadows()
      throws Exception
  {
    // this will add shadows manually for all registered reposes
    // changes in master should propagate to shadows
    addShadowReposes();

    // get some content to masters
    StorageItem item =
        getRootRouter()
            .retrieveItem(
                new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar",
                    false));
    checkForFileAndMatchContents(item);

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2/xstream/xstream/1.2.2/xstream-1.2.2.pom", false));
    checkForFileAndMatchContents(item);

    // we will check stuff on M1 places but,
    // we will get links as responses, since shadow reposes contains links only
    getRootRouter().setFollowLinks(false);
    getApplicationConfiguration().saveConfiguration();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1-m1/activemq/jars/activemq-core-1.2.jar", false));
    assertTrue(StorageLinkItem.class.isAssignableFrom(item.getClass()));

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2-m1/xstream/poms/xstream-1.2.2.pom", false));
    assertTrue(StorageLinkItem.class.isAssignableFrom(item.getClass()));

    // and now we will force the router itself to resolve links
    // and will expect the original contents
    getRootRouter().setFollowLinks(true);
    getApplicationConfiguration().saveConfiguration();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1-m1/activemq/jars/activemq-core-1.2.jar", false));
    // it comes from repo1 even if we requested it from repo1-m1
    assertTrue("repo1".equals(item.getRepositoryId()));
    // and the content is correct
    checkForFileAndMatchContents(item);

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2-m1/xstream/poms/xstream-1.2.2.pom", false));
    // it comes from repo1 even if we requested it from repo1-m1
    assertTrue("repo2".equals(item.getRepositoryId()));
    // and the content is correct
    checkForFileAndMatchContents(item);

  }

  @Test
  public void testM1ShadowSync()
      throws Exception
  {
    StorageItem item =
        getRootRouter()
            .retrieveItem(
                new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar",
                    false));
    checkForFileAndMatchContents(item);

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2/xstream/xstream/1.2.2/xstream-1.2.2.pom", false));
    checkForFileAndMatchContents(item);

    // this will add shadows manually for all registered reposes
    // and sync them
    addShadowReposes();

    // and after sync, we will check stuff on M1 places but,
    // we will get links as responses, since shadow reposes contains links only
    getRootRouter().setFollowLinks(false);
    getApplicationConfiguration().saveConfiguration();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1-m1/activemq/jars/activemq-core-1.2.jar", false));
    assertTrue(StorageLinkItem.class.isAssignableFrom(item.getClass()));

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2-m1/xstream/poms/xstream-1.2.2.pom", false));
    assertTrue(StorageLinkItem.class.isAssignableFrom(item.getClass()));

    // and now we will force the router itself to resolve links
    // and will expect the original contents
    getRootRouter().setFollowLinks(true);
    getApplicationConfiguration().saveConfiguration();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1-m1/activemq/jars/activemq-core-1.2.jar", false));
    // it comes from repo1 even if we requested it from repo1-m1
    assertTrue("repo1".equals(item.getRepositoryId()));
    // and the content is correct
    checkForFileAndMatchContents(item);

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2-m1/xstream/poms/xstream-1.2.2.pom", false));
    // it comes from repo1 even if we requested it from repo1-m1
    assertTrue("repo2".equals(item.getRepositoryId()));
    // and the content is correct
    checkForFileAndMatchContents(item);
  }

  @Test
  public void testProxyLastRequestedAttribute()
      throws Exception
  {
    addShadowReposes();

    testProxyLastRequestedAttribute(getRepositoryRegistry().getRepositoryWithFacet("repo2-m1",
        ShadowRepository.class), "/xstream/poms/xstream-1.2.2.pom", "/xstream/xstream/1.2.2/xstream-1.2.2.pom");
  }
}