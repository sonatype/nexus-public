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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;

public class MetadataRebuilderTest
    extends TestSupport
{
  @Mock
  private StorageFacet storageFacet;

  @Mock
  private MavenFacet mavenFacet;

  @Mock
  private MavenPathParser mavenPathParser;

  @Mock
  private Repository repository;

  @Mock
  private StorageTx storageTx;

  private Bucket bucket;

  private static EntityMetadata mockBucketEntityMetadata() {
    EntityAdapter owner = mock(EntityAdapter.class);
    ODocument document = mock(ODocument.class);
    when(document.getIdentity()).thenReturn(new ORecordId(1, 1));
    return new AttachedEntityMetadata(owner, document);
  }

  @Before
  public void setup() {
    bucket = new Bucket();
    bucket.setEntityMetadata(mockBucketEntityMetadata());

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.facet(MavenFacet.class)).thenReturn(mavenFacet);
    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(mavenFacet.getMavenPathParser()).thenReturn(mavenPathParser);
    when(mavenPathParser.parsePath(anyString())).thenReturn(mock(MavenPath.class));
  }

  @Test
  public void verifyAsyncControlArguments() {
    ArgumentCaptor<Integer> bufferSizeCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Long> timeoutSecondsCaptor = ArgumentCaptor.forClass(Long.class);

    when(storageTx
        .browse(anyString(), anyMapOf(String.class, Object.class), bufferSizeCaptor.capture(),
            timeoutSecondsCaptor.capture()))
        .thenReturn(emptyList());

    new MetadataRebuilder(10, 20).rebuild(repository, true, false, null, null, null);

    assertThat(bufferSizeCaptor.getValue(), equalTo(10));
    assertThat(timeoutSecondsCaptor.getValue(), equalTo(20L));
  }

  @Test
  public void rebuildIsCancelable() throws Exception {
    ODocument doc = mock(ODocument.class);
    when(doc.field("groupId", OType.STRING)).thenReturn("group");
    when(doc.field("artifactId", OType.STRING)).thenReturn("artifact");
    when(doc.field("baseVersions", OType.EMBEDDEDSET)).thenReturn(newHashSet("version"));

    Component component = mock(Component.class);
    
    NestedAttributesMap attributes = mock(NestedAttributesMap.class);
    when(attributes.get(P_BASE_VERSION)).thenReturn("version");
    
    doReturn(attributes).when(component).formatAttributes();
    doReturn(infiniteIterator(doc)).when(storageTx).browse(anyString(), anyMapOf(String.class, Object.class), anyInt(), anyLong());
    doReturn(infiniteIterator(component)).when(storageTx).browseComponents(any(), any());
    doReturn(infiniteIterator(mock(Asset.class))).when(storageTx).browseAssets(any(Component.class));

    final AtomicBoolean canceled = new AtomicBoolean(false);
    final List<Throwable> uncaught = new ArrayList<>();
    Thread taskThread = new Thread(() -> {
      CancelableHelper.set(canceled);

      new MetadataRebuilder(10, 20).rebuild(repository, true, false, "group", "artifact", "version");
    });
    taskThread.setUncaughtExceptionHandler((t, e) -> {
      if (e instanceof TaskInterruptedException) {
        return;
      }

      uncaught.add(e);
    });
    taskThread.start();

    sleep((long) (Math.random() * 1000)); // sleep for up to a second (emulate task running)
    canceled.set(true); // cancel the task
    taskThread.join(5000); // ensure task thread ends

    if (taskThread.isAlive()) {
      fail("Task did not cancel");
    }

    if (uncaught.size() > 0) {
      throw new MultipleFailureException(uncaught);
    }
  }

  private Iterable infiniteIterator(Object returnItem) {
    Iterable iterable = mock(Iterable.class);
    Iterator iterator = mock(Iterator.class);

    when(iterable.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(returnItem);

    return iterable;
  }
}
