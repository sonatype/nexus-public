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
package org.sonatype.nexus.common.io;

import org.sonatype.goodies.testsupport.TestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;

public class ZipSupportTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final byte[] PAYLOAD = "payload".getBytes(UTF_8);

  private File root;

  private final ZipSupport zipSupport = new ZipSupport();

  @Before
  public void prepare() throws IOException {
    root = util.createTempDir();
    Path path = root.toPath();
    Files.write(path.resolve("file1.tx"), PAYLOAD);
    Files.write(path.resolve("file2.txt"), PAYLOAD);
    Files.write(path.resolve("file4.txt"), PAYLOAD);
  }

  @Test
  public void testZipFiles() throws IOException {
    List<String> filesToZip = Arrays.asList("file1.txt", "file2.txt", "file3.txt");

    String zipFileName = root.toPath() + "/test.zip";

    zipSupport.zipFiles(root.toPath(), filesToZip, zipFileName);

    File zipFile = new File(zipFileName);
    assertTrue(zipFile.exists());
  }

}
