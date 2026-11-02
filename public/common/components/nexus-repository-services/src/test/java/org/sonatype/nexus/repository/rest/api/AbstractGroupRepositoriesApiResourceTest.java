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
package org.sonatype.nexus.repository.rest.api;

import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.model.GroupAttributes;
import org.sonatype.nexus.repository.rest.api.model.GroupRepositoryApiRequest;
import org.sonatype.nexus.repository.security.GroupMemberAuthorizationChecker;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractGroupRepositoriesApiResourceTest
{
  @Mock
  ConstraintViolationFactory constraintViolationFactory;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  RepositoryPermissionChecker repositoryPermissionChecker;

  AbstractGroupRepositoriesApiResource<GroupRepositoryApiRequest> underTest =
      new AbstractGroupRepositoriesApiResource<>()
      {
      };

  @BeforeEach
  void setup() {
    lenient().when(constraintViolationFactory.createViolation(any(), any())).thenAnswer(i -> {
      ConstraintViolation violation = mock();
      when(violation.getMessage()).thenReturn(i.getArgument(1, String.class));
      return violation;
    });
    lenient().when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(true);

    underTest.setRepositoryManager(repositoryManager);
    underTest.setConstraintViolationFactory(constraintViolationFactory);
    underTest.setGroupMemberAuthorizationChecker(
        new GroupMemberAuthorizationChecker(repositoryManager, repositoryPermissionChecker));
  }

  @Test
  void testValidateRequest() {
    assertDoesNotThrow(
        () -> underTest.validateRequest(new GroupRepositoryApiRequest("test-group", "test", true, null, null)));
    assertDoesNotThrow(() -> underTest
        .validateRequest(new GroupRepositoryApiRequest("test-group", "test", true, null, new GroupAttributes(null))));

    List<String> repositoryNames = List.of("missing", "bad-format", "valid");
    when(repositoryManager.get("test-group")).thenReturn(null);
    when(repositoryManager.get("missing")).thenReturn(null);
    mockRepository("bad-format", "maven2");
    mockRepository("valid", "test");

    ConstraintViolationException e = assertThrows(ConstraintViolationException.class, () -> underTest
        .validateRequest(
            new GroupRepositoryApiRequest("test-group", "test", true, null, new GroupAttributes(repositoryNames))));

    Collection<String> violations = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList();
    assertThat(violations, hasSize(2));
    assertThat(violations, allOf(hasItem(containsString("missing")), hasItem(containsString("bad-format")),
        not(hasItem(containsString("valid")))));
  }

  @Test
  void validateRequest_rejectsCreateWithUnreadableMember() {
    mockRepository("secret-repo", "test");
    when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(false);
    when(repositoryManager.get("test-group")).thenReturn(null);

    ConstraintViolationException e = assertThrows(ConstraintViolationException.class, () -> underTest
        .validateRequest(new GroupRepositoryApiRequest("test-group", "test", true, null,
            new GroupAttributes(List.of("secret-repo")))));

    Collection<String> violations = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList();
    assertThat(violations, hasItem("Not authorized to add one or more of the requested member repositories."));
  }

  @Test
  void validateRequest_allowsCreateWithReadableMember() {
    mockRepository("readable-repo", "test");
    when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(true);
    when(repositoryManager.get("test-group")).thenReturn(null);

    assertDoesNotThrow(() -> underTest.validateRequest(new GroupRepositoryApiRequest("test-group", "test", true, null,
        new GroupAttributes(List.of("readable-repo")))));
  }

  @Test
  void validateRequest_rejectsUpdateAddingUnreadableMember() {
    mockRepository("secret-repo", "test");
    mockRepository("existing-repo", "test");
    when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(false);
    mockGroupRepository("test-group", List.of("existing-repo"));

    ConstraintViolationException e = assertThrows(ConstraintViolationException.class, () -> underTest
        .validateRequest(new GroupRepositoryApiRequest("test-group", "test", true, null,
            new GroupAttributes(List.of("existing-repo", "secret-repo")))));

    Collection<String> violations = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList();
    assertThat(violations, hasItem("Not authorized to add one or more of the requested member repositories."));
  }

  @Test
  void validateRequest_allowsUpdateRetainingUnreadableExistingMember() {
    mockRepository("secret-repo", "test");
    lenient().when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(false);
    mockGroupRepository("test-group", List.of("secret-repo"));

    assertDoesNotThrow(() -> underTest.validateRequest(new GroupRepositoryApiRequest("test-group", "test", true, null,
        new GroupAttributes(List.of("secret-repo")))));
  }

  @Test
  void validateRequest_grandfathersExistingMemberIgnoringCase() {
    // stored member name uses different casing than the request; get() resolves names case-insensitively and
    // repository names may contain uppercase, so the grandfather comparison must ignore case (NEXUS-53747)
    mockRepository("existing-repo", "test");
    lenient().when(repositoryPermissionChecker.userCanReadOrBrowse(any())).thenReturn(false);
    mockGroupRepository("test-group", List.of("Existing-Repo"));

    assertDoesNotThrow(() -> underTest.validateRequest(new GroupRepositoryApiRequest("test-group", "test", true, null,
        new GroupAttributes(List.of("existing-repo")))));
  }

  private Repository mockRepository(final String name, final String formatName) {
    Repository repository = mock();
    Format format = mock();
    when(format.getValue()).thenReturn(formatName);
    when(repository.getFormat()).thenReturn(format);

    when(repositoryManager.get(name)).thenReturn(repository);

    return repository;
  }

  private Repository mockGroupRepository(final String name, final Collection<String> currentMemberNames) {
    Repository repository = mock();
    Configuration configuration = mock();
    NestedAttributesMap groupAttributes = new NestedAttributesMap("group", new java.util.HashMap<>());
    groupAttributes.set("memberNames", currentMemberNames);
    lenient().when(configuration.attributes("group")).thenReturn(groupAttributes);
    lenient().when(repository.getConfiguration()).thenReturn(configuration);

    when(repositoryManager.get(name)).thenReturn(repository);

    return repository;
  }
}
