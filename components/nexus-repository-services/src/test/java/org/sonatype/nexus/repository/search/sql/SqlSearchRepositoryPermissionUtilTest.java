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

import java.util.List;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.selector.SelectorConfiguration.EXPRESSION;

public class SqlSearchRepositoryPermissionUtilTest
    extends TestSupport
{

  private static final String SELECTOR_EXPRESSION_1 = "format=\"raw\" and path^=\"/awesome-raw-file.txt\"";

  private static final String SELECTOR_EXPRESSION_2 = "format=\"raw\" and path^=\"/super-raw-file.txt\"";

  private static final String RAW = "raw";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private SelectorConfiguration selectorConfiguration1;

  @Mock
  private SelectorConfiguration selectorConfiguration2;

  @Mock
  private SelectorConfiguration selectorConfiguration3;

  @InjectMocks
  private SqlSearchRepositoryPermissionUtil underTest;

  @Test
  public void shouldReturnUnknownRepositories() {
    Set<String> repositories = ImmutableSet.of("repo1", "repo2", "repo3", "repo4");

    Set<String> unknownRepositories = underTest.browsableAndUnknownRepositories(RAW, repositories);

    assertThat(unknownRepositories, containsInAnyOrder(repositories.toArray(new String[0])));
    verify(securityHelper, never()).allPermitted(any());
  }

  @Test
  public void shouldReturnBrowsableRepositories() {
    Set<String> repositories = ImmutableSet.of("repo1", "repo2", "repo3", "repo4");
    Set<String> expected = ImmutableSet.of("repo2", "repo3");

    mockRepositoryExists(repositories);
    mockRepositoryBrowsePermission(expected);

    Set<String> browsableRepositories = underTest.browsableAndUnknownRepositories(RAW, repositories);

    assertThat(browsableRepositories, containsInAnyOrder(expected.toArray(new String[0])));
  }

  @Test
  public void selectorConfigurationsShouldBeEmptyList() {
    List<SelectorConfiguration> selectorConfigurations = underTest.selectorConfigurations(emptySet(), emptyList());

    assertThat(selectorConfigurations.isEmpty(), is(true));
  }

  @Test
  public void shouldGetDistinctSelectorConfigurations() {
    Set<String> repositories = ImmutableSet.of("repo2", "repo3");
    List<String> formats = singletonList(RAW);
    mockSelectorConfigurations(repositories, formats);

    List<SelectorConfiguration> selectorConfigurations = underTest.selectorConfigurations(repositories, formats);

    assertThat(selectorConfigurations, containsInAnyOrder(selectorConfiguration1, selectorConfiguration2));
  }

  private List<SelectorConfiguration> getSelectorConfigurations() {
    return asList(selectorConfiguration1, selectorConfiguration2, selectorConfiguration3);
  }

  private void mockSelectorConfigurations(final Set<String> repositories, final List<String> formats) {
    when(selectorConfiguration1.getAttributes()).thenReturn(ImmutableMap.of(EXPRESSION, SELECTOR_EXPRESSION_1));
    when(selectorConfiguration2.getAttributes()).thenReturn(ImmutableMap.of(EXPRESSION, SELECTOR_EXPRESSION_2));
    when(selectorConfiguration3.getAttributes()).thenReturn(ImmutableMap.of(EXPRESSION, SELECTOR_EXPRESSION_2));
    when(selectorManager.browseActive(repositories, formats)).thenReturn(getSelectorConfigurations());
  }

  private void mockRepositoryBrowsePermission(final Set<String> repositories) {
    repositories.forEach(
        repository -> when(securityHelper.allPermitted(new RepositoryViewPermission(RAW, repository, BROWSE)))
            .thenReturn(true));
  }

  private void mockRepositoryExists(final Set<String> repositories) {
    repositories.forEach(
        repository -> when(repositoryManager.exists(repository)).thenReturn(true));
  }
}
