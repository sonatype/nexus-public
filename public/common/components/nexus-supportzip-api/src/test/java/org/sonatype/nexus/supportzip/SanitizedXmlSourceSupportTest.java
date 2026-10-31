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
package org.sonatype.nexus.supportzip;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;

import com.google.common.io.Resources;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

/**
 * UT for {@link SanitizedXmlSourceSupport}.
 *
 * @since 3.0
 */
public class SanitizedXmlSourceSupportTest
{
  /**
   * Tests that a sanitizer correctly sanitizes basic content based on a provided XSLT.
   */
  @Test
  public void testSanitizeContent() throws Exception {
    String stylesheet = Resources.toString(Resources.getResource(getClass(), "sanitize.xsl"), Charset.forName("UTF-8"));

    File file = new File(Resources.getResource(getClass(), "input.xml").toURI());
    SanitizedXmlSourceSupport support = new SanitizedXmlSourceSupport(Type.CONFIG,
        "some/path",
        file,
        Priority.DEFAULT,
        stylesheet);

    support.prepare();

    try (InputStream in = support.getContent()) {
      Diff diff = DiffBuilder.compare(Input.fromURL(Resources.getResource(getClass(), "output.xml")))
          .withTest(Input.fromStream(support.getContent()))
          .ignoreWhitespace()
          .build();

      assertFalse(diff.toString(), diff.hasDifferences());
    }
  }

  /**
   * Tests that calling {@link SanitizedXmlSourceSupport#prepare()} a second time fails because the content has
   * already been generated (covers the {@code checkState(content == null)} false branch).
   */
  @Test
  public void testPrepareTwiceThrows() throws Exception {
    SanitizedXmlSourceSupport support = createSupport();

    support.prepare();

    // the second prepare() must fail because content is no longer null
    assertThrows(IllegalStateException.class, support::prepare);
  }

  /**
   * Tests that {@link SanitizedXmlSourceSupport#getSize()} returns the length of the sanitized content after a
   * successful prepare() (covers the {@code content != null} branch).
   */
  @Test
  public void testGetSizeReturnsContentLength() throws Exception {
    SanitizedXmlSourceSupport support = createSupport();

    support.prepare();

    byte[] content;
    try (InputStream in = support.getContent()) {
      content = in.readAllBytes();
    }

    assertThat(content.length, is(greaterThan(0)));
    assertThat(support.getSize(), is((long) content.length));
  }

  /**
   * Tests that {@link SanitizedXmlSourceSupport#getContent()} fails when invoked before prepare() (covers the
   * {@code checkState(content != null)} false branch).
   */
  @Test
  public void testGetContentBeforePrepareThrows() throws Exception {
    SanitizedXmlSourceSupport support = createSupport();

    // getContent() must fail because prepare() has not populated content yet
    assertThrows(IllegalStateException.class, () -> support.getContent());
  }

  /**
   * Tests that {@link SanitizedXmlSourceSupport#getSize()} fails when invoked before prepare() (covers the
   * {@code checkState(content != null)} false branch).
   */
  @Test
  public void testGetSizeBeforePrepareThrows() throws Exception {
    SanitizedXmlSourceSupport support = createSupport();

    // getSize() must fail because prepare() has not populated content yet
    assertThrows(IllegalStateException.class, () -> support.getSize());
  }

  /**
   * Tests that the constructor rejects a {@code null} stylesheet (covers the {@code checkNotNull(stylesheet)}
   * guard added by {@link SanitizedXmlSourceSupport}).
   */
  @Test
  public void testNullStylesheetThrows() throws Exception {
    File file = new File(Resources.getResource(getClass(), "input.xml").toURI());

    assertThrows(NullPointerException.class,
        () -> new SanitizedXmlSourceSupport(Type.CONFIG, "some/path", file, Priority.DEFAULT, null));
  }

  private SanitizedXmlSourceSupport createSupport() throws Exception {
    String stylesheet = Resources.toString(Resources.getResource(getClass(), "sanitize.xsl"), Charset.forName("UTF-8"));

    File file = new File(Resources.getResource(getClass(), "input.xml").toURI());
    return new SanitizedXmlSourceSupport(Type.CONFIG,
        "some/path",
        file,
        Priority.DEFAULT,
        stylesheet);
  }
}
