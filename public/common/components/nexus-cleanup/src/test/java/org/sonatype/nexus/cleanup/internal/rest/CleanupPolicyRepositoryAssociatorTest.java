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
package org.sonatype.nexus.cleanup.internal.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.rest.ValidationErrorsException;

import jakarta.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CleanupPolicyRepositoryAssociatorTest
{
  private static final String CLEANUP_ATTRIBUTES_KEY = "cleanup";

  private static final String CLEANUP_NAME_KEY = "policyName";

  @Mock
  private RepositoryManager repositoryManager;

  private CleanupPolicyRepositoryAssociator underTest;

  @Before
  public void setup() {
    underTest = new CleanupPolicyRepositoryAssociator(repositoryManager);
  }

  private Repository repo(final String name, final String formatName, final List<String> policies) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    Format format = mock(Format.class);

    Map<String, Map<String, Object>> attrs = new HashMap<>();
    if (policies != null) {
      Map<String, Object> cleanup = new HashMap<>();
      cleanup.put(CLEANUP_NAME_KEY, new ArrayList<>(policies));
      attrs.put(CLEANUP_ATTRIBUTES_KEY, cleanup);
    }

    when(repository.getName()).thenReturn(name);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn(formatName);
    when(configuration.getAttributes()).thenReturn(attrs);
    return repository;
  }

  @Test
  public void testRepositoryHasPolicyTrue() {
    Repository r = repo("r1", "npm", List.of("policy-a", "policy-b"));
    assertThat(underTest.repositoryHasPolicy(r, "policy-a")).isTrue();
  }

  @Test
  public void testRepositoryHasPolicyFalse() {
    Repository r = repo("r1", "npm", List.of("policy-a"));
    assertThat(underTest.repositoryHasPolicy(r, "policy-x")).isFalse();
  }

  @Test
  public void testRepositoryHasPolicyNoCleanupAttributes() {
    Repository r = repo("r1", "npm", null);
    assertThat(underTest.repositoryHasPolicy(r, "policy-a")).isFalse();
  }

  @Test
  public void testGetRepositoriesForPolicyFiltersByFormat() {
    Repository r1 = repo("r1", "npm", List.of("policy-a"));
    Repository r2 = repo("r2", "npm", List.of("policy-a"));
    Repository r3 = repo("r3", "pypi", List.of("policy-a"));
    Repository r4 = repo("r4", "npm", List.of("policy-other"));
    when(repositoryManager.browse()).thenReturn(List.of(r1, r2, r3, r4));

    Set<String> result = underTest.getRepositoriesForPolicy("policy-a", "npm");

    assertThat(result).containsExactlyInAnyOrder("r1", "r2");
  }

  @Test
  public void testUpdateRepositoriesValidatesMissingRepository() {
    when(repositoryManager.get("missing")).thenReturn(null);

    assertThatThrownBy(() -> underTest.updateRepositoriesForPolicy("p1", "npm", Set.of("missing")))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("missing");
  }

  @Test
  public void testUpdateRepositoriesValidatesFormatMismatch() {
    Repository r1 = repo("r1", "pypi", new ArrayList<>());
    when(repositoryManager.get("r1")).thenReturn(r1);

    assertThatThrownBy(() -> underTest.updateRepositoriesForPolicy("p1", "npm", Set.of("r1")))
        .isInstanceOf(ValidationErrorsException.class)
        .hasMessageContaining("format");
  }

  @Test
  public void testUpdateRepositoriesAttachesNew() throws Exception {
    Repository r1 = repo("r1", "npm", new ArrayList<>());
    when(repositoryManager.get("r1")).thenReturn(r1);
    when(repositoryManager.browse()).thenReturn(List.of(r1));

    underTest.updateRepositoriesForPolicy("p1", "npm", Set.of("r1"));

    verify(repositoryManager, times(1)).update(r1.getConfiguration());
    @SuppressWarnings("unchecked")
    Collection<String> policies =
        (Collection<String>) r1.getConfiguration().getAttributes().get(CLEANUP_ATTRIBUTES_KEY).get(CLEANUP_NAME_KEY);
    assertThat(policies).contains("p1");
  }

  @Test
  public void testUpdateRepositoriesDetachesNoLongerRequested() throws Exception {
    Repository r1 = repo("r1", "npm", List.of("p1"));
    when(repositoryManager.get("r1")).thenReturn(r1);
    when(repositoryManager.browse()).thenReturn(List.of(r1));

    underTest.updateRepositoriesForPolicy("p1", "npm", Set.of());

    verify(repositoryManager, times(1)).update(r1.getConfiguration());
    @SuppressWarnings("unchecked")
    Collection<String> policies =
        (Collection<String>) r1.getConfiguration().getAttributes().get(CLEANUP_ATTRIBUTES_KEY).get(CLEANUP_NAME_KEY);
    assertThat(policies).doesNotContain("p1");
  }

  @Test
  public void testUpdateRepositoriesNullRequestedTreatedAsEmpty() throws Exception {
    when(repositoryManager.browse()).thenReturn(List.of());

    underTest.updateRepositoriesForPolicy("p1", "npm", null);

    verify(repositoryManager, never()).update(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void testDetachAllRemovesFromAllAttachedRepositories() throws Exception {
    Repository r1 = repo("r1", "npm", List.of("p1"));
    Repository r2 = repo("r2", "npm", List.of("p2"));
    Repository r3 = repo("r3", "npm", List.of("p1"));
    when(repositoryManager.browse()).thenReturn(List.of(r1, r2, r3));

    underTest.detachAll("p1");

    verify(repositoryManager, times(1)).update(r1.getConfiguration());
    verify(repositoryManager, never()).update(r2.getConfiguration());
    verify(repositoryManager, times(1)).update(r3.getConfiguration());
  }
}
