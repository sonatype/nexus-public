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
package org.sonatype.nexus.repository.r.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.junit.TestDataRule;
import org.sonatype.nexus.repository.r.internal.RException;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static java.lang.System.getProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.sonatype.nexus.repository.r.internal.util.RDescriptionUtils.extractDescriptionFromArchive;

public class RDescriptionUtilsTest
    extends TestSupport
{
  Path path = Paths.get(new File(getProperty("basedir", "")).getAbsolutePath(), "src/test/resources");

  @Rule
  public final TestDataRule testData = new TestDataRule(path.toFile());

  @Mock
  InputStream inputStream;

  @Test(expected = IllegalStateException.class)
  public void illegalStateExceptionWhenUnsupportedType() throws Exception {
    extractDescriptionFromArchive("Unsupported.unsuppored", inputStream);
  }

  @Test(expected = NullPointerException.class)
  public void nullPointerWhenNullFilename() throws Exception {
    extractDescriptionFromArchive(null, inputStream);
  }

  @Test(expected = NullPointerException.class)
  public void nullPointerWhenNullInputStream() throws Exception {
    extractDescriptionFromArchive("file", () -> null);
  }

  @Test
  public void extractDescriptionFromTar() throws Exception {
    Map<String, String> attributes = extractDescriptionFromArchive("r-package.tar.gz", loadFile("r-package.tar.gz"));
    assertAttributes(attributes);
  }

  @Test
  public void extractDescriptionFromTgz() throws Exception {
    Map<String, String> attributes = extractDescriptionFromArchive("r-package.tgz", loadFile("r-package.tar.gz"));
    assertAttributes(attributes);
  }

  @Test
  public void extractDescriptionFromZip() throws Exception {
    Map<String, String> attributes = extractDescriptionFromArchive("r-package.zip", loadFile("r-package.zip"));
    assertAttributes(attributes);
  }

  @Test(expected = RException.class)
  public void rExceptionOnCompressorException() throws Exception {
    extractDescriptionFromArchive("r-package.tar.gz", inputStream);
  }

  @Test(expected = IllegalStateException.class)
  public void illegalStateExceptionWhenNoMetadata() throws Exception {
    extractDescriptionFromArchive("r-package-no-description.zip", loadFile("r-package-no-description.zip"));
  }

  private InputStream loadFile(final String name) throws Exception {
    File tar = testData.resolveFile("org/sonatype/nexus/repository/r/internal/" + name);
    return new FileInputStream(tar);
  }

  private void assertAttributes(final Map<String, String> attributes) {
    assertThat(attributes.get("Package"), is(equalTo("RPostgreSQL")));
    assertThat(attributes.get("Version"), is(equalTo("0.4-1")));
    assertThat(attributes.get("Date"), is(equalTo("$Date: 2013-03-27 15:32:53 +0900 (Wed, 27 Mar 2013) $")));
    assertThat(attributes.get("Title"), is(equalTo("R interface to the PostgreSQL database system")));
    assertThat(attributes.get("Author"), is(equalTo("Joe Conway, Dirk Eddelbuettel, Tomoaki Nishiyama, Sameer Kumar\n" +
        "        Prayaga (during 2008), Neil Tiffin")));
    assertThat(attributes.get("Maintainer"), is(equalTo("Tomoaki Nishiyama <tomoakin@staff.kanazawa-u.ac.jp>")));
    assertThat(attributes.get("LazyLoad"), is(equalTo("true")));
    assertThat(attributes.get("Depends"), is(equalTo("R (>= 2.9.0), methods, DBI (>= 0.1-4)")));
    assertThat(attributes.get("License"), is(equalTo("GPL-2 | file LICENSE")));
    assertThat(attributes.get("Copyright"), is(equalTo("Authors listed above, PostgreSQL Global Development Group,\n" +
        "        and The Regents of the University of California")));
    assertThat(attributes.get("Collate"), is(equalTo("S4R.R zzz.R PostgreSQLSupport.R dbObjectId.R PostgreSQL.R")));
    assertThat(attributes.get("URL"), is(equalTo("https://code.google.com/p/rpostgresql/,\n" +
        "        http://www.stat.bell-labs.com/RS-DBI, http://www.postgresql.org")));
    assertThat(attributes.get("Packaged"), is(equalTo("2016-05-08 20:59:35 UTC; ligges")));
    assertThat(attributes.get("NeedsCompilation"), is(equalTo("yes")));
    assertThat(attributes.get("Repository"), is(equalTo("CRAN")));
    assertThat(attributes.get("Date/Publication"), is(equalTo("2016-05-08 23:09:20")));
    assertThat(attributes.get("Description"), containsString("Database interface and PostgreSQL driver for R This"));
  }
}
