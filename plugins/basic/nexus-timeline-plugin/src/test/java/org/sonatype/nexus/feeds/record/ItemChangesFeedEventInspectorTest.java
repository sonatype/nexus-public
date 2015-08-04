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
package org.sonatype.nexus.feeds.record;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.attributes.internal.DefaultAttributes;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStoreCreate;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenArtifactSignatureAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenChecksumAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenRepositoryMetadataAttribute;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ItemChangesFeedEventInspectorTest
    extends TestSupport
{

  @Mock
  private FeedRecorder feedRecorder;

  @Mock
  private ApplicationStatusSource applicationStatusSource;

  @Mock
  private Repository repository;

  @Mock
  private StorageFileItem storageFileItem;

  @Mock
  private RepositoryItemUid repositoryItemUid;

  @Before
  public void setup() {
    when(repository.getId()).thenReturn("test");
    when(storageFileItem.getItemContext()).thenReturn(new RequestContext());
    when(storageFileItem.getRepositoryItemUid()).thenReturn(repositoryItemUid);
    when(storageFileItem.getRepositoryItemAttributes()).thenReturn(new DefaultAttributes());
  }

  @Test
  public void eventsOnHiddenFilesAreNotRecorded() {
    final ItemChangesFeedEventInspector underTest =
        new ItemChangesFeedEventInspector(feedRecorder, applicationStatusSource);
    final RepositoryItemEventStoreCreate evt = new RepositoryItemEventStoreCreate(repository, storageFileItem);
    when(repositoryItemUid.getBooleanAttributeValue(IsHiddenAttribute.class)).thenReturn(true);
    underTest.on(evt);

    verifyNoMoreInteractions(feedRecorder);
  }

  @Test
  public void eventsOnMavenMetadataSignatureAndHashFilesShouldNotBeRecorded() {
    final ItemChangesFeedEventInspector underTest =
        new ItemChangesFeedEventInspector(feedRecorder, applicationStatusSource);
    {
      final RepositoryItemEventStoreCreate evt = new RepositoryItemEventStoreCreate(repository, storageFileItem);
      when(repositoryItemUid.getBooleanAttributeValue(IsMavenRepositoryMetadataAttribute.class)).thenReturn(
          true);
      underTest.on(evt);
    }
    {
      final RepositoryItemEventStoreCreate evt = new RepositoryItemEventStoreCreate(repository, storageFileItem);
      when(repositoryItemUid.getBooleanAttributeValue(IsMavenArtifactSignatureAttribute.class)).thenReturn(
          true);
      underTest.on(evt);
    }
    {
      final RepositoryItemEventStoreCreate evt = new RepositoryItemEventStoreCreate(repository, storageFileItem);
      when(repositoryItemUid.getBooleanAttributeValue(IsMavenChecksumAttribute.class)).thenReturn(true);
      underTest.on(evt);
    }

    // these events above should be filtered out by ItemChangesFeedEventInspector, feedRecordes shall be untouched
    verifyNoMoreInteractions(feedRecorder);

    // now do touch it (with event that has all the flags we added false)
    final RepositoryItemEventStoreCreate evt = new RepositoryItemEventStoreCreate(repository, storageFileItem);
    when(repositoryItemUid.getBooleanAttributeValue(IsMavenRepositoryMetadataAttribute.class)).thenReturn(
        false);
    when(repositoryItemUid.getBooleanAttributeValue(IsMavenArtifactSignatureAttribute.class)).thenReturn(false);
    when(repositoryItemUid.getBooleanAttributeValue(IsMavenChecksumAttribute.class)).thenReturn(false);
    underTest.on(evt);
    // method touched only once
    verify(feedRecorder, times(1)).addNexusArtifactEvent(any(NexusArtifactEvent.class));
  }
}
