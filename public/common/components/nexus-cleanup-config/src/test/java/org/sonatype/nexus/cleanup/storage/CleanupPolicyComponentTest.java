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
package org.sonatype.nexus.cleanup.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.cleanup.internal.storage.CleanupPolicyData;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.ADD;
import static org.sonatype.nexus.security.BreadActions.READ;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CleanupPolicyComponentTest
{
  private static final String FORMAT = "maven2";

  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  private StoreLoadParameters parameters;

  private CleanupPolicyComponent underTest;

  @Before
  public void setup() {
    parameters = mock(StoreLoadParameters.class);
    underTest = new CleanupPolicyComponent(cleanupPolicyStorage, repositoryManager, repositoryPermissionChecker);
  }

  @Test
  public void readByFormatReturnsMappedPoliciesWhenFormatPresent() {
    when(parameters.getFilter("format")).thenReturn(FORMAT);
    when(cleanupPolicyStorage.getAllByFormat(FORMAT)).thenReturn(singletonList(policy("policy-1", FORMAT, "delete",
        "some notes")));

    List<CleanupPolicyXO> result = underTest.readByFormat(parameters);

    assertThat(result, hasSize(1));
    CleanupPolicyXO xo = result.get(0);
    assertThat(xo.getName(), is("policy-1"));
    assertThat(xo.getFormat(), is(FORMAT));
    assertThat(xo.getMode(), is("delete"));
    assertThat(xo.getNotes(), is("some notes"));
    assertThat(xo.getCriteria(), is(notNullValue()));
    assertThat(xo.getSortOrder(), is(0));

    verify(repositoryPermissionChecker).ensureUserHasAnyPermissionOrAdminAccess(any(), eq(READ), any());
    verify(repositoryManager).browse();
    verify(cleanupPolicyStorage).getAllByFormat(FORMAT);
  }

  @Test
  public void readByFormatMapsEveryReturnedPolicy() {
    when(parameters.getFilter("format")).thenReturn(FORMAT);
    when(cleanupPolicyStorage.getAllByFormat(FORMAT)).thenReturn(asList(
        policy("policy-1", FORMAT, "delete", "first"),
        policy("policy-2", FORMAT, "delete", "second")));

    List<CleanupPolicyXO> result = underTest.readByFormat(parameters);

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getName(), is("policy-1"));
    assertThat(result.get(1).getName(), is("policy-2"));
  }

  @Test
  public void readByFormatMapsAllFormatsPolicyToDisplayValue() {
    when(parameters.getFilter("format")).thenReturn(CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT);
    when(cleanupPolicyStorage.getAllByFormat(CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT)).thenReturn(
        singletonList(policy("all-policy", CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT, "delete", "notes")));

    List<CleanupPolicyXO> result = underTest.readByFormat(parameters);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getFormat(), is(CleanupPolicyXO.ALL_CLEANUP_POLICY_XO_FORMAT));
  }

  @Test
  public void readByFormatReturnsEmptyListWhenNoFormatFilter() {
    when(parameters.getFilter("format")).thenReturn(null);

    List<CleanupPolicyXO> result = underTest.readByFormat(parameters);

    assertThat(result, is(empty()));

    verify(cleanupPolicyStorage, never()).getAllByFormat(any());
    verify(repositoryPermissionChecker, never()).ensureUserHasAnyPermissionOrAdminAccess(any(), any(), any());
    verify(repositoryManager, never()).browse();
  }

  @Test(expected = NullPointerException.class)
  public void constructorRejectsNullCleanupPolicyStorage() {
    new CleanupPolicyComponent(null, repositoryManager, repositoryPermissionChecker);
  }

  @Test(expected = NullPointerException.class)
  public void constructorRejectsNullRepositoryManager() {
    new CleanupPolicyComponent(cleanupPolicyStorage, null, repositoryPermissionChecker);
  }

  @Test
  public void constructorAllowsNullRepositoryPermissionChecker() {
    CleanupPolicyComponent component =
        new CleanupPolicyComponent(cleanupPolicyStorage, repositoryManager, null);

    assertThat(component, is(notNullValue()));
  }

  @Test
  public void readByFormatReturnsEmptyListButStillChecksPermissionWhenStorageReturnsEmpty() {
    when(parameters.getFilter("format")).thenReturn(FORMAT);
    when(cleanupPolicyStorage.getAllByFormat(FORMAT)).thenReturn(emptyList());

    List<CleanupPolicyXO> result = underTest.readByFormat(parameters);

    assertThat(result, is(empty()));

    // unlike the absent-filter path, the permission check, browse and storage lookup are still exercised
    verify(repositoryPermissionChecker).ensureUserHasAnyPermissionOrAdminAccess(any(), eq(READ), any());
    verify(repositoryManager).browse();
    verify(cleanupPolicyStorage).getAllByFormat(FORMAT);
  }

  @Test
  public void readByFormatChecksAddPermissionForFormatAndPassesBrowseResultWithReadAction() {
    List<Repository> repositories = singletonList(mock(Repository.class));
    when(parameters.getFilter("format")).thenReturn(FORMAT);
    when(repositoryManager.browse()).thenReturn(repositories);
    when(cleanupPolicyStorage.getAllByFormat(FORMAT)).thenReturn(emptyList());

    underTest.readByFormat(parameters);

    ArgumentCaptor<Iterable> permissionsCaptor = ArgumentCaptor.forClass(Iterable.class);
    verify(repositoryPermissionChecker)
        .ensureUserHasAnyPermissionOrAdminAccess(permissionsCaptor.capture(), eq(READ), same(repositories));

    Iterator<?> permissions = permissionsCaptor.getValue().iterator();
    assertThat(permissions.hasNext(), is(true));
    RepositoryAdminPermission permission = (RepositoryAdminPermission) permissions.next();
    assertThat(permissions.hasNext(), is(false));

    // the admin permission targets the requested format with name "*"; note the permission action is ADD even
    // though the access check itself is performed with the READ action
    assertThat(permission.getFormat(), is(FORMAT));
    assertThat(permission.getName(), is("*"));
    assertThat(permission.getActions(), is(singletonList(ADD)));
  }

  @Test
  public void readByFormatPropagatesPermissionFailureAndDoesNotQueryStorage() {
    RuntimeException failure = new RuntimeException("not permitted");
    when(parameters.getFilter("format")).thenReturn(FORMAT);
    doThrow(failure).when(repositoryPermissionChecker)
        .ensureUserHasAnyPermissionOrAdminAccess(any(), eq(READ), any());

    try {
      underTest.readByFormat(parameters);
      fail("Expected the permission failure to propagate");
    }
    catch (RuntimeException e) {
      assertThat(e, is(sameInstance(failure)));
    }

    verify(cleanupPolicyStorage, never()).getAllByFormat(any());
  }

  private static CleanupPolicy policy(
      final String name,
      final String format,
      final String mode,
      final String notes)
  {
    CleanupPolicy policy = new CleanupPolicyData();
    policy.setName(name);
    policy.setFormat(format);
    policy.setMode(mode);
    policy.setNotes(notes);
    Map<String, String> criteria = new HashMap<>();
    policy.setCriteria(criteria);
    return policy;
  }
}
