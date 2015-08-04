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
package org.sonatype.nexus.mime;

import java.util.List;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultMimeSupport}.
 */
public class DefaultMimeSupportTest
    extends TestSupport
{
  private DefaultMimeSupport mimeSupport;

  @Mock
  private NexusMimeTypes mimeTypes;

  @Mock
  private NexusMimeTypes.NexusMimeType mimeType;

  @Before
  public void setUp() throws Exception {
    mimeSupport = new DefaultMimeSupport();
  }

  private void mock() {
    this.mimeSupport = new DefaultMimeSupport(mimeTypes);
  }

  /**
   * Tests the simple "guessing" against some known paths.
   */
  @Test
  public void testGuessMimeTypeFromPath() {
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/artifact.pom"), equalTo("application/xml"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/artifact.jar"),
        equalTo("application/java-archive"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/artifact-sources.jar"),
        equalTo("application/java-archive"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/maven-metadata.xml"), equalTo("application/xml"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.xml"), equalTo("application/xml"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.tar.gz"), equalTo("application/x-gzip"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.tar.bz2"), equalTo("application/x-bzip2"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.zip"), equalTo("application/zip"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.war"), equalTo("application/java-archive"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.rar"), equalTo("application/java-archive"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.ear"), equalTo("application/java-archive"));
    assertThat(mimeSupport.guessMimeTypeFromPath("/some/path/some.ejb"), equalTo("application/java-archive"));
  }

  /**
   * Tests that repo with diverting MimeRulesSupport actually works. If both tests, this one and
   * {@link #testGuessMimeTypeFromPath()} passes, their conjunction proves it works.
   */
  @Test
  public void testGuessfakeMimeRulesSourceMimeTypeFromPath() {
    MimeRulesSource fakeMimeRulesSource = new MimeRulesSource()
    {
      @Override
      public String getRuleForPath(String path) {
        return "foo/bar";
      }
    };

    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/artifact.pom"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/artifact.jar"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/artifact-sources.jar"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/maven-metadata.xml"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/some.xml"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/some.tar.gz"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/some.tar.bz2"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/some.zip"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/some.war"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/some.ear"),
        equalTo("foo/bar"));
    assertThat(mimeSupport.guessMimeTypeFromPath(fakeMimeRulesSource, "/some/path/some.ejb"),
        equalTo("foo/bar"));
  }

  @Test
  public void testGuessNullMimeRulesSourceMimeTypeFromPath() {
    assertThat(mimeSupport.guessMimeTypeFromPath(null, "/some/path/artifact.pom"), equalTo("application/xml"));
  }

  @Test
  public void useNexusMimeTypes() {
    mock();
    when(mimeTypes.getMimeTypes("test")).thenReturn(mimeType);
    when(mimeType.getExtension()).thenReturn("test");
    when(mimeType.getMimetypes()).thenReturn(Lists.newArrayList("fake/mimetype"));

    assertThat(mimeSupport.guessMimeTypeFromPath("foo.test"), is("fake/mimetype"));
  }

  @Test
  public void retainDefaultMimeTypes() {
    // empty NexusMimeTypes
    mock();

    assertThat(mimeSupport.guessMimeTypeFromPath("foo.doc"), is("application/msword"));
  }

  @Test
  public void preferDefaultMimeType() {
    mock();
    when(mimeTypes.getMimeTypes("zip")).thenReturn(mimeType);
    when(mimeType.getExtension()).thenReturn("zip");
    when(mimeType.getMimetypes()).thenReturn(Lists.newArrayList("fake/mimetype"));

    final List<String> mimeTypes = mimeSupport.guessMimeTypesListFromPath("foo.zip");
    assertThat(mimeTypes, hasSize(3));
    assertThat(mimeTypes, hasItem("application/zip"));
    assertThat(mimeTypes, hasItem("fake/mimetype"));
    assertThat(mimeTypes, hasItem("application/octet-stream"));
    assertThat(mimeSupport.guessMimeTypeFromPath("foo.zip"), is("fake/mimetype"));
  }

  @Test
  public void overrideDefaultMimeType() {
    mock();
    when(mimeTypes.getMimeTypes("zip")).thenReturn(mimeType);
    when(mimeType.getExtension()).thenReturn("zip");
    when(mimeType.isOverride()).thenReturn(true);
    when(mimeType.getMimetypes()).thenReturn(Lists.newArrayList("fake/mimetype"));

    assertThat(mimeSupport.guessMimeTypeFromPath("foo.zip"), is("fake/mimetype"));

  }
}
