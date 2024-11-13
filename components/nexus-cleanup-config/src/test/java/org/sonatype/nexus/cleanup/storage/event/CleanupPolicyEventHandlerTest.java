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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.empty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT;

public class CleanupPolicyEventHandlerTest
    extends TestSupport
{
  // TODO ORIENT rewrite
  //private static final String CLEANUP_ATTRIBUTES_KEY = "cleanup";
  //
  //private static final String CLEANUP_NAME_KEY = "policyName";
  //
  //@Mock
  //private OrientCleanupPolicy cleanupPolicy1, cleanupPolicy2, cleanupPolicy3;
  //
  //@Mock
  //private EntityMetadata entityMetadata1, entityMetadata2, entityMetadata3;
  //
  //@Mock
  //private RepositoryManager repositoryManager;
  //
  //@Mock
  //private Repository repository1, repository2;
  //
  //@Mock
  //private Configuration configuration1, configuration2;
  //
  //private Map<String, Map<String, Object>> attributes1, attributes2;
  //
  //private String name1, name2, name3;
  //
  //private CleanupPolicyEventHandler underTest;
  //
  //@Before
  //public void setup() throws Exception {
  //  underTest = new CleanupPolicyEventHandler(repositoryManager);
  //
  //  name1 = generateValidName();
  //  name2 = generateValidName();
  //  name3 = generateValidName();
  //
  //  Map<String, Object> cleanupAttributes1 = newHashMap();
  //  cleanupAttributes1.put(CLEANUP_NAME_KEY, newLinkedHashSet(singletonList(name1)));
  //
  //  Map<String, Object> cleanupAttributes2 = newHashMap();
  //  cleanupAttributes2.put(CLEANUP_NAME_KEY, newLinkedHashSet(asList(name2, name3)));
  //
  //  attributes1 = newHashMap();
  //  attributes1.put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes1);
  //
  //  attributes2 = newHashMap();
  //  attributes2.put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes2);
  //
  //  when(cleanupPolicy1.getName()).thenReturn(name1);
  //  when(cleanupPolicy2.getName()).thenReturn(name2);
  //  when(cleanupPolicy3.getName()).thenReturn(name3);
  //  when(cleanupPolicy1.getFormat()).thenReturn(ALL_CLEANUP_POLICY_FORMAT);
  //  when(cleanupPolicy2.getFormat()).thenReturn(ALL_CLEANUP_POLICY_FORMAT);
  //  when(cleanupPolicy3.getFormat()).thenReturn(ALL_CLEANUP_POLICY_FORMAT);
  //  when(entityMetadata1.getEntity()).thenReturn(Optional.of(cleanupPolicy1));
  //  when(entityMetadata2.getEntity()).thenReturn(Optional.of(cleanupPolicy2));
  //  when(entityMetadata3.getEntity()).thenReturn(Optional.of(cleanupPolicy3));
  //  when(configuration1.copy()).thenReturn(configuration1);
  //  when(configuration2.copy()).thenReturn(configuration2);
  //  when(configuration1.getAttributes()).thenReturn(attributes1);
  //  when(configuration2.getAttributes()).thenReturn(attributes2);
  //  when(repository1.getConfiguration()).thenReturn(configuration1);
  //  when(repository2.getConfiguration()).thenReturn(configuration2);
  //  when(repositoryManager.browseForCleanupPolicy(name1)).thenReturn(Stream.of(repository1));
  //  when(repositoryManager.browseForCleanupPolicy(name2)).thenReturn(Stream.of(repository2));
  //  when(repositoryManager.browseForCleanupPolicy(name3)).thenReturn(Stream.of(repository2));
  //}
  //
  //@Test
  //public void removedCleanupAttributeFromRepository() throws Exception {
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata1));
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata2));
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata3));
  //
  //  assertThat(attributes1.get(CLEANUP_ATTRIBUTES_KEY), nullValue());
  //  assertThat(attributes2.get(CLEANUP_ATTRIBUTES_KEY), nullValue());
  //
  //  verifyConfigurationUpdatedWithoutCleanupPolicyAttribute(3);
  //}
  //
  //@Test
  //public void removedOneCleanupPolicyFromRepositoryWithMultiPolicy() throws Exception {
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata3));
  //
  //  verifyConfigurationUpdated(1);
  //  verifyContainsCleanupPolicies(attributes2, cleanupPolicy2.getName());
  //}
  //
  //@Test
  //public void onlyRepositoryWithMatchingCleanupPolicyGetsAttributesRemoved() throws Exception {
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata1));
  //
  //  assertThat(attributes1.get(CLEANUP_ATTRIBUTES_KEY), nullValue());
  //  assertThat(attributes2.get(CLEANUP_ATTRIBUTES_KEY), notNullValue());
  //
  //  verifyConfigurationUpdatedWithoutCleanupPolicyAttribute(1);
  //  verifyContainsCleanupPolicies(attributes2, cleanupPolicy2.getName(), cleanupPolicy3.getName());
  //}
  //
  //@Test
  //public void multipleRepositoriesWithSameMatchingCleanupPolicyGetTheirAttributesRemoved() throws Exception {
  //  // we make the second configuration return the same attributes as the first
  //  when(repositoryManager.browseForCleanupPolicy(name1)).thenReturn(Stream.of(repository1, repository2));
  //  when(configuration2.getAttributes()).thenReturn(attributes1);
  //
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata1));
  //
  //  assertThat(attributes1.get(CLEANUP_ATTRIBUTES_KEY), nullValue());
  //  assertThat(attributes2.get(CLEANUP_ATTRIBUTES_KEY), notNullValue());
  //
  //  verifyConfigurationUpdatedWithoutCleanupPolicyAttribute(2);
  //  verifyContainsCleanupPolicies(attributes2, cleanupPolicy2.getName(), cleanupPolicy3.getName());
  //}
  //
  //@Test
  //public void configurationWithoutRepositoryDoesNotGetUpdated() throws Exception {
  //  when(repositoryManager.browseForCleanupPolicy(any())).thenReturn(empty());
  //
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata1));
  //
  //  when(repositoryManager.browseForCleanupPolicy(any())).thenReturn(empty());
  //  underTest.on(new OrientCleanupPolicyDeletedEvent(entityMetadata2));
  //
  //  verifyConfigurationNeverUpdated();
  //}
  //
  //private void verifyConfigurationUpdatedWithoutCleanupPolicyAttribute(final int count) throws Exception {
  //  ArgumentCaptor<Configuration> argument = ArgumentCaptor.forClass(Configuration.class);
  //  verify(repositoryManager, times(count)).update(argument.capture());
  //}
  //
  //private void verifyConfigurationUpdated(final int count) throws Exception {
  //  ArgumentCaptor<Configuration> argument = ArgumentCaptor.forClass(Configuration.class);
  //  verify(repositoryManager, times(count)).update(argument.capture());
  //}
  //
  //@SuppressWarnings("unchecked")
  //private void verifyContainsCleanupPolicies(final Map<String, Map<String, Object>> attributes,
  //                                           final String... cleanupNames)
  //{
  //  Set<String> names = (Set<String>) attributes.get(CLEANUP_ATTRIBUTES_KEY).get(CLEANUP_NAME_KEY);
  //  assertThat(names, hasSize(cleanupNames.length));
  //  assertThat(names, contains(cleanupNames));
  //}
  //
  //private void verifyConfigurationNeverUpdated() throws Exception {
  //  verify(repositoryManager, times(0)).update(any());
  //}
  //
  //private String generateValidName() {
  //  return randomUUID().toString().replace("-", "");
  //}
}
