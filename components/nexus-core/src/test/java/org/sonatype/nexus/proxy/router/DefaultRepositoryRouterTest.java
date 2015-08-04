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
package org.sonatype.nexus.proxy.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.maven1.M1LayoutedM2ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven1.M1Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.security.WebSecurityUtil;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultRepositoryRouterTest
    extends NexusAppTestSupport
{
  private RepositoryRouter router = null;

  private RepositoryRegistry repositoryRegistry = null;

  private SecuritySystem securitySystem = null;

  private ApplicationConfiguration applicationConfiguration;

  private TargetRegistry targetRegistry;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    // loads up config, defaults
    nexusConfiguration().loadConfiguration(false);
    nexusConfiguration().saveConfiguration();

    applicationConfiguration = this.lookup(ApplicationConfiguration.class);
    applicationConfiguration.saveConfiguration();

    this.router = this.lookup(RepositoryRouter.class);
    this.repositoryRegistry = this.lookup(RepositoryRegistry.class);

    this.buildRepository("repo1", true).getCurrentCoreConfiguration();
    this.buildRepository("repo2", true);
    this.buildRepository("repo3-notexposed", false);

    this.buildGroupRepository("group1", true);
    this.buildGroupRepository("group2", true);
    this.buildGroupRepository("group3-notexposed", false);

    this.buildShadowRepository("repo3", true);
    this.buildShadowRepository("repo4", true);
    this.buildShadowRepository("repo5-notexposed", false);

    // copy the security-configuration
    // String resource = this.getClass().getName().replaceAll( "\\.", "\\/" ) + "-security-configuration.xml";
    // URL url = Thread.currentThread().getContextClassLoader().getResource( resource );
    // FileUtils.copyURLToFile( url, new File( getConfHomeDir(), "security-configuration.xml" ) );

    targetRegistry = this.lookup(TargetRegistry.class);

    // shave off defaults
    final Collection<Target> targets = new ArrayList<Target>(targetRegistry.getRepositoryTargets());
    for (Target t : targets) {
      targetRegistry.removeRepositoryTarget(t.getId());
    }

    // create a target
    Target t1 =
        new Target("maven2-all", "All (Maven2)", new Maven2ContentClass(), Arrays.asList(new String[]{".*"}));
    targetRegistry.addRepositoryTarget(t1);

    // flush changes
    applicationConfiguration.saveConfiguration();

    // setup security
    this.securitySystem = this.lookup(SecuritySystem.class);
    this.securitySystem.setRealms(Collections.singletonList("default"));
  }

  @Override
  public void tearDown()
      throws Exception
  {
    try {
      ThreadContext.remove();
    }
    finally {
      super.tearDown();
    }
  }

  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Test
  public void testNXCM4999UseInRepoPathForTargetMatching()
      throws Exception
  {
    // add repo1 to group1
    repositoryRegistry.getRepositoryWithFacet("group1", MavenGroupRepository.class).addMemberRepositoryId(
        "repo1");
    // create a path specific target
    Target t =
        new Target("nxcm4999", "NXCM4999", new Maven2ContentClass(),
            Arrays.asList(new String[]{"(?!/com/.*-sources.jar).*"}));
    targetRegistry.addRepositoryTarget(t);
    // flush changes
    applicationConfiguration.saveConfiguration();

    {
      final Subject subject = loginUser("nxcm4999user");
      try {
        final ResourceStoreRequest request1 =
            new ResourceStoreRequest("/repositories/repo1/com/mycorp/artifact/1.0/artifact-1.0.jar");
        assertThat("User should have access to this resource as it has all the needed perms.",
            router.authorizePath(request1, Action.read));
        final ResourceStoreRequest request2 =
            new ResourceStoreRequest("/repositories/repo1/com/mycorp/artifact/1.0/artifact-1.0-sources.jar");
        assertThat("User should have access to this resource as it has all the needed perms.",
            router.authorizePath(request2, Action.read));
      }
      finally {
        securitySystem.logout(subject);
      }
    }
    {
      final Subject subject = loginUser("nxcm4999userNoSources");
      try {
        final ResourceStoreRequest request1 =
            new ResourceStoreRequest("/repositories/repo1/com/mycorp/artifact/1.0/artifact-1.0.jar");
        assertThat("User should have access to this resource as it has no needed perms.",
            router.authorizePath(request1, Action.read));
        final ResourceStoreRequest request2 =
            new ResourceStoreRequest("/repositories/repo1/com/mycorp/artifact/1.0/artifact-1.0-sources.jar");
        assertThat("User should NOT have access to this resource as it has no needed perms.",
            !router.authorizePath(request2, Action.read));
      }
      finally {
        securitySystem.logout(subject);
      }
    }
  }

  @Test
  public void testRouterWithViewAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1user");

    ResourceStoreRequest request = new ResourceStoreRequest("/repositories/");

    StorageItem item = router.retrieveItem(request);

    StorageCollectionItem collectionItem = (StorageCollectionItem) item;

    // this user only has access to repo1, that is all they should see
    Assert.assertEquals("User should only have access to 'repo1'", 1, collectionItem.list().size());
    Assert.assertEquals("repo1", collectionItem.list().iterator().next().getName());

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRouterWithNoViewAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1userNoView");

    ResourceStoreRequest request = new ResourceStoreRequest("/repositories/");

    StorageItem item = router.retrieveItem(request);

    StorageCollectionItem collectionItem = (StorageCollectionItem) item;

    // this user only has access to repo1, that is all they should see
    Assert.assertEquals("User should not have access to any repos", 0, collectionItem.list().size());

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testFilterOutNonExposedRepositories()
      throws Exception
  {
    Subject subject = this.loginUser("admin");

    ResourceStoreRequest request = new ResourceStoreRequest("/repositories/");

    StorageItem item = router.retrieveItem(request);

    StorageCollectionItem collectionItem = (StorageCollectionItem) item;
    Assert.assertEquals("User should see 8 repositories", 8, collectionItem.list().size()); // we create a new
    // repo for each
    // shadow

    List<String> repoIds = new ArrayList<String>();
    for (StorageItem tmpItem : collectionItem.list()) {
      repoIds.add(tmpItem.getName());
    }

    // now check them all
    Assert.assertTrue(repoIds.contains("repo1"));
    Assert.assertTrue(repoIds.contains("repo2"));
    Assert.assertFalse(repoIds.contains("repo3-notexposed"));

    Assert.assertTrue(repoIds.contains("group1"));
    Assert.assertTrue(repoIds.contains("group2"));
    Assert.assertFalse(repoIds.contains("group3-notexposed"));

    Assert.assertTrue(repoIds.contains("repo3"));
    Assert.assertTrue(repoIds.contains("repo4"));
    Assert.assertFalse(repoIds.contains("repo5-notexposed"));

    Assert.assertTrue(repoIds.contains("repo3-shadow"));
    Assert.assertTrue(repoIds.contains("repo4-shadow"));
    Assert.assertFalse(repoIds.contains("repo5-notexposed-shadow"));

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testFilterOutNonExposedGroups()
      throws Exception
  {
    Subject subject = this.loginUser("admin");

    ResourceStoreRequest request = new ResourceStoreRequest("/shadows/");

    StorageItem item = router.retrieveItem(request);

    StorageCollectionItem collectionItem = (StorageCollectionItem) item;
    Assert.assertEquals("User should see 2 groups", 2, collectionItem.list().size());

    List<String> repoIds = new ArrayList<String>();
    for (StorageItem tmpItem : collectionItem.list()) {
      repoIds.add(tmpItem.getName());
    }

    // now check them all
    Assert.assertTrue(repoIds.contains("repo3-shadow"));
    Assert.assertTrue(repoIds.contains("repo4-shadow"));
    Assert.assertFalse(repoIds.contains("repo5-notexposed-shadow"));

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testFilterOutNonExposedShadows()
      throws Exception
  {
    Subject subject = this.loginUser("admin");

    ResourceStoreRequest request = new ResourceStoreRequest("/groups/");

    StorageItem item = router.retrieveItem(request);

    StorageCollectionItem collectionItem = (StorageCollectionItem) item;
    Assert.assertEquals("User should see 2 groups", 2, collectionItem.list().size());

    List<String> repoIds = new ArrayList<String>();
    for (StorageItem tmpItem : collectionItem.list()) {
      repoIds.add(tmpItem.getName());
    }

    // now check them all
    Assert.assertTrue(repoIds.contains("group1"));
    Assert.assertTrue(repoIds.contains("group2"));
    Assert.assertFalse(repoIds.contains("group3-notexposed"));

    // logout user
    this.securitySystem.logout(subject);
  }

  private M2Repository buildRepository(String repoId, boolean exposed)
      throws Exception
  {
    M2Repository repo = (M2Repository) this.lookup(Repository.class, "maven2");
    CRepository repoConfig = new DefaultCRepository();
    repoConfig.setId(repoId);
    repoConfig.setExposed(exposed);
    repoConfig.setProviderRole(Repository.class.getName());
    repoConfig.setProviderHint("maven2");
    repo.configure(repoConfig);
    this.repositoryRegistry.addRepository(repo);
    this.applicationConfiguration.getConfigurationModel().addRepository(repoConfig);

    return repo;
  }

  private Repository buildGroupRepository(String repoId, boolean exposed)
      throws Exception
  {
    M2GroupRepository repo = (M2GroupRepository) this.lookup(GroupRepository.class, "maven2");
    CRepository repoConfig = new DefaultCRepository();
    repoConfig.setId(repoId);
    repoConfig.setExposed(exposed);
    repoConfig.setProviderRole(GroupRepository.class.getName());
    repoConfig.setProviderHint("maven2");
    repo.configure(repoConfig);
    this.repositoryRegistry.addRepository(repo);
    this.applicationConfiguration.getConfigurationModel().addRepository(repoConfig);

    return repo;
  }

  private Repository buildShadowRepository(String repoId, boolean exposed)
      throws Exception
  {
    M1Repository repo = (M1Repository) this.lookup(Repository.class, "maven1");
    CRepository repoConfig = new DefaultCRepository();
    repoConfig.setId(repoId);
    repoConfig.setExposed(exposed);
    repoConfig.setIndexable(false);
    repoConfig.setProviderRole(Repository.class.getName());
    repoConfig.setProviderHint("maven1");
    repo.configure(repoConfig);
    this.repositoryRegistry.addRepository(repo);
    this.applicationConfiguration.getConfigurationModel().addRepository(repoConfig);

    // now for the shadow
    M2LayoutedM1ShadowRepository shadow =
        (M2LayoutedM1ShadowRepository) this.lookup(ShadowRepository.class, "m1-m2-shadow");
    CRepository shadowConfig = new DefaultCRepository();
    shadowConfig.setId(repoId + "-shadow");
    shadowConfig.setExposed(exposed);
    shadowConfig.setProviderRole(ShadowRepository.class.getName());
    shadowConfig.setProviderHint("m2-m1-shadow");
    shadowConfig.setIndexable(false);

    Xpp3Dom exRepo = new Xpp3Dom("externalConfiguration");
    shadowConfig.setExternalConfiguration(exRepo);
    M1LayoutedM2ShadowRepositoryConfiguration exRepoConf = new M1LayoutedM2ShadowRepositoryConfiguration(exRepo);
    exRepoConf.setMasterRepositoryId(repo.getId());

    shadow.configure(shadowConfig);
    // shadow.
    this.repositoryRegistry.addRepository(shadow);
    this.applicationConfiguration.getConfigurationModel().addRepository(shadowConfig);

    return repo;

  }

  private Subject loginUser(String username)
      throws AuthenticationException
  {
    WebSecurityUtil.setupWebContext(username);
    return this.securitySystem.login(new UsernamePasswordToken(username, ""));
  }

}
