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
package org.sonatype.nexus.repository.config;

import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests validity of Repository "foo"s validated by {@link UniqueRepositoryNameValidator}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class UniqueRepositoryNameValidatorTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConfigurationStore configurationStore;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

  @InjectMocks
  private UniqueRepositoryNameValidator validator;

  /*
   * Name is valid when the RepositoryManager says it does not exist
   */
  @Test
  public void testIsValid() {
    when(repositoryManager.exists("foo")).thenReturn(true, false);

    assertFalse(validator.isValid("foo", null));
    assertTrue(validator.isValid("foo", null));
  }

  /*
   * Name is invalid when a soft-deleted repository exists whose name matches ignoring case, because
   * repository names are unique case-insensitively and the soft-deleted repository could still be restored.
   */
  @Test
  public void testInvalidWhenSoftDeletedRepositoryExistsIgnoringCase() {
    when(repositoryManager.exists("Foo")).thenReturn(false);
    when(configurationStore.existsDeletedByNameCaseInsensitive("Foo")).thenReturn(true);
    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

    assertFalse(validator.isValid("Foo", context));
  }

  /*
   * Name is valid when neither a live repository nor a soft-deleted repository (ignoring case) exists.
   */
  @Test
  public void testValidWhenNoLiveOrSoftDeletedRepositoryExists() {
    when(repositoryManager.exists("Foo")).thenReturn(false);
    when(configurationStore.existsDeletedByNameCaseInsensitive("Foo")).thenReturn(false);

    assertTrue(validator.isValid("Foo", null));
  }

  /*
   * Null name is considered valid (other constraints such as @NotEmpty handle the null/empty case)
   */
  @Test
  public void testIsValid_nullName() {
    assertTrue(validator.isValid(null, null));
  }
}
