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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Test;

public class DefaultRepositoryConfiguratorTest
    extends NexusAppTestSupport
{
  @Test
  public void testExpireNFCOnUpdate()
      throws Exception
  {
    M2Repository oldRepository = (M2Repository)this.lookup(Repository.class, "maven2");

    CRepository cRepo = new DefaultCRepository();
    cRepo.setId("test-repo");
    cRepo.setLocalStatus(LocalStatus.IN_SERVICE.toString());
    cRepo.setNotFoundCacheTTL(1);
    cRepo.setLocalStorage(new CLocalStorage());
    cRepo.getLocalStorage().setProvider("file");
    cRepo.setProviderRole(Repository.class.getName());
    cRepo.setProviderHint("maven2");

    Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
    cRepo.setExternalConfiguration(ex);
    M2RepositoryConfiguration extConf = new M2RepositoryConfiguration(ex);
    extConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);

    oldRepository.configure(cRepo);

    oldRepository.getNotFoundCache().put("test-path", "test-object");

    // make sure the item is in NFC
    Assert.assertTrue(oldRepository.getNotFoundCache().contains("test-path"));

    // change config
    cRepo.setNotFoundCacheTTL(2);

    oldRepository.configure(cRepo);

    // make sure the item is NOT in NFC
    Assert.assertFalse(oldRepository.getNotFoundCache().contains("test-path"));
  }

  @Test
  public void testExpireNFCOnUpdateWithNFCDisabled()
      throws Exception
  {
    M2Repository oldRepository = (M2Repository)this.lookup(Repository.class, "maven2");

    CRepository cRepo = new DefaultCRepository();
    cRepo.setId("test-repo");
    cRepo.setLocalStatus(LocalStatus.IN_SERVICE.toString());
    cRepo.setNotFoundCacheTTL(1);
    cRepo.setLocalStorage(new CLocalStorage());
    cRepo.getLocalStorage().setProvider("file");
    Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
    cRepo.setExternalConfiguration(ex);
    M2RepositoryConfiguration extConf = new M2RepositoryConfiguration(ex);
    extConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
    cRepo.setProviderRole(Repository.class.getName());
    cRepo.setProviderHint("maven2");

    oldRepository.configure(cRepo);

    oldRepository.getNotFoundCache().put("test-path", "test-object");

    // make sure the item is in NFC
    // (cache is disabled )
    // NOTE: we don't care if it in the cache right now, because the retrieve item does not return it.
    // Assert.assertFalse( oldRepository.getNotFoundCache().contains( "test-path" ) );

    oldRepository.configure(cRepo);

    // make sure the item is NOT in NFC
    Assert.assertFalse(oldRepository.getNotFoundCache().contains("test-path"));
  }

  @Test
  public void testDoNotStoreDefaultLocalStorage()
      throws Exception
  {

    M2Repository repository = (M2Repository) this.lookup(Repository.class, "maven2");

    CRepository cRepo = new DefaultCRepository();
    cRepo.setId("test-repo");
    cRepo.setLocalStatus(LocalStatus.IN_SERVICE.toString());
    cRepo.setNotFoundCacheTTL(1);
    cRepo.setLocalStorage(new CLocalStorage());
    cRepo.getLocalStorage().setProvider("file");
    cRepo.setProviderRole(Repository.class.getName());
    cRepo.setProviderHint("maven2");

    Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
    cRepo.setExternalConfiguration(ex);
    M2RepositoryConfiguration extConf = new M2RepositoryConfiguration(ex);
    extConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);

    repository.configure(cRepo);

    Assert.assertNotNull(repository.getLocalUrl());
    Assert.assertNull(cRepo.getLocalStorage().getUrl());

  }

}
