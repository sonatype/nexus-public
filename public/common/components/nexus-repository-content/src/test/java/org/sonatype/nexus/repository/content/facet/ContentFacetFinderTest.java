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
package org.sonatype.nexus.repository.content.facet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.RepositoryStoppedEvent;
import org.sonatype.nexus.repository.group.GroupFacet;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentFacetFinderTest
{
  private static final String FORMAT_VALUE = "maven2";

  private static final int CONTENT_REPOSITORY_ID = 42;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  @Mock
  private ContentFacet contentFacet;

  private ContentFacetFinder underTest;

  @Before
  public void setUp() {
    underTest = new ContentFacetFinder();

    when(format.getValue()).thenReturn(FORMAT_VALUE);
    when(repository.getFormat()).thenReturn(format);
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(contentFacet.contentRepositoryId()).thenReturn(CONTENT_REPOSITORY_ID);
  }

  @Test
  public void testFindRepositoryAfterStart() {
    RepositoryStartedEvent startedEvent = new RepositoryStartedEvent(repository);

    underTest.on(startedEvent);

    Optional<Repository> result = underTest.findRepository(FORMAT_VALUE, CONTENT_REPOSITORY_ID);
    assertTrue(result.isPresent());
    assertThat(result.get(), is(repository));
  }

  @Test
  public void testFindRepositoryReturnsEmptyWhenNotStarted() {
    Optional<Repository> result = underTest.findRepository(FORMAT_VALUE, CONTENT_REPOSITORY_ID);
    assertFalse(result.isPresent());
  }

  @Test
  public void testFindRepositoryRemovesOnStop() {
    RepositoryStartedEvent startedEvent = new RepositoryStartedEvent(repository);
    underTest.on(startedEvent);

    // verify it was registered
    assertTrue(underTest.findRepository(FORMAT_VALUE, CONTENT_REPOSITORY_ID).isPresent());

    RepositoryStoppedEvent stoppedEvent = new RepositoryStoppedEvent(repository);
    underTest.on(stoppedEvent);

    Optional<Repository> result = underTest.findRepository(FORMAT_VALUE, CONTENT_REPOSITORY_ID);
    assertFalse(result.isPresent());
  }

  @Test
  public void testFindContentFacets() {
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));

    Stream<ContentFacet> result = ContentFacetFinder.findContentFacets(repository);
    List<ContentFacet> facets = result.collect(Collectors.toList());

    assertThat(facets, contains(contentFacet));
  }

  @Test
  public void testFindContentFacetsForGroupRepository() {
    Repository groupRepository = mock(Repository.class);
    GroupFacet groupFacet = mock(GroupFacet.class);

    // group repository has no ContentFacet, but has a GroupFacet
    when(groupRepository.optionalFacet(ContentFacet.class)).thenReturn(Optional.empty());
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));

    // group members include a repository with a ContentFacet
    Repository memberRepository = mock(Repository.class);
    ContentFacet memberContentFacet = mock(ContentFacet.class);
    when(memberRepository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(memberContentFacet));
    when(groupFacet.members()).thenReturn(ImmutableList.of(memberRepository));

    Stream<ContentFacet> result = ContentFacetFinder.findContentFacets(groupRepository);
    List<ContentFacet> facets = result.collect(Collectors.toList());

    assertThat(facets, contains(memberContentFacet));
  }

  @Test
  public void testFindContentFacetsReturnsEmptyForRepositoryWithNoFacets() {
    Repository emptyRepository = mock(Repository.class);
    when(emptyRepository.optionalFacet(ContentFacet.class)).thenReturn(Optional.empty());
    when(emptyRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    Stream<ContentFacet> result = ContentFacetFinder.findContentFacets(emptyRepository);
    List<ContentFacet> facets = result.collect(Collectors.toList());

    assertThat(facets, is(empty()));
  }
}
