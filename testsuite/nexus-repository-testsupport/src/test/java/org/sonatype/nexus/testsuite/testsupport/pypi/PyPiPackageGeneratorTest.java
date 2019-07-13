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
package org.sonatype.nexus.testsuite.testsupport.pypi;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.sonatype.nexus.testsuite.testsupport.pypi.PyPiPackageGenerator.METADATA_FILE_NAME;

public class PyPiPackageGeneratorTest
    extends TestSupport
{
  @Test
  public void shouldBuildWheelWithNameAndVersion() throws Exception {
    String name = "test";
    String version = "1.1.0";
    InputStream wheel = new PyPiPackageGenerator().buildWheel(name, version);

    String metadata = extractMetadataFile(wheel);

    assertThat(metadata, containsString("Metadata-Version: 2.0" + System.lineSeparator() +
        "Name: test" + System.lineSeparator() +
        "Version: 1.1.0" + System.lineSeparator() +
        "Summary: test" + System.lineSeparator() +
        "License: MIT" + System.lineSeparator() +
        "A test Python project"));
  }

  private String extractMetadataFile(final InputStream wheel) throws IOException {
    try (ZipInputStream zip = new ZipInputStream(wheel)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.getName().equals(METADATA_FILE_NAME)) {
          return IOUtils.toString(zip);
        }
      }
    }
    return null;
  }
}
