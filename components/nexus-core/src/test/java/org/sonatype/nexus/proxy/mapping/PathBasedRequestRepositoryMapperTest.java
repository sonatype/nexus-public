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
package org.sonatype.nexus.proxy.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractNexusTestEnvironment;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.mapping.RepositoryPathMapping.MappingType;
import org.sonatype.nexus.proxy.maven.AbstractMavenGroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.litmus.testsupport.mock.MockitoRule;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doReturn;

public class PathBasedRequestRepositoryMapperTest
    extends AbstractNexusTestEnvironment
{
  @Rule
  public TestRule mockito = new MockitoRule(this);

  @Mock
  private Repository repoA;

  @Mock
  private Repository repoB;

  @Mock
  private Repository repoC;

  @Mock
  private Repository repoD;

  @Mock
  private Repository repoE;

  @Mock
  private Repository repoF;

  private RepositoryRegistry registry;

  private AbstractMavenGroupRepository groupRepo;

  private DefaultRequestRepositoryMapper requestRepositoryMapper;

  protected RequestRepositoryMapper prepare(Map<String, String[]> inclusions, Map<String, String[]> exclusions,
                                            Map<String, String[]> blockings)
      throws Exception
  {
    requestRepositoryMapper = (DefaultRequestRepositoryMapper) lookup(RequestRepositoryMapper.class);

    // clear it
    for (String id : requestRepositoryMapper.getMappings().keySet()) {
      requestRepositoryMapper.removeMapping(id);
    }
    requestRepositoryMapper.commitChanges();

    registry = lookup(RepositoryRegistry.class);

    Map<String, Repository> repoMap = new LinkedHashMap<String, Repository>();
    repoMap.put("A", repoA);
    repoMap.put("B", repoB);
    repoMap.put("C", repoC);
    repoMap.put("D", repoD);
    repoMap.put("E", repoE);
    repoMap.put("F", repoF);

    ArrayList<String> testgroup = new ArrayList<String>();

    for (Map.Entry<String, Repository> entry : repoMap.entrySet()) {
      doReturn("repo" + entry.getKey()).when(entry.getValue()).getId();
      doReturn("repo" + entry.getKey() + "Name").when(entry.getValue()).getName();
      doReturn(Repository.class.getName()).when(entry.getValue()).getProviderRole();
      doReturn("maven2").when(entry.getValue()).getProviderHint();
      doReturn(true).when(entry.getValue()).isUserManaged();
      doReturn(null).when(entry.getValue()).adaptToFacet(ProxyRepository.class);
      doReturn(new Maven2ContentClass()).when(entry.getValue()).getRepositoryContentClass();
      doReturn(new DefaultRepositoryKind(HostedRepository.class, null)).when(entry.getValue()).getRepositoryKind();
      registry.addRepository(entry.getValue());
      testgroup.add(entry.getValue().getId());

    }

    groupRepo = (M2GroupRepository) getContainer().lookup(GroupRepository.class, "maven2");

    CRepository repoGroupConf = new DefaultCRepository();

    repoGroupConf.setProviderRole(GroupRepository.class.getName());
    repoGroupConf.setProviderHint("maven2");
    repoGroupConf.setId("test");

    repoGroupConf.setLocalStorage(new CLocalStorage());
    repoGroupConf.getLocalStorage().setProvider("file");

    Xpp3Dom exGroupRepo = new Xpp3Dom("externalConfiguration");
    repoGroupConf.setExternalConfiguration(exGroupRepo);
    M2GroupRepositoryConfiguration exGroupRepoConf = new M2GroupRepositoryConfiguration(exGroupRepo);
    exGroupRepoConf.setMemberRepositoryIds(testgroup);
    exGroupRepoConf.setMergeMetadata(true);

    groupRepo.configure(repoGroupConf);

    registry.addRepository(groupRepo);

    if (inclusions != null) {
      for (String key : inclusions.keySet()) {
        RepositoryPathMapping item =
            new RepositoryPathMapping("I" + key, MappingType.INCLUSION, "*", Arrays
                .asList(new String[]{key}), Arrays.asList(inclusions.get(key)));

        requestRepositoryMapper.addMapping(item);
      }
    }

    if (exclusions != null) {
      for (String key : exclusions.keySet()) {
        RepositoryPathMapping item =
            new RepositoryPathMapping("E" + key, MappingType.EXCLUSION, "*", Arrays
                .asList(new String[]{key}), Arrays.asList(exclusions.get(key)));

        requestRepositoryMapper.addMapping(item);
      }
    }

    if (blockings != null) {
      for (String key : blockings.keySet()) {
        RepositoryPathMapping item =
            new RepositoryPathMapping("B" + key, MappingType.BLOCKING, "*", Arrays
                .asList(new String[]{key}), Arrays.asList(blockings.get(key)));

        requestRepositoryMapper.addMapping(item);
      }
    }

    requestRepositoryMapper.commitChanges();

    return requestRepositoryMapper;
  }

  @Test
  public void testInclusionAndExclusion()
      throws Exception
  {
    HashMap<String, String[]> inclusions = new HashMap<String, String[]>();
    inclusions.put("/a/b/.*", new String[]{"repoA", "repoB"});
    inclusions.put("/c/d/.*", new String[]{"repoC", "repoD"});
    inclusions.put("/all/.*", new String[]{"*"});

    HashMap<String, String[]> exclusions = new HashMap<String, String[]>();
    exclusions.put("/e/f/.*", new String[]{"*"});

    RequestRepositoryMapper pm = prepare(inclusions, exclusions, null);

    // using group to guarantee proper ordering
    List<Repository> resolvedRepositories = new ArrayList<Repository>();

    resolvedRepositories.addAll(registry.getRepositoryWithFacet("test", GroupRepository.class)
        .getMemberRepositories());

    List<Repository> mappedRepositories;

    ResourceStoreRequest request;

    request = new ResourceStoreRequest("/a/b/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(2, mappedRepositories.size());
    assertTrue(mappedRepositories.get(0).equals(repoA));
    assertTrue(mappedRepositories.get(1).equals(repoB));

    request = new ResourceStoreRequest("/e/f/should/not/return/any/repo", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(0, mappedRepositories.size());

    request = new ResourceStoreRequest("/all/should/be/servicing", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(6, mappedRepositories.size());

  }

  @Test
  public void testInclusionAndExclusionKeepsGroupOrdering()
      throws Exception
  {
    HashMap<String, String[]> inclusions = new HashMap<String, String[]>();
    inclusions.put("/a/b/.*", new String[]{"repoB", "repoA"});
    inclusions.put("/c/d/.*", new String[]{"repoD", "repoC"});
    inclusions.put("/all/.*", new String[]{"*"});

    HashMap<String, String[]> exclusions = new HashMap<String, String[]>();
    exclusions.put("/e/f/.*", new String[]{"repoE", "repoF"});
    exclusions.put("/e/f/all/.*", new String[]{"*"});

    RequestRepositoryMapper pm = prepare(inclusions, exclusions, null);

    // using group to guarantee proper ordering
    List<Repository> resolvedRepositories = new ArrayList<Repository>();

    resolvedRepositories.addAll(registry.getRepositoryWithFacet("test", GroupRepository.class)
        .getMemberRepositories());

    List<Repository> mappedRepositories;

    ResourceStoreRequest request;

    // /a/b inclusion hit, needed order: A, B
    request = new ResourceStoreRequest("/a/b/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertThat(mappedRepositories, hasSize(2));
    assertThat(mappedRepositories.get(0), equalTo(repoA));
    assertThat(mappedRepositories.get(1), equalTo(repoB));

    // /e/f exclusion hit, needed order: A, B, C, D
    request = new ResourceStoreRequest("/e/f/should/not/return/any/repo", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertThat(mappedRepositories, hasSize(4));
    assertThat(mappedRepositories.get(0), equalTo(repoA));
    assertThat(mappedRepositories.get(1), equalTo(repoB));
    assertThat(mappedRepositories.get(2), equalTo(repoC));
    assertThat(mappedRepositories.get(3), equalTo(repoD));

    request = new ResourceStoreRequest("/all/should/be/servicing", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertThat(mappedRepositories, hasSize(6));

  }

  /**
   * Empty rules are invalid, they are spitted out by validator anyway. This test is bad, and hence is turned off,
   * but
   * it is left here for reference. (added 'dont' at the start)
   */
  @Test
  @Ignore
  public void testEmptyRules()
      throws Exception
  {
    HashMap<String, String[]> inclusions = new HashMap<String, String[]>();
    inclusions.put("/empty/1/.*", new String[]{""});
    inclusions.put("/empty/2/.*", new String[]{null});
    inclusions.put("/empty/5/.*", new String[]{null});

    HashMap<String, String[]> exclusions = new HashMap<String, String[]>();
    exclusions.put("/empty/5/.*", new String[]{""});
    exclusions.put("/empty/6/.*", new String[]{""});
    exclusions.put("/empty/7/.*", new String[]{null});

    RequestRepositoryMapper pm = prepare(inclusions, exclusions, null);

    // using group to guarantee proper ordering
    List<Repository> resolvedRepositories = new ArrayList<Repository>();

    resolvedRepositories.addAll(registry.getRepositoryWithFacet("test", GroupRepository.class)
        .getMemberRepositories());

    List<Repository> mappedRepositories;

    ResourceStoreRequest request;

    // empty inclusion, it should don't be acted upon
    request = new ResourceStoreRequest("/empty/1/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(6, mappedRepositories.size());

    // null inclusion, it should don't be acted upon
    request = new ResourceStoreRequest("/empty/2/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(6, mappedRepositories.size());

    request = new ResourceStoreRequest("/empty/5/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(6, mappedRepositories.size());

    request = new ResourceStoreRequest("/empty/5/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(6, mappedRepositories.size());

    request = new ResourceStoreRequest("/empty/5/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(6, mappedRepositories.size());
  }

  @Test
  public void testBlockingRules()
      throws Exception
  {
    HashMap<String, String[]> blockings = new HashMap<String, String[]>();
    blockings.put("/blocked/1/.*", new String[]{""});

    RequestRepositoryMapper pm = prepare(null, null, blockings);

    // using group to guarantee proper ordering
    List<Repository> resolvedRepositories = new ArrayList<Repository>();

    resolvedRepositories.addAll(registry.getRepositoryWithFacet("test", GroupRepository.class)
        .getMemberRepositories());

    List<Repository> mappedRepositories;

    ResourceStoreRequest request;

    // empty inclusion, it should don't be acted upon
    request = new ResourceStoreRequest("/blocked/1/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(0, mappedRepositories.size());

    // null inclusion, it should don't be acted upon
    request = new ResourceStoreRequest("/dummy/2/something", true);
    mappedRepositories = pm.getMappedRepositories(groupRepo, request, resolvedRepositories);
    assertEquals(6, mappedRepositories.size());
  }

}
