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

import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DatastoreMetadataUpdater}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DatastoreMetadataUpdaterTest
{
  private static final String METADATA_PATH = "org/test/artifact/maven-metadata.xml";

  @Mock
  private Repository repository;

  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private MavenPath mavenPath;

  @Mock
  private MavenPath sha1Path;

  @Mock
  private MavenPath sha256Path;

  @Mock
  private MavenPath sha512Path;

  @Mock
  private MavenPath md5Path;

  @Mock
  private Appender<ILoggingEvent> mockAppender;

  private DatastoreMetadataUpdater metadataUpdater;

  private Logger logger;

  @Before
  public void setup() throws IOException {
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(mavenPath.hash(HashType.SHA1)).thenReturn(sha1Path);
    when(mavenPath.hash(HashType.SHA256)).thenReturn(sha256Path);
    when(mavenPath.hash(HashType.SHA512)).thenReturn(sha512Path);
    when(mavenPath.hash(HashType.MD5)).thenReturn(md5Path);
    when(mavenPath.getPath()).thenReturn(METADATA_PATH);

    metadataUpdater = new DatastoreMetadataUpdater(true, repository);

    logger = (Logger) LoggerFactory.getLogger(DatastoreMetadataUpdater.class);
    logger.addAppender(mockAppender);
  }

  @After
  public void tearDown() {
    logger.detachAppender(mockAppender);
  }

  @Test
  public void hasMissingChecksumsReturnsFalseWhenAllChecksumsExist() throws IOException {
    when(mavenContentFacet.exists(sha1Path)).thenReturn(true);
    when(mavenContentFacet.exists(sha256Path)).thenReturn(true);
    when(mavenContentFacet.exists(sha512Path)).thenReturn(true);
    when(mavenContentFacet.exists(md5Path)).thenReturn(true);

    boolean result = metadataUpdater.hasMissingChecksums(mavenPath);
    assertThat(result, is(false));
  }

  @Test
  public void hasMissingChecksumsReturnsTrueWhenSha1IsMissing() throws IOException {
    when(mavenContentFacet.exists(sha1Path)).thenReturn(false);

    boolean result = metadataUpdater.hasMissingChecksums(mavenPath);
    assertThat(result, is(true));
  }

  @Test
  public void hasMissingChecksumsReturnsTrueWhenSha256IsMissing() throws IOException {
    when(mavenContentFacet.exists(sha1Path)).thenReturn(true);
    when(mavenContentFacet.exists(sha256Path)).thenReturn(false);

    boolean result = metadataUpdater.hasMissingChecksums(mavenPath);
    assertThat(result, is(true));
  }

  @Test
  public void hasMissingChecksumsReturnsTrueWhenSha512IsMissing() throws IOException {
    when(mavenContentFacet.exists(sha1Path)).thenReturn(true);
    when(mavenContentFacet.exists(sha256Path)).thenReturn(true);
    when(mavenContentFacet.exists(sha512Path)).thenReturn(false);

    boolean result = metadataUpdater.hasMissingChecksums(mavenPath);
    assertThat(result, is(true));
  }

  @Test
  public void hasMissingChecksumsReturnsTrueWhenMd5IsMissing() throws IOException {
    when(mavenContentFacet.exists(sha1Path)).thenReturn(true);
    when(mavenContentFacet.exists(sha256Path)).thenReturn(true);
    when(mavenContentFacet.exists(sha512Path)).thenReturn(true);
    when(mavenContentFacet.exists(md5Path)).thenReturn(false);

    boolean result = metadataUpdater.hasMissingChecksums(mavenPath);
    assertThat(result, is(true));
  }

  @Test
  public void hasMissingChecksumsReturnsTrueWhenFirstChecksumIsMissing() throws IOException {
    when(mavenContentFacet.exists(sha1Path)).thenReturn(false);

    boolean result = metadataUpdater.hasMissingChecksums(mavenPath);
    assertThat(result, is(true));
  }

  @Test
  public void hasMissingChecksumsReturnsFalseOnExceptionAndLogsWarning() throws IOException {
    RuntimeException testException = new RuntimeException("Test exception");
    when(mavenContentFacet.exists(sha1Path)).thenThrow(testException);

    boolean result = metadataUpdater.hasMissingChecksums(mavenPath);

    assertThat(result, is(false));

    ArgumentCaptor<ILoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);
    verify(mockAppender).doAppend(loggingEventCaptor.capture());

    ILoggingEvent loggingEvent = loggingEventCaptor.getValue();
    assertThat(loggingEvent.getLevel(), is(Level.WARN));
    assertThat(loggingEvent.getFormattedMessage(), containsString("Error checking for missing checksums"));
    assertThat(loggingEvent.getFormattedMessage(), containsString(METADATA_PATH));
  }
}
