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
package org.sonatype.nexus.yum.internal;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.yum.internal.support.YumNexusTestSupport;
import org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers;
import org.sonatype.sisu.resource.scanner.scanners.SerialScanner;

import org.junit.Test;

import static java.io.File.pathSeparator;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class RpmListWriterTest
    extends YumNexusTestSupport
{

  private static final String FILE_CONTENT = "another-artifact/0.0.1/another-artifact-0.0.1-1.noarch.rpm\n"
      + "conflict-artifact/2.2-1/conflict-artifact-2.2-1.noarch.rpm\n"
      + "conflict-artifact/2.2-2/conflict-artifact-2.2-2.noarch.rpm\n"
      + "test-artifact/1.2/test-artifact-1.2-1.noarch.rpm\n" + "test-artifact/1.3/test-artifact-1.3-1.noarch.rpm\n";

  private static final String NEW_RPM1 = "newAddFileRpm1.rpm";

  private static final String NEW_RPM2 = "newAddFileRpm2.rpm";

  public static final String NO_VERSION = null;

  public static final String NO_ADDED_FILE = null;

  public static final boolean NO_SINGLE_RPM_PER_DIRECTORY = true;

  @Test
  public void shouldListFileInSubDirs()
      throws Exception
  {
    assertThat(writeRpmListFile(rpmsDir(), NO_VERSION), FileMatchers.containsOnly(
        osIndependent(FILE_CONTENT)));
  }

  @Test
  public void shouldListPackagesOnHighestLevel()
      throws Exception
  {
    assertThat(
        writeRpmListFile(new File(rpmsDir(), "conflict-artifact/2.2-2"), NO_VERSION),
        FileMatchers.containsOnly("conflict-artifact-2.2-2.noarch.rpm\n")
    );
  }

  @Test
  public void shouldRemoveNotExistingRpmsAndAddNewlyAddedFile()
      throws Exception
  {
    final File rpmListFile = new RpmListWriter(
        new File(rpmsDir(), "tomcat-mysql-jdbc/5.1.15-2"),
        "/is24-tomcat-mysql-jdbc-5.1.15-2.1082.noarch.rpm",
        null,
        true,
        false,
        listFileFactory(writeRpmListFile(rpmsDir(), NO_VERSION)),
        new RpmScanner(new SerialScanner())
    ).writeList();

    assertThat(rpmListFile, FileMatchers.containsOnly("is24-tomcat-mysql-jdbc-5.1.15-2.1082.noarch.rpm\n"));
  }

  @Test
  public void shouldReuseExistingPackageFile()
      throws Exception
  {
    final File rpmListFile = new RpmListWriter(
        rpmsDir(),
        null,
        null,
        true,
        false,
        listFileFactory(writeRpmListFile(rpmsDir(), NO_VERSION)),
        new RpmScanner(new SerialScanner())
    ).writeList();
    assertThat(rpmListFile, FileMatchers.containsOnly(osIndependent(FILE_CONTENT)));
  }

  @Test
  public void shouldCreateVersionSpecificRpmListFile()
      throws Exception
  {
    assertThat(
        writeRpmListFile(rpmsDir(), "2.2-2"),
        FileMatchers.containsOnly(osIndependent("conflict-artifact/2.2-2/conflict-artifact-2.2-2.noarch.rpm\n"))
    );
  }

  @Test
  public void shouldNotAddDuplicateToList()
      throws Exception
  {
    final File rpmListFile = new RpmListWriter(
        rpmsDir(),
        osIndependent("conflict-artifact/2.2-1/conflict-artifact-2.2-1.noarch.rpm"),
        null,
        true,
        false,
        listFileFactory(writeRpmListFile(rpmsDir(), NO_VERSION)),
        new RpmScanner(new SerialScanner())
    ).writeList();
    assertThat(rpmListFile, FileMatchers.containsOnly(osIndependent(FILE_CONTENT)));
  }

  @Test
  public void shouldAddMultipleFiles()
      throws Exception
  {
    // given written list file
    final File rpmListFile = new RpmListWriter(
        rpmsDir(),
        NEW_RPM1 + pathSeparator + NEW_RPM2,
        null,
        false,
        false,
        listFileFactory(writeRpmListFile(rpmsDir(), NO_VERSION)),
        new RpmScanner(new SerialScanner())
    ).writeList();

    assertThat(rpmListFile, FileMatchers.contains(NEW_RPM1, NEW_RPM2));
    assertThat(rpmListFile, not(FileMatchers.contains(pathSeparator)));
  }

  private File writeRpmListFile(final File rpmsDir, final String version)
      throws IOException
  {
    final File rpmListFile = new File(testIndex.getDirectory(), "package-list.txt");

    new RpmListWriter(
        rpmsDir,
        NO_ADDED_FILE,
        version,
        NO_SINGLE_RPM_PER_DIRECTORY,
        false,
        listFileFactory(rpmListFile),
        new RpmScanner(new SerialScanner())
    ).writeList();

    return rpmListFile;
  }

  private ListFileFactory listFileFactory(final File file) {
    return new ListFileFactory()
    {
      @Override
      public File getRpmListFile() {
        return file;
      }

      @Override
      public File getRpmListFile(String version) {
        return file;
      }
    };
  }

  private String osIndependent(final String fileContent) { return fileContent.replace("/", File.separator); }
}
