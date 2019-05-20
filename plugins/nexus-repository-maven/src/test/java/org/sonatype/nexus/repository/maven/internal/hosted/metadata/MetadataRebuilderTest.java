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

import java.io.IOException;
import java.io.InputStream;
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
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
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

  @Mock
  private Appender mockAppender;

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

    Logger logger = (Logger)LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    logger.addAppender(mockAppender);
  }

  @After
  public void teardown() {
    Logger logger = (Logger)LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    logger.detachAppender(mockAppender);
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

  @Test
  public void rebuildDoesntFailOnException() throws Exception {
    final String group1 = "group1";
    final String artifact1 = "artifact1";
    final String version1 = "1.0-SNAPSHOT";
    final String group2 = "group2";
    final String artifact2 = "artifact2";
    final String version2 = "2.0-SNAPSHOT";
    Content content = mock(Content.class);
    when(mavenFacet.get(any(MavenPath.class))).thenReturn(content);

    // setup objects necessary to trigger actual metadata construction in MetadataBuilder
    ODocument doc = mock(ODocument.class);
    when(doc.field("groupId", OType.STRING)).thenReturn(group1, group2);
    when(doc.field("artifactId", OType.STRING)).thenReturn(artifact1, artifact2);
    when(doc.field("baseVersions", OType.EMBEDDEDSET)).thenReturn(newHashSet(version1), newHashSet(version2));

    Component component = mock(Component.class);
    NestedAttributesMap attributes = mock(NestedAttributesMap.class);
    when(attributes.get(P_BASE_VERSION)).thenReturn(version1, version2);
    when(component.formatAttributes()).thenReturn(attributes);

    MavenPath gav1Path = mock(MavenPath.class);
    Coordinates gav1Coordinates = mock(Coordinates.class);
    when(gav1Path.getCoordinates()).thenReturn(gav1Coordinates);
    when(gav1Coordinates.getGroupId()).thenReturn(group1);
    when(gav1Coordinates.getArtifactId()).thenReturn(artifact1);
    when(gav1Coordinates.getBaseVersion()).thenReturn(version1);

    MavenPath gav2Path = mock(MavenPath.class);
    Coordinates gav2Coordinates = mock(Coordinates.class);
    when(gav2Path.getCoordinates()).thenReturn(gav2Coordinates);
    when(gav2Coordinates.getGroupId()).thenReturn(group2);
    when(gav2Coordinates.getArtifactId()).thenReturn(artifact2);
    when(gav2Coordinates.getBaseVersion()).thenReturn(version2);

    when(mavenPathParser.parsePath(anyString())).thenReturn(gav1Path, gav2Path);

    // wiring to return the above
    doReturn(iteratorWithItems(doc, doc)).when(storageTx)
        .browse(anyString(), anyMapOf(String.class, Object.class), anyInt(), anyLong());
    when(storageTx.browseComponents(any(), any())).thenAnswer(i -> iteratorWithItems(component));
    when(storageTx.browseAssets(any(Component.class))).thenAnswer(i -> iteratorWithItems(mock(Asset.class)));

    // stage error on first artifact
    when(content.openInputStream()).thenThrow(new IOException()).thenReturn(mock(InputStream.class));

    new MetadataRebuilder(10, 20).rebuild(repository, true, false, null, null, null);

    // ensure second metadata read occurs
    verify(content, times(2)).openInputStream();

    // verify metadata errors are logged
    ArgumentCaptor<LoggingEvent> logCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
    verify(mockAppender, atLeastOnce()).doAppend(logCaptor.capture());
    List<LoggingEvent> allValues = logCaptor.getAllValues();
    assertThat(allValues.stream()
        .filter(e -> e.getLevel() == Level.WARN)
        .filter(e -> e.getMessage().equals("Errors encountered during metadata rebuild:"))
        .count(), is(1L));
  }

  private Iterable infiniteIterator(Object returnItem) {
    Iterable iterable = mock(Iterable.class);
    Iterator iterator = mock(Iterator.class);

    when(iterable.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(returnItem);

    return iterable;
  }

  private Iterable iteratorWithItems(Object... returnItems) {
    int numItems = returnItems.length;
    Iterable iterable = mock(Iterable.class);
    Iterator iterator = mock(Iterator.class);

    when(iterable.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenAnswer(new Answer<Boolean>() {
      int counter = 0;

      @Override
      public Boolean answer(final InvocationOnMock invocation) {
        return counter++ < numItems;
      }
    });

    when(iterator.next()).thenAnswer(new Answer() {
      int counter = 0;

      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        return returnItems[counter++];
      }
    });

    return iterable;
  }
}
