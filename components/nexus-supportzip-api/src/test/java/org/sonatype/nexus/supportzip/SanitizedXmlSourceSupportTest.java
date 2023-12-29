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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;

import com.google.common.io.Resources;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import static org.junit.Assert.assertFalse;

/**
 * UT for {@link SanitizedXmlSourceSupport}.
 *
 * @since 3.0
 */
public class SanitizedXmlSourceSupportTest
    extends TestSupport
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
}
