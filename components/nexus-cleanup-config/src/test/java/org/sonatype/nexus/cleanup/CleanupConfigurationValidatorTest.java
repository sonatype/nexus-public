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
package org.sonatype.nexus.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolation;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CleanupConfigurationValidatorTest
    extends TestSupport
{
  private static final String REPO_NAME = "repoName";

  private static final String CLEANUP_KEY = "cleanup";

  private static final String POLICY_NAME_KEY = "policyName";

  private static final String POLICY_NAME = "policy";

  private static final String FORMAT = "format";

  @Mock
  private ConstraintViolationFactory constraintFactory;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private Configuration configuration;

  @Mock
  private CleanupPolicy cleanupPolicy;

  @Mock
  private ConstraintViolation constraintViolation;

  @Mock
  private Recipe recipe;

  @Mock
  private Map<String, Map<String, Object>> attributes;

  @Mock
  private Map<String, Object> cleanupAttributes;

  private List<Recipe> recipes = new ArrayList<>();

  CleanupConfigurationValidator underTest;

  @Before
  public void setUp() throws Exception {
    when(configuration.getRepositoryName()).thenReturn(REPO_NAME);
    when(configuration.getAttributes()).thenReturn(attributes);
    when(cleanupAttributes.containsKey(POLICY_NAME_KEY)).thenReturn(true);
    when(cleanupAttributes.get(POLICY_NAME_KEY)).thenReturn(ImmutableSet.of(POLICY_NAME));
    when(attributes.containsKey(CLEANUP_KEY)).thenReturn(true);
    when(attributes.get(CLEANUP_KEY)).thenReturn(cleanupAttributes);
    when(cleanupPolicyStorage.get(POLICY_NAME)).thenReturn(cleanupPolicy);
    when(cleanupPolicy.getFormat()).thenReturn(FORMAT);
    when(constraintFactory.createViolation(anyString(), anyString())).thenReturn(constraintViolation);

    recipes.add(recipe);
    when(repositoryManager.getAllSupportedRecipes()).thenReturn(recipes);
    when(configuration.getRecipeName()).thenReturn(FORMAT + "-" + ProxyType.NAME);
    when(recipe.getType()).thenReturn(new ProxyType());
    when(recipe.getFormat()).thenReturn(new Format(FORMAT){});

    underTest = new CleanupConfigurationValidator(constraintFactory, repositoryManager, cleanupPolicyStorage);
  }

  @Test
  public void whenRepositoryNotFoundReturnNull() {
    when(repositoryManager.get(REPO_NAME)).thenReturn(null);

    assertThat(underTest.validate(configuration), is(nullValue()));
    verify(constraintFactory, times(0)).createViolation(anyString(), anyString());
  }

  @Test
  public void whenAttributeNotFoundReturnNull() {
    when(configuration.getAttributes()).thenReturn(null);

    assertThat(underTest.validate(configuration), is(nullValue()));
    verify(constraintFactory, times(0)).createViolation(anyString(), anyString());
  }

  @Test
  public void whenCleanupAttributeNotFoundReturnNull() {
    when(attributes.containsKey(CLEANUP_KEY)).thenReturn(false);

    assertThat(underTest.validate(configuration), is(nullValue()));
    verify(constraintFactory, times(0)).createViolation(anyString(), anyString());
  }

  @Test
  public void whenPolicyNameNotFoundReturnNull() {
    when(cleanupAttributes.containsKey(POLICY_NAME_KEY)).thenReturn(false);

    assertThat(underTest.validate(configuration), is(nullValue()));
    verify(constraintFactory, times(0)).createViolation(anyString(), anyString());
  }

  @Test
  public void whenCleanupPolicyNotFoundReturnNull() {
    when(cleanupPolicyStorage.get(POLICY_NAME)).thenReturn(null);

    assertThat(underTest.validate(configuration), is(nullValue()));
    verify(constraintFactory, times(0)).createViolation(anyString(), anyString());
  }

  @Test
  public void whenValidFormatsReturnNull() {
    assertThat(underTest.validate(configuration), is(nullValue()));
    verify(constraintFactory, times(0)).createViolation(anyString(), anyString());
  }

  @Test
  public void whenInvalidFormatReturnConstraintViolation() {
    when(cleanupPolicy.getFormat()).thenReturn("other");

    assertThat(underTest.validate(configuration), is(constraintViolation));
    verify(constraintFactory).createViolation(anyString(), anyString());
  }
}
