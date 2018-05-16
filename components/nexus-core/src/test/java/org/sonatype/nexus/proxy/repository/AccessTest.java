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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.M2TestsuiteEnvironmentBuilder;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.security.PlexusConfiguredRealm;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.security.WebSecurityUtil;
import org.sonatype.nexus.test.http.RemoteRepositories;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Assert;
import org.junit.Test;

public class AccessTest
    extends AbstractProxyTestEnvironment
{
  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  @Override
  public void setUp()
      throws Exception
  {
    // loads up config, defaults
    startNx();

    super.setUp();

    final TargetRegistry targetRegistry = this.lookup(TargetRegistry.class);

    // shave off defaults
    final Collection<Target> targets = new ArrayList<Target>(targetRegistry.getRepositoryTargets());
    for (Target t : targets) {
      targetRegistry.removeRepositoryTarget(t.getId());
    }

    // add target
    Target t1 =
        new Target("maven2-all", "All (Maven2)", new Maven2ContentClass(), Arrays.asList(new String[]{".*"}));

    targetRegistry.addRepositoryTarget(t1);

    getApplicationConfiguration().saveConfiguration();

    // setup security
    // setup security
    final SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    securitySystem.setRealms(Collections.singletonList("default"));
  }

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
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

  protected void customizeModules(final List<Module> modules) {
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        Map<String, String> userPrivileges = new HashMap<>();
        userPrivileges.put("allrepo3", "nexus:target:*:repo3:*,nexus:view:repository:repo3,nexus:view:repository:test");
        userPrivileges.put("allrepo1", "nexus:target:*:repo1:*,nexus:view:repository:repo1,nexus:view:repository:test");
        userPrivileges.put("alltest", "nexus:target:*:test:*,nexus:view:repository:test,nexus:view:repository:repo3,nexus:view:repository:repo1");
        userPrivileges.put("repo3-noview", "nexus:target:*:repo3:*");
        userPrivileges.put("repo1-noview", "nexus:target:*:repo1:*");
        userPrivileges.put("test-noview", "nexus:target:*:test:*");
        PlexusConfiguredRealm realm = new PlexusConfiguredRealm();
        realm.getUserPrivilageMap().putAll(userPrivileges);
        bind(Realm.class).toInstance(realm);
      }
    });
  }

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    if (this.jettyTestsuiteEnvironmentBuilder == null) {
      RemoteRepositories remoteRepositories = RemoteRepositories.builder()
          .repo("repo1", "target/test-classes/repo1")
          .repo("repo2", "target/test-classes/repo2")
          .repo("repo3", "target/test-classes/repo3")
          .build();
      this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(remoteRepositories);
    }
    return this.jettyTestsuiteEnvironmentBuilder;
  }

  @Test
  public void testGroupAccess()
      throws AuthenticationException, Exception
  {

    // user does not have access to the group
    try {
      this.getItem("allrepo3", "test", "/spoof/simple.txt");
      Assert.fail("Expected AccessDeniedException");
    }
    catch (AccessDeniedException e) {
      // expected
    }

    // this user has access to the group
    StorageItem item = this.getItem("alltest", "test", "/spoof/simple.txt");
    // the first repo in the group
    Assert.assertEquals("repo1", item.getRepositoryId());

  }

  @Test
  public void testRepositoryAccess()
      throws AuthenticationException, Exception
  {
    // Not true, currently perms are always transitive
    // group access imples repository access, IF NOT EXPOSED, SO "UN"-EXPOSE IT
    // getRepositoryRegistry().getRepositoryWithFacet( "test", GroupRepository.class ).setExposed( false );

    StorageItem item = this.getItem("alltest", "repo1", "/spoof/simple.txt");
    Assert.assertEquals("repo1", item.getRepositoryId());
  }

  //
  // public void testAuthorizerDirectly()
  // throws Exception
  // {
  // String repoId = "repo1";
  // String path = "/spoof/simple.txt";
  // String username = "alltest";
  //
  // Subject subject = SecurityUtils.getSecurityManager().login( new UsernamePasswordToken( username, "" ) );
  //
  // NexusItemAuthorizer authorizer = this.lookup( NexusItemAuthorizer.class );
  //
  // ResourceStoreRequest request = new ResourceStoreRequest( "/repositories/" + repoId + "/" + path, true );
  // Assert.assertTrue( authorizer.authorizePath( request, Action.read ) );
  //
  // // not sure if we really need to log the user out, we are not using a remember me,
  // // but what can it hurt?
  // SecurityUtils.getSecurityManager().logout( subject.getPrincipals() );
  //
  // }

  private StorageItem getItem(String username, String repositoryId, String path)
      throws AuthenticationException, Exception
  {
    WebSecurityUtil.setupWebContext(username + "-" + repositoryId + "-" + path);

    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);

    Subject subject = securitySystem.login(new UsernamePasswordToken(username, ""));

    Repository repo = this.getRepositoryRegistry().getRepository(repositoryId);

    ResourceStoreRequest request = new ResourceStoreRequest(path, false);

    StorageItem item = repo.retrieveItem(request);

    // not sure if we really need to log the user out, we are not using a remember me,
    // but what can it hurt?
    securitySystem.logout(subject);

    return item;
  }

}
