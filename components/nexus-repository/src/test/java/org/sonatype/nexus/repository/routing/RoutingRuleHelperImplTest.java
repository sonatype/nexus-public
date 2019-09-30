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
package org.sonatype.nexus.repository.routing;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleCache;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleHelperImpl;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoutingRuleHelperImplTest
    extends TestSupport
{
  private RoutingRuleHelperImpl underTest;

  private RoutingRuleCache cache;

  @Mock
  private RoutingRuleStore routingRuleStore;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  RoutingRulesConfiguration config;

  @Mock
  RepositoryPermissionChecker repositoryPermissionChecker;

  @Before
  public void setup() {
    when(routingRuleStore.getById("block")).thenReturn(new RoutingRule("block", "some description", RoutingMode.BLOCK,
        Arrays.asList("^/com/sonatype/.*", ".*foobar.*")));
    when(routingRuleStore.getById("allow")).thenReturn(new RoutingRule("allow", "some description", RoutingMode.ALLOW,
        Arrays.asList(".*foobar.*", "^/org/apache/.*")));
    when(repository.getName()).thenReturn("test-repo");
    when(config.isEnabled()).thenReturn(true);
    cache = new RoutingRuleCache(routingRuleStore);
    underTest = new RoutingRuleHelperImpl(cache, repositoryManager, config, repositoryPermissionChecker);
  }

  @Test
  public void testHandle_disabled() throws Exception {
    assertBlocked("block", "/com/sonatype/internal/secrets");

    when(config.isEnabled()).thenReturn(false);
    assertAllowed("block", "/com/sonatype/internal/secrets");
  }

  @Test
  public void testHandle_blockMode() throws Exception {
    assertAllowed("block", "/org/apache/tomcat/catalina");

    assertBlocked("block", "/com/sonatype/internal/secrets");
    assertBlocked("block", "/com/foobar/");
  }

  @Test
  public void testHandle_allowMode() throws Exception {
    assertAllowed("allow", "/org/apache/tomcat/catalina");
    assertAllowed("allow", "/com/foobar/");

    assertBlocked("allow", "/com/sonatype/internal/secrets");
  }

  @Test
  public void testHandle_noRuleAssigned() throws Exception {
    configureRepositoryMock(null);

    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository));

    assertThat(underTest.isAllowed(repository, "/com/sonatype/internal/secrets"), is(true));
    assertThat(underTest.calculateAssignedRepositories().size(), is(0));
  }

  @Test
  public void testHandle_nullRepositoryConfiguration() throws Exception {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getRoutingRuleId()).thenReturn(null);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository));

    assertTrue(underTest.isAllowed(repository, "/some/path"));
    assertEquals(0, underTest.calculateAssignedRepositories().size());
  }

  @Test
  public void testAssignedRepositories_singleRepositoryAssigned() throws Exception {
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository));
    configureRepositoryMock("singleRule");

    Map<EntityId, List<Repository>> assignedRepositoryMap = underTest.calculateAssignedRepositories();
    assertEquals(1, assignedRepositoryMap.size());
    List<Repository> assignedRepositories = assignedRepositoryMap.get(new DetachedEntityId("singleRule"));
    assertEquals(ImmutableList.of(repository), assignedRepositories);
  }

  @Test
  public void testAssignedRepositories_multipleRulesAndRepositories() throws Exception {
    Repository repository2 = mock(Repository.class);
    Repository repository3 = mock(Repository.class);

    when(repository2.getName()).thenReturn("test-repo-2");
    when(repository3.getName()).thenReturn("test-repo-3");
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository, repository2, repository3));

    configureRepositoryMock(repository,"rule-1");
    configureRepositoryMock(repository2,"rule-2");
    configureRepositoryMock(repository3,"rule-2");

    Map<EntityId, List<Repository>> assignedRepositoryMap = underTest.calculateAssignedRepositories();
    assertEquals(2, assignedRepositoryMap.size());
    assertEquals(ImmutableList.of(repository), assignedRepositoryMap.get(new DetachedEntityId("rule-1")));
    assertEquals(ImmutableList.of(repository2, repository3), assignedRepositoryMap.get(new DetachedEntityId("rule-2")));
  }

  private void assertBlocked(final String ruleId, final String path) throws Exception {
    configureRepositoryMock(ruleId);
    assertFalse(underTest.isAllowed(repository, path));
  }

  private void assertAllowed(final String ruleId, final String path) throws Exception {
    configureRepositoryMock(ruleId);
    assertTrue(underTest.isAllowed(repository, path));
  }

  private void configureRepositoryMock(final String repositoryRuleId) {
    configureRepositoryMock(repository, repositoryRuleId);
  }

  private void configureRepositoryMock(final Repository repo, final String repositoryRuleId) {
    Configuration configuration = mock(Configuration.class);
    when(repo.getConfiguration()).thenReturn(configuration);

    if (repositoryRuleId != null) {
      when(configuration.getRoutingRuleId()).thenReturn(new DetachedEntityId(repositoryRuleId));
    }
  }
}
