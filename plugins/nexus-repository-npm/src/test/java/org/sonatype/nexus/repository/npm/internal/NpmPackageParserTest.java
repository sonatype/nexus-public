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
package org.sonatype.nexus.repository.npm.internal;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.Test;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NpmPackageParserTest
    extends TestSupport
{
  private NpmPackageParser underTest = new NpmPackageParser();

  @Test
  public void testParsePackageJson() {
    Map<String, Object> results = underTest.parsePackageJson(() -> getClass().getResourceAsStream("package.tgz"));
    assertThat(results, is(not(nullValue())));
    assertThat(results.isEmpty(), is(false));
    assertThat(results.size(), is(9));
  }

  @Test
  public void testParseMissingPackageJson() {
    Map<String, Object> results = underTest.parsePackageJson(() -> getClass().getResourceAsStream("package-without-json.tgz"));
    assertThat(results, is(not(nullValue())));
    assertThat(results.isEmpty(), is(true));
    assertThat(results.size(), is(0));
  }

  @Test
  public void testRecoveryFromExceptionalSituation() {
    Map<String, Object> results = underTest.parsePackageJson(() -> {
      throw new RuntimeException("test");
    });
    assertThat(results, is(not(nullValue())));
    assertThat(results.isEmpty(), is(true));
    assertThat(results.size(), is(0));
  }

  @Test
  public void testPackageJsonPathDeterminations() {
    // package.json should always be under the main path in the tarball, whatever that is for a particular archive
    assertThat(underTest.isPackageJson(makeEntry("package/package.json", false)), is(true));
    assertThat(underTest.isPackageJson(makeEntry("foo/package.json", false)), is(true));

    // other paths we should just skip over or ignore, just in case someone put a "package.json" somewhere else
    assertThat(underTest.isPackageJson(makeEntry("package.json", false)), is(false));
    assertThat(underTest.isPackageJson(makeEntry("/package.json", false)), is(false));
    assertThat(underTest.isPackageJson(makeEntry("/foo/bar/package.json", false)), is(false));
    assertThat(underTest.isPackageJson(makeEntry("/foo/bar/baz/package.json", false)), is(false));
    assertThat(underTest.isPackageJson(makeEntry("package/package.json", true)), is(false));
  }

  private ArchiveEntry makeEntry(final String name, final boolean directory) {
    ArchiveEntry entry = mock(ArchiveEntry.class);
    when(entry.getName()).thenReturn(name);
    when(entry.isDirectory()).thenReturn(directory);
    return entry;
  }
}
