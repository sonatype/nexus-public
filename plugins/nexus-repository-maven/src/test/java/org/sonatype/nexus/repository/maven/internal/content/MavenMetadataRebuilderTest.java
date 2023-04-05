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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

public class MavenMetadataRebuilderTest
    extends TestSupport
{
  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private MavenPathParser mavenPathParser;

  @Mock
  private Repository repository;

  @Mock
  private Appender<ILoggingEvent> mockAppender;

  @Mock
  private FluentAssets assets;

  @Mock
  private FluentComponents components;

  @Before
  public void setup() {
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.getFormat()).thenReturn(new Maven2Format());
    when(mavenContentFacet.getMavenPathParser()).thenReturn(mavenPathParser);
    when(mavenPathParser.parsePath(anyString())).thenReturn(mock(MavenPath.class));
    when(mavenContentFacet.assets()).thenReturn(assets);
    when(mavenContentFacet.components()).thenReturn(components);

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

      new MavenMetadataRebuilder(20).rebuild(repository, true, false, null, null, null);
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

  private Continuation infiniteContinuation(final Object returnItem) {
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
