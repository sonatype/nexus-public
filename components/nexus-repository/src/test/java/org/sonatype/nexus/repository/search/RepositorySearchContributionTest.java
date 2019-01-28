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
package org.sonatype.nexus.repository.search;

import java.util.Arrays;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;

public class RepositorySearchContributionTest
  extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  private RepositorySearchContribution underTest;

  @Before
  public void setup() {
    underTest = new RepositorySearchContribution(repositoryManager);
  }

  @Test
  public void testContribute() {
    mockRepo("repo");

    BoolQueryBuilder query = QueryBuilders.boolQuery();

    underTest.contribute(query, REPOSITORY_NAME, "repo");

    //validate we revert to default query when a reponame isn't a group, which is just to match the string as entered
    assertThat(query.toString(), Matchers.containsString("\"query\" : \"repo\""));
    assertThat(query.toString(), Matchers.containsString("\"fields\" : [ \"repository_name\" ]"));
  }

  @Test
  public void testContribute_noRepoMatch() {
    //this isn't necessary, null will be returned by default, but I think it helps point out exactly what is going on
    when(repositoryManager.get("repo")).thenReturn(null);

    BoolQueryBuilder query = QueryBuilders.boolQuery();

    underTest.contribute(query, REPOSITORY_NAME, "repo");

    //validate we revert to default query when a reponame doesn't match, which is just to match the string as entered
    assertThat(query.toString(), Matchers.containsString("\"query\" : \"repo\""));
    assertThat(query.toString(), Matchers.containsString("\"fields\" : [ \"repository_name\" ]"));
  }

  @Test
  public void testContribute_withGroup() {
    Repository repository = mockRepo("repo");
    Repository repository2 = mockRepo("repo2");
    mockRepo("group", repository, repository2);

    BoolQueryBuilder query = QueryBuilders.boolQuery();

    underTest.contribute(query, REPOSITORY_NAME, "group");

    assertThat(query.toString(), Matchers.containsString("\"repository_name\" : [ \"repo\", \"repo2\" ]"));
  }

  private Repository mockRepo(String name, Repository... members) {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(name);
    when(repositoryManager.get(name)).thenReturn(repository);

    if (members != null && members.length > 0) {
      GroupFacet groupFacet = mock(GroupFacet.class);
      when(groupFacet.leafMembers()).thenReturn(Arrays.asList(members));
      when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    }
    else {
      when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    }
    return repository;
  }
}
