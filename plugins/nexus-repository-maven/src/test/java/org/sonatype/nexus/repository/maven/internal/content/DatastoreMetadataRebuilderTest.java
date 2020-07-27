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
package org.sonatype.nexus.repository.maven.internal.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_PACKAGING;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

public class DatastoreMetadataRebuilderTest
    extends TestSupport
{
  @Mock
  private ContentFacet contentFacet;

  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private MavenPathParser mavenPathParser;

  @Mock
  private Repository repository;

  @Mock
  private Appender mockAppender;

  @Mock
  private FluentAssets assets;

  @Mock
  private FluentComponents components;

  @Before
  public void setup() {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.getFormat()).thenReturn(new Maven2Format());
    when(mavenContentFacet.getMavenPathParser()).thenReturn(mavenPathParser);
    when(mavenPathParser.parsePath(anyString())).thenReturn(mock(MavenPath.class));
    when(contentFacet.assets()).thenReturn(assets);
    when(contentFacet.components()).thenReturn(components);

    Logger logger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    logger.addAppender(mockAppender);
  }

  @After
  public void teardown() {
    Logger logger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    logger.detachAppender(mockAppender);
  }

  @Test
  public void rebuildIsCancelable() throws Exception {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    doReturn(infiniteContinuation(component)).when(components).browse(anyInt(), anyString());
    doReturn(infiniteContinuation(asset)).when(assets).browse(anyInt(), anyString());

    final AtomicBoolean canceled = new AtomicBoolean(false);
    final List<Throwable> uncaught = new ArrayList<>();
    Thread taskThread = new Thread(() -> {
      CancelableHelper.set(canceled);

      new DatastoreMetadataRebuilder(10, 20).rebuild(repository, true, false, "group", "artifact", "version");
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
    final String path1_noSlash = "group1/artifact1/1.0-SNAPSHOT/artifact1-1.0-SNAPSHOT.jar";
    final String path1 = "/" + path1_noSlash;
    final String group2 = "group2";
    final String artifact2 = "artifact2";
    final String version2 = "2.0-SNAPSHOT";
    final String path2_noSlash = "/group1/artifact1/2.0-SNAPSHOT/artifact1-2.0-SNAPSHOT.jar";
    final String path2 = "/" + path2_noSlash;

    when(components.namespaces()).thenReturn(Arrays.asList(group1, group2));
    when(components.names(group1)).thenReturn(Collections.singletonList(artifact1));
    when(components.names(group2)).thenReturn(Collections.singletonList(artifact2));
    when(components.versions(group1, artifact1)).thenReturn(Collections.singletonList(version1));
    when(components.versions(group2, artifact2)).thenReturn(Collections.singletonList(version2));

    NestedAttributesMap attributesMap1 =
        new NestedAttributesMap(P_ATTRIBUTES, ImmutableMap.of(P_PACKAGING, "jar", P_BASE_VERSION, version1));
    FluentComponentBuilder componentBuilder1 = mock(FluentComponentBuilder.class);
    when(components.name(artifact1)).thenReturn(componentBuilder1);
    when(componentBuilder1.namespace(group1)).thenReturn(componentBuilder1);
    when(componentBuilder1.version(version1)).thenReturn(componentBuilder1);
    FluentComponent component1 = mock(FluentComponent.class);
    when(component1.attributes(Maven2Format.NAME)).thenReturn(attributesMap1);
    when(componentBuilder1.find()).thenReturn(Optional.of(component1));
    FluentAsset asset1 = mock(FluentAsset.class);
    when(asset1.path()).thenReturn(path1);
    when(asset1.blob()).thenReturn(Optional.of(mock(AssetBlob.class)));
    when(component1.assets()).thenReturn(Collections.singletonList(asset1));
    when(asset1.component()).thenReturn(Optional.of(component1));
    AssetBlob assetBlob1 = mock(AssetBlob.class);
    when(assetBlob1.checksums()).thenReturn(Collections.emptyMap());
    when(asset1.blob()).thenReturn(Optional.of(assetBlob1));

    NestedAttributesMap attributesMap2 =
        new NestedAttributesMap(P_ATTRIBUTES, ImmutableMap.of(P_PACKAGING, "jar", P_BASE_VERSION, version2));
    FluentComponentBuilder componentBuilder2 = mock(FluentComponentBuilder.class);
    when(components.name(artifact2)).thenReturn(componentBuilder2);
    when(componentBuilder2.namespace(group2)).thenReturn(componentBuilder2);
    when(componentBuilder2.version(version2)).thenReturn(componentBuilder2);
    FluentComponent component2 = mock(FluentComponent.class);
    when(component2.attributes(Maven2Format.NAME)).thenReturn(attributesMap2);
    when(componentBuilder2.find()).thenReturn(Optional.of(component2));
    FluentAsset asset2 = mock(FluentAsset.class);
    when(asset2.path()).thenReturn(path2);
    when(asset2.blob()).thenReturn(Optional.of(mock(AssetBlob.class)));
    when(component2.assets()).thenReturn(Collections.singletonList(asset2));
    when(asset2.component()).thenReturn(Optional.of(component2));
    AssetBlob assetBlob2 = mock(AssetBlob.class);
    when(assetBlob2.checksums()).thenReturn(Collections.emptyMap());
    when(asset2.blob()).thenReturn(Optional.of(assetBlob2));

    MavenPath mavenPath1 = mock(MavenPath.class);
    when(mavenPath1.getPath()).thenReturn(path1_noSlash);
    when(mavenPath1.hash(Matchers.any())).thenCallRealMethod();
    MavenPath mavenPath2 = mock(MavenPath.class);
    when(mavenPath2.getPath()).thenReturn(path2_noSlash);
    when(mavenPath2.hash(Matchers.any())).thenCallRealMethod();

    FluentAssetBuilder assetBuilder1 = mock(FluentAssetBuilder.class);
    when(assetBuilder1.find()).thenReturn(Optional.of(asset1));
    when(assets.path(path1)).thenReturn(assetBuilder1);
    when(mavenPathParser.parsePath(path1)).thenReturn(mavenPath1);

    FluentAssetBuilder assetBuilder2 = mock(FluentAssetBuilder.class);
    when(assetBuilder2.find()).thenReturn(Optional.of(asset2));
    when(assets.path(path2)).thenReturn(assetBuilder2);
    when(mavenPathParser.parsePath(path2)).thenReturn(mavenPath2);

    Content content = mock(Content.class);
    when(mavenContentFacet.get(Matchers.any(MavenPath.class))).thenReturn(Optional.of(content));

    // stage error on first artifact
    when(content.openInputStream()).thenThrow(new IOException()).thenReturn(mock(InputStream.class));

    new DatastoreMetadataRebuilder(10, 20).rebuild(repository, true, true, null, null, null);

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

  private Continuation infiniteContinuation(Object returnItem) {
    Continuation continuation = mock(Continuation.class);
    Iterator iterator = mock(Iterator.class);
    Spliterator spliterator = mock(Spliterator.class);

    when(continuation.spliterator()).thenReturn(spliterator);
    when(continuation.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(returnItem);

    return continuation;
  }
}
