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
package org.sonatype.nexus.script.plugin.internal.provisioning;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class RepositoryApiImplTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BlobStoreManager blobStoreManager;

  @InjectMocks
  private RepositoryApiImpl api;

  @Test(expected = IllegalArgumentException.class)
  public void testCannotValidateBlobStoreThatDoesNotExist() {
    when(blobStoreManager.browse()).thenReturn(Collections.emptyList());
    api.validateBlobStore(configWithAttributes(ImmutableMap.of("storage", ImmutableMap.of("blobStoreName", "foo"))));
  }

  @Test
  public void testCanValidateGivenAnExistingBlobStore() {
    BlobStore blobStore = mock(BlobStore.class);
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();
    configuration.setName("foo");

    when(blobStoreManager.browse()).thenReturn(Collections.singletonList(blobStore));
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);

    api.validateBlobStore(configWithAttributes(ImmutableMap.of("storage", ImmutableMap.of("blobStoreName", "foo"))));

    verify(blobStoreManager).browse();
    verify(blobStore).getBlobStoreConfiguration();
  }

  @Test(expected = ClassCastException.class)
  public void testGroupMemberNamesMustBeACollection() {
    api.validateGroupMembers(configWithAttributes(ImmutableMap.of("group", ImmutableMap.of("memberNames", "foo"))));
  }

  @Test(expected = IllegalStateException.class)
  public void testCannotValidateGroupThatContainsNonExistentMembers() {
    when(repositoryManager.browse()).thenReturn(Collections.emptyList());
    api.validateGroupMembers(configWithAttributes(
        ImmutableMap.of("group", ImmutableMap.of("memberNames", Collections.singletonList("foo")))));
  }

  @Test
  public void testCanValidateGroupWithExistingMembers() {
    Repository repository = mock(Repository.class);

    when(repositoryManager.browse()).thenReturn(Collections.singletonList(repository));
    when(repository.getName()).thenReturn("foo");

    api.validateGroupMembers(configWithAttributes(
        ImmutableMap.of("group", ImmutableMap.of("memberNames", Collections.singletonList("foo")))));

    verify(repositoryManager).browse();
    verify(repository).getName();
  }

  @Test
  public void testNonGroupRepositoriesPassGroupValidationTrivially() {
    api.validateGroupMembers(configWithAttributes(Collections.emptyMap()));
    assertTrue(true);
  }

  private Configuration configWithAttributes(final Map<String, Map<String, Object>> attributes) {
    Configuration config = mock(Configuration.class);
    when(config.getAttributes()).thenReturn(attributes);
    return config;
  }
}
