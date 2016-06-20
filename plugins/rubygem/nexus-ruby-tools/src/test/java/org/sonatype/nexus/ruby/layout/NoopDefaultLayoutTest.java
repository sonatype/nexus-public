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
package org.sonatype.nexus.ruby.layout;

import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class NoopDefaultLayoutTest
    extends TestSupport
{
  private final DefaultRubygemsFileSystem bootstrap = new DefaultRubygemsFileSystem(
      new NoopDefaultLayout(null, null), null, null);

  @Test
  public void testSpecsZippedIndex() throws Exception {
    String[] pathes = {
        "/specs.4.8.gz",
        "/prerelease_specs.4.8.gz",
        "/latest_specs.4.8.gz"
    };
    assertFiletype(pathes, FileType.SPECS_INDEX_ZIPPED);
  }

  @Test
  public void testSpecsIndex() throws Exception {
    String[] pathes = {
        "/specs.4.8",
        "/prerelease_specs.4.8",
        "/latest_specs.4.8"
    };
    assertFiletype(pathes, FileType.SPECS_INDEX);
  }

  @Test
  public void testSha1() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/jbundler/maven-metadata.xml.sha1",
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.gem.sha1",
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.pom.sha1",
        "/maven/prereleases/rubygems/jbundler/maven-metadata.xml.sha1",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/maven-metadata.xml.sha1",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.gem.sha1",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.pom.sha1"
    };
    assertForbidden(pathes);
  }

  @Test
  public void testGemArtifact() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.gem",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.gem"
    };
    assertForbidden(pathes);
  }

  @Test
  public void testPom() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.pom",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.pom"
    };
    assertForbidden(pathes);
  }

  @Test
  public void testMavenMetadata() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/jbundler/maven-metadata.xml",
        "/maven/prereleases/rubygems/jbundler/maven-metadata.xml"
    };
    assertForbidden(pathes);
  }

  @Test
  public void testMavenMetadataSnapshot() throws Exception {
    String[] pathes = {"/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/maven-metadata.xml"};
    assertForbidden(pathes);
  }

  @Test
  public void testBundlerApi() throws Exception {
    String[] pathes = {"/api/v1/dependencies?gems=nexus,bundler"};
    assertForbidden(pathes);
  }

  @Test
  public void testApiV1() throws Exception {
    String[] pathes = {"/api/v1/gems", "/api/v1/api_key"};
    assertFiletype(pathes, FileType.API_V1);
  }

  @Test
  public void testDependency() throws Exception {
    String[] pathes = {
        "/api/v1/dependencies?gems=nexus", "/api/v1/dependencies/jbundler.ruby",
        "/api/v1/dependencies/b/bundler.ruby"
    };
    assertFiletype(pathes, FileType.DEPENDENCY);
  }

  @Test
  public void testGemspec() throws Exception {
    String[] pathes = {"/quick/Marshal.4.8/jbundler.gemspec.rz", "/quick/Marshal.4.8/b/bundler.gemspec.rz"};
    assertFiletype(pathes, FileType.GEMSPEC);
  }

  @Test
  public void testGem() throws Exception {
    String[] pathes = {"/gems/jbundler.gem", "/gems/b/bundler.gem"};
    assertFiletype(pathes, FileType.GEM);
  }

  @Test
  public void testDirectory() throws Exception {
    String[] pathes = {
        "/", "/api", "/api/", "/api/v1", "/api/v1/",
        "/api/v1/dependencies/", "/gems/", "/gems"
    };
    assertForbidden(pathes);
    String[] mpathes = {
        "/maven/releases/rubygems/jbundler",
        "/maven/releases/rubygems/jbundler/1.2.3",
        "/maven/prereleases/rubygems/jbundler",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT",
    };
    assertFiletype(mpathes, FileType.DIRECTORY);
  }

  @Test
  public void testNoContent() throws Exception {
    String[] pathes = {
        "/api/v1/dependencies", "/api/v1/dependencies?gems=",
    };
    assertFiletype(pathes, FileType.NO_CONTENT);
  }

  @Test
  public void testNotFound() throws Exception {
    String[] pathes = {
        "/asa", "/asa/", "/api/a", "/api/v1ds", "/api/v1/ds",
        "/api/v1/dependencies/jbundler.rubyz", "/api/v1/dependencies/b/bundler.rubyd",
        "/api/v1/dependencies/basd/bundler.ruby",
        "/quick/Marshal.4.8/jbundler.jrubyz", "/quick/Marshal.4.8/b/bundler.gemspec.rzd",
        "/quick/Marshal.4.8/basd/bundler.gemspec.rz",
        "/gems/jbundler.jrubyz", "/gems/b/bundler.gemsa",
        "/gems/basd/bundler.gem",
        "/maven/releasesss/rubygemsss/a",
        "/maven/releases/rubygemsss/jbundler",
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.gema",
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.pom2",
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.gem.sha",
        "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.pom.msa",
        "/maven/prereleases/rubygemsss/jbundler",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/maven-metadata.xml.sha1a",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.gem.sh1",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.pom.sha",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.gema",
        "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.pom2",
    };
    assertFiletype(pathes, FileType.NOT_FOUND);
  }

  protected void assertFiletype(String[] pathes, FileType type) {
    for (String path : pathes) {
      assertThat(path, bootstrap.get(path).type(), equalTo(type));
    }
  }

  protected void assertForbidden(String[] pathes) {
    for (String path : pathes) {
      assertThat(path, bootstrap.get(path).forbidden(), is(true));
    }
  }
}
