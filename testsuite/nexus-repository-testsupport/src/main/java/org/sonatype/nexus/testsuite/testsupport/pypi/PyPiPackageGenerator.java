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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Test utility class for building PyPi packages.
 *
 * @since 3.15
 */
public class PyPiPackageGenerator
    extends ComponentSupport
{
  private static final String TEMPLATE = "Metadata-Version: 2.0%n" +
      "Name: %s%n" +
      "Version: %s%n" +
      "Summary: %s%n" +
      "License: MIT%n" +
      "A test Python project";

  @VisibleForTesting
  static final String METADATA_FILE_NAME = "METADATA";

  public InputStream buildWheel(final String name, final String version) throws IOException {
    String metadataFile = String.format(TEMPLATE, name, version, name);
    
    return buildZipWithFileAndContents(METADATA_FILE_NAME, metadataFile);
  }

  private InputStream buildZipWithFileAndContents(final String fileName, final String contents) throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
      zipOut.putNextEntry(new ZipEntry(fileName));
      zipOut.write(contents.getBytes(UTF_8));
      zipOut.closeEntry();
    }

    return new ByteArrayInputStream(out.toByteArray());
  }
}
