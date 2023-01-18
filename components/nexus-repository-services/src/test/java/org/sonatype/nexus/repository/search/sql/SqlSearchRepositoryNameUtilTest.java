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
package org.sonatype.nexus.repository.search.sql;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SqlSearchRepositoryNameUtil;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqlSearchRepositoryNameUtilTest
    extends TestSupport
{
  private static final String HOSTED_REPO_4 = "hosted-repo4";

  private static final String HOSTED_REPO_3 = "hosted-repo3";

  private static final String HOSTED_REPO_2 = "hosted-repo2";

  private static final String HOSTED_REPO_1 = "hosted-repo1";

  private static final String GROUP_REPO = "group-repo";

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository hostedRepo1;

  @Mock
  private Repository hostedRepo2;

  @Mock
  private Repository hostedRepo3;

  @Mock
  private Repository groupRepo;

  @Mock
  private GroupFacet groupFacet;

  @Mock
  private Type hostedType;

  @Mock
  private Type groupType;

  @Mock
  private Format format1;

  @Mock
  private Format format2;

  private SqlSearchRepositoryNameUtil underTest;

  public static final String RAW = "raw";

  @Before
  public void setup() {
    underTest = new SqlSearchRepositoryNameUtil(repositoryManager);
  }

  @Test
  public void shouldBeEmptyCollectionWhenRepositoryIsBlank() {
    assertThat(underTest.getRepositoryNames(null), emptyCollectionOf(String.class));
    assertThat(underTest.getRepositoryNames(EMPTY), emptyCollectionOf(String.class));
  }

  @Test
  public void shouldBeHostedRepositoriesWhenHostedRepositorySpecified() {
    mockHostedRepositories();

    Set<String> repositoryNames = underTest.getRepositoryNames(HOSTED_REPO_2 + " " + HOSTED_REPO_1);

    assertThat(repositoryNames, containsInAnyOrder(HOSTED_REPO_1, HOSTED_REPO_2));
    verify(repositoryManager, never()).browse();
    verifyGroupFacetNotChecked();
  }

  @Test
  public void returnUnknownRepositoriesWhenUnknownRepositorySpecified() {
    Set<String> repositoryNames = underTest.getRepositoryNames(HOSTED_REPO_2 + " " + HOSTED_REPO_1);

    assertThat(repositoryNames, containsInAnyOrder(HOSTED_REPO_1, HOSTED_REPO_2));
    verify(repositoryManager, never()).browse();
    verifyGroupFacetNotChecked();
  }

  @Test
  public void shouldBeLeafMembersWhenGroupRepositorySpecified() {
    mockGroupRepositories();

    Set<String> repositoryNames = underTest.getRepositoryNames(GROUP_REPO + " " + HOSTED_REPO_4);

    assertThat(repositoryNames, containsInAnyOrder(HOSTED_REPO_1, HOSTED_REPO_2, HOSTED_REPO_3, HOSTED_REPO_4));
    verify(repositoryManager, never()).browse();
  }

  @Test
  public void shouldFindMatchingRepositoriesWhenWildcardSpecified() {
    mockRepositoryConfiguration();
    assertThat(underTest.getRepositoryNames("hosted.*" + " " + HOSTED_REPO_3),
        containsInAnyOrder(HOSTED_REPO_1, HOSTED_REPO_2, HOSTED_REPO_3));

    mockRepositoryConfiguration();
    assertThat(underTest.getRepositoryNames("hosted-repo?" + " " + HOSTED_REPO_3),
        containsInAnyOrder(HOSTED_REPO_1, HOSTED_REPO_2, HOSTED_REPO_3));
  }

  @Test
  public void shouldGetRepositoryNamesForFormat() {
    mockRepositoryFormats();

    mockHostedRepositories();
    when(repositoryManager.browse()).thenReturn(asList(hostedRepo1, hostedRepo2, hostedRepo3));
    assertThat(underTest.getFormatRepositoryNames(RAW),
        containsInAnyOrder(HOSTED_REPO_1, HOSTED_REPO_3));

    mockGroupRepositories();
    when(repositoryManager.browse()).thenReturn(singletonList(groupRepo));
    assertThat(underTest.getFormatRepositoryNames(RAW),
        containsInAnyOrder(HOSTED_REPO_1, HOSTED_REPO_2, HOSTED_REPO_3));
  }

  @Test
  public void shouldBeEmptyCollectionWhenFormatIsBlank() {
    assertThat(underTest.getFormatRepositoryNames(null), emptyCollectionOf(String.class));
    assertThat(underTest.getFormatRepositoryNames(EMPTY), emptyCollectionOf(String.class));
  }

  @Test
  public void shouldThrowExceptionWhenLeadingWildcard() {
    expectedException.expect(ValidationErrorsException.class);
    expectedException.expectMessage("Leading wildcards are prohibited");
    underTest.getRepositoryNames("*osted-repo1");
  }

  @Test
  public void shouldThrowExceptionWhenWildcardHasLessThenThreeSymbols() {
    expectedException.expect(ValidationErrorsException.class);
    expectedException.expectMessage("3 characters or more are required with a trailing wildcard (*)");
    underTest.getRepositoryNames("ho*");
  }

  private void mockGroupRepositories() {
    when(repositoryManager.get(GROUP_REPO)).thenReturn(groupRepo);
    when(groupFacet.leafMembers()).thenReturn(ImmutableList.of(hostedRepo1, hostedRepo2, hostedRepo3));
    when(groupRepo.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    when(hostedRepo1.getName()).thenReturn(HOSTED_REPO_1);
    when(hostedRepo2.getName()).thenReturn(HOSTED_REPO_2);
    when(hostedRepo3.getName()).thenReturn(HOSTED_REPO_3);
    when(groupRepo.getName()).thenReturn(GROUP_REPO);
    mockHostedRepositoryType(hostedRepo1, hostedRepo2, hostedRepo3);
    when(groupRepo.getType()).thenReturn(groupType);
    when(groupType.getValue()).thenReturn(GroupType.NAME);
  }

  private void mockHostedRepositories() {
    mockRepositoryManager();
    when(hostedRepo1.optionalFacet(GroupFacet.class)).thenReturn(empty());
    when(hostedRepo2.optionalFacet(GroupFacet.class)).thenReturn(empty());
    when(hostedRepo1.getName()).thenReturn(HOSTED_REPO_1);
    when(hostedRepo2.getName()).thenReturn(HOSTED_REPO_2);
    when(hostedRepo3.getName()).thenReturn(HOSTED_REPO_3);
    mockHostedRepositoryType(hostedRepo1, hostedRepo2, hostedRepo3);
  }

  private void mockHostedRepositoryType(Repository... repositories) {
    Stream.of(repositories)
        .forEach(repository -> when(repository.getType()).thenReturn(hostedType));
    when(hostedType.getValue()).thenReturn(HostedType.NAME);
  }

  private void mockRepositoryManager() {
    when(repositoryManager.get(HOSTED_REPO_1)).thenReturn(hostedRepo1);
    when(repositoryManager.get(HOSTED_REPO_2)).thenReturn(hostedRepo2);
  }

  private void mockRepositoryFormats() {
    when(groupRepo.getFormat()).thenReturn(format2);
    when(hostedRepo1.getFormat()).thenReturn(format2);
    when(hostedRepo2.getFormat()).thenReturn(format1);
    when(hostedRepo3.getFormat()).thenReturn(format2);

    when(format1.getValue()).thenReturn("maven2");
    when(format2.getValue()).thenReturn(RAW);
  }

  private void verifyGroupFacetNotChecked() {
    verify(hostedRepo1, never()).optionalFacet(GroupFacet.class);
    verify(hostedRepo2, never()).optionalFacet(GroupFacet.class);
  }

  private void mockRepositoryConfiguration() {
    when(repositoryManager.browse()).thenReturn(asList(hostedRepo1, hostedRepo2));
    when(hostedRepo1.getName()).thenReturn(HOSTED_REPO_1);
    when(hostedRepo2.getName()).thenReturn(HOSTED_REPO_2);
  }
}
