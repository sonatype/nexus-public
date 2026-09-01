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
package org.sonatype.nexus.cleanup.storage.event;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.CLEANUP_ATTRIBUTES_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.CLEANUP_NAME_KEY;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CleanupPolicyEventHandlerTest
{
  private static final String POLICY_NAME = "policy-to-delete";

  private static final String OTHER_POLICY = "other-policy";

  private static final String REPOSITORY_NAME = "test-repo";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private CleanupPolicyDeletedEvent event;

  @Mock
  private CleanupPolicy cleanupPolicy;

  private CleanupPolicyEventHandler underTest;

  @Before
  public void setup() {
    underTest = new CleanupPolicyEventHandler(repositoryManager);

    when(event.getCleanupPolicy()).thenReturn(cleanupPolicy);
    when(cleanupPolicy.getName()).thenReturn(POLICY_NAME);
  }

  @Test(expected = NullPointerException.class)
  public void constructorRejectsNullRepositoryManager() {
    new CleanupPolicyEventHandler(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullEventThrowsNullPointerException() {
    underTest.on(null);
  }

  @Test
  public void doesNotProcessNonLocalEvent() throws Exception {
    when(event.isLocal()).thenReturn(false);

    underTest.on(event);

    verify(repositoryManager, never()).browseForCleanupPolicy(any());
    verify(repositoryManager, never()).update(any());
  }

  @Test
  public void removesPolicyNameWhenOthersRemain() throws Exception {
    Map<String, Map<String, Object>> attributes = attributesWithPolicies(POLICY_NAME, OTHER_POLICY);
    Configuration configuration = mockConfiguration(attributes);
    stubBrowse(configuration);
    when(event.isLocal()).thenReturn(true);

    underTest.on(event);

    verify(repositoryManager).browseForCleanupPolicy(POLICY_NAME);
    verify(repositoryManager).update(configuration);

    Map<String, Object> cleanup = attributes.get(CLEANUP_ATTRIBUTES_KEY);
    assertThat(cleanup, is(notNullValue()));
    @SuppressWarnings("unchecked")
    Set<String> remaining = (Set<String>) cleanup.get(CLEANUP_NAME_KEY);
    assertThat(remaining, hasItem(OTHER_POLICY));
    assertThat(remaining, not(hasItem(POLICY_NAME)));
  }

  @Test
  public void removesCleanupAttributeWhenLastPolicy() throws Exception {
    Map<String, Map<String, Object>> attributes = attributesWithPolicies(POLICY_NAME);
    Configuration configuration = mockConfiguration(attributes);
    stubBrowse(configuration);
    when(event.isLocal()).thenReturn(true);

    underTest.on(event);

    verify(repositoryManager).update(configuration);
    assertThat(attributes.get(CLEANUP_ATTRIBUTES_KEY), is(nullValue()));
  }

  @Test
  public void handlesNullAttributes() throws Exception {
    Configuration configuration = mockConfiguration(null);
    stubBrowse(configuration);
    when(event.isLocal()).thenReturn(true);

    underTest.on(event);

    verify(repositoryManager).update(configuration);
  }

  @Test
  public void doesNotModifyWhenNoCleanupAttribute() throws Exception {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Configuration configuration = mockConfiguration(attributes);
    stubBrowse(configuration);
    when(event.isLocal()).thenReturn(true);

    underTest.on(event);

    verify(repositoryManager).update(configuration);
    assertThat(attributes.containsKey(CLEANUP_ATTRIBUTES_KEY), is(false));
  }

  @Test
  public void doesNotModifyWhenPolicyNameAbsent() throws Exception {
    Map<String, Map<String, Object>> attributes = attributesWithPolicies(OTHER_POLICY);
    Configuration configuration = mockConfiguration(attributes);
    stubBrowse(configuration);
    when(event.isLocal()).thenReturn(true);

    underTest.on(event);

    verify(repositoryManager).update(configuration);

    Map<String, Object> cleanup = attributes.get(CLEANUP_ATTRIBUTES_KEY);
    assertThat(cleanup, is(notNullValue()));
    @SuppressWarnings("unchecked")
    Set<String> remaining = (Set<String>) cleanup.get(CLEANUP_NAME_KEY);
    assertThat(remaining, hasItem(OTHER_POLICY));
    assertThat(remaining, not(hasItem(POLICY_NAME)));
  }

  @Test
  public void throwsRuntimeExceptionWhenUpdateFails() throws Exception {
    Configuration configuration = mockConfiguration(null);
    stubBrowse(configuration);
    when(event.isLocal()).thenReturn(true);
    Exception failure = new Exception("boom");
    when(repositoryManager.update(any())).thenThrow(failure);

    try {
      underTest.on(event);
      fail("Expected RuntimeException to be thrown");
    }
    catch (RuntimeException e) {
      // update() declares a checked Exception; remove() catches it and rethrows wrapped, preserving the cause
      assertThat(e.getCause(), is(sameInstance(failure)));
    }
  }

  @Test
  public void rethrowsRuntimeExceptionWhenCleanupNameKeyMissing() throws Exception {
    // cleanup attribute present but missing the policyName key -> new HashSet<>((Collection) null) throws NPE,
    // which remove() catches and rethrows wrapped in a RuntimeException
    Map<String, Object> cleanup = new HashMap<>();
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put(CLEANUP_ATTRIBUTES_KEY, cleanup);
    Configuration configuration = mockConfiguration(attributes);
    stubBrowse(configuration);
    when(event.isLocal()).thenReturn(true);

    try {
      underTest.on(event);
      fail("Expected RuntimeException to be thrown");
    }
    catch (RuntimeException e) {
      assertThat(e.getCause(), is(instanceOf(NullPointerException.class)));
    }

    verify(repositoryManager, never()).update(any());
  }

  @Test
  public void updatesCopyOfConfigurationNotOriginal() throws Exception {
    Configuration original = mock(Configuration.class);
    Configuration copy = mockConfiguration(null);
    when(original.copy()).thenReturn(copy);
    Repository repository = mock(Repository.class);
    when(repository.getConfiguration()).thenReturn(original);
    when(repositoryManager.browseForCleanupPolicy(POLICY_NAME)).thenReturn(Stream.of(repository));
    when(event.isLocal()).thenReturn(true);

    underTest.on(event);

    verify(repositoryManager).update(copy);
    verify(repositoryManager, never()).update(original);
  }

  @Test
  public void removesPolicyFromAllBrowsedRepositories() throws Exception {
    Map<String, Map<String, Object>> attributesWithOthers = attributesWithPolicies(POLICY_NAME, OTHER_POLICY);
    Map<String, Map<String, Object>> attributesWithOnlyPolicy = attributesWithPolicies(POLICY_NAME);
    Configuration configurationOne = mockConfiguration(attributesWithOthers);
    Configuration configurationTwo = mockConfiguration(attributesWithOnlyPolicy);
    Repository repositoryOne = mock(Repository.class);
    Repository repositoryTwo = mock(Repository.class);
    when(repositoryOne.getConfiguration()).thenReturn(configurationOne);
    when(repositoryTwo.getConfiguration()).thenReturn(configurationTwo);
    when(repositoryManager.browseForCleanupPolicy(POLICY_NAME))
        .thenReturn(Stream.of(repositoryOne, repositoryTwo));
    when(event.isLocal()).thenReturn(true);

    underTest.on(event);

    verify(repositoryManager).update(configurationOne);
    verify(repositoryManager).update(configurationTwo);
    assertThat(attributesWithOthers.get(CLEANUP_ATTRIBUTES_KEY), is(notNullValue()));
    assertThat(attributesWithOnlyPolicy.get(CLEANUP_ATTRIBUTES_KEY), is(nullValue()));
  }

  private static Map<String, Map<String, Object>> attributesWithPolicies(final String... policyNames) {
    Set<String> names = new HashSet<>(Arrays.asList(policyNames));
    Map<String, Object> cleanup = new HashMap<>();
    cleanup.put(CLEANUP_NAME_KEY, names);
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put(CLEANUP_ATTRIBUTES_KEY, cleanup);
    return attributes;
  }

  private static Configuration mockConfiguration(final Map<String, Map<String, Object>> attributes) {
    Configuration configuration = mock(Configuration.class);
    // copy() returns the mock itself by default so tests that don't care about the copy/original
    // distinction (e.g. attributes == null cases) operate on a single configuration. Tests that DO
    // care (updatesCopyOfConfigurationNotOriginal) override this with when(original.copy()).thenReturn(copy).
    when(configuration.copy()).thenReturn(configuration);
    when(configuration.getAttributes()).thenReturn(attributes);
    when(configuration.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    return configuration;
  }

  private void stubBrowse(final Configuration configuration) {
    Repository repository = mock(Repository.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repositoryManager.browseForCleanupPolicy(POLICY_NAME)).thenReturn(Stream.of(repository));
  }
}
