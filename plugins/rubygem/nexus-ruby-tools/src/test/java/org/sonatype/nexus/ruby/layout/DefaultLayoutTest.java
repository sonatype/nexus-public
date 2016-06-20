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

import org.sonatype.nexus.ruby.BundlerApiFile;
import org.sonatype.nexus.ruby.DependencyData;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.GemFile;
import org.sonatype.nexus.ruby.GemspecFile;
import org.sonatype.nexus.ruby.MavenMetadataFile;
import org.sonatype.nexus.ruby.MavenMetadataSnapshotFile;
import org.sonatype.nexus.ruby.PomFile;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.SpecsIndexFile;
import org.sonatype.nexus.ruby.SpecsIndexType;
import org.sonatype.nexus.ruby.SpecsIndexZippedFile;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class DefaultLayoutTest
    extends TestSupport
{
  static class DependenciesMock
      implements DependencyData
  {
    private final String platform;

    DependenciesMock() {
      this("ruby");
    }

    DependenciesMock(String platform) {
      this.platform = platform;
    }

    @Override
    public String[] versions(boolean prereleased) {
      return null;
    }

    @Override
    public String platform(String version) {
      return platform;
    }

    @Override
    public String name() {
      return null;
    }

    @Override
    public long modified() {
      return 0;
    }
  }

  private RubygemsFileSystem fileSystem;

  private DefaultLayout layout;

  @Before
  public void setUp() throws Exception {
    layout = new DefaultLayout();
    fileSystem = new DefaultRubygemsFileSystem(layout, null, null);
  }

  @Test
  public void testGemArtifact() throws Exception {
    String path = "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.gem";
    RubygemsFile file = fileSystem.get(path);
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get(path);
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo(path));
    assertThat(file.remotePath(), equalTo(path));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.GEM_ARTIFACT));

    GemArtifactFile file3 = (GemArtifactFile) file;

    DependencyData deps = new DependenciesMock();
    assertThat(file3.version(), equalTo("1.2.3"));
    assertThat(file3.gem(deps).name(), equalTo("jbundler"));
    assertThat(file3.gem(deps).version(), equalTo("1.2.3"));
    assertThat(file3.isSnapshot(), equalTo(false));
  }

  @Test
  public void testGemSnapshotArtifact() throws Exception {
    String path = "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123123123.gem";
    RubygemsFile file = fileSystem.get(path);
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get(path);
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo(path));
    assertThat(file.remotePath(), equalTo(path));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.GEM_ARTIFACT));

    GemArtifactFile file3 = (GemArtifactFile) file;

    DependencyData deps = new DependenciesMock();
    assertThat(file3.version(), equalTo("1.2.3"));
    assertThat(file3.gem(deps).name(), equalTo("jbundler"));
    assertThat(file3.gem(deps).version(), equalTo("1.2.3"));
    assertThat(file3.isSnapshot(), equalTo(true));
  }

  @Test
  public void testPomRelease() throws Exception {
    String path = "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.pom";
    RubygemsFile file = fileSystem.get(path);
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get(path);
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo(path));
    assertThat(file.remotePath(), equalTo(path));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.POM));

    PomFile file3 = (PomFile) file;

    DependencyData deps = new DependenciesMock();
    assertThat(file3.version(), equalTo("1.2.3"));
    assertThat(file3.gemspec(deps).name(), equalTo("jbundler"));
    assertThat(file3.gemspec(deps).version(), equalTo("1.2.3"));
    assertThat(file3.gemspec(deps).platform(), equalTo("ruby"));
    assertThat(file3.isSnapshot(), equalTo(false));
  }

  @Test
  public void testPomSnapshot() throws Exception {
    String path = "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123123123.pom";
    RubygemsFile file = fileSystem.get(path);
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get(path);
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo(path));
    assertThat(file.remotePath(), equalTo(path));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.POM));

    PomFile file3 = (PomFile) file;

    DependencyData deps = new DependenciesMock();
    assertThat(file3.version(), equalTo("1.2.3"));
    assertThat(file3.gemspec(deps).filename(), equalTo("jbundler-1.2.3"));
    assertThat(file3.gemspec(deps).version(), equalTo("1.2.3"));
    assertThat(file3.isSnapshot(), equalTo(true));
  }

  @Test
  public void testMetadataXmlReleases() throws Exception {
    String path = "/maven/releases/rubygems/jbundler/maven-metadata.xml";
    RubygemsFile file = fileSystem.get(path);
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get(path);
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo(path));
    assertThat(file.remotePath(), equalTo(path));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.MAVEN_METADATA));

    MavenMetadataFile file3 = (MavenMetadataFile) file;

    assertThat(file3.dependency().name(), equalTo("jbundler"));
    assertThat(file3.isPrerelease(), equalTo(false));
  }

  @Test
  public void testMetadataXmlPrereleases() throws Exception {
    String path = "/maven/prereleases/rubygems/jbundler/maven-metadata.xml";
    RubygemsFile file = fileSystem.get(path);
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get(path);
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo(path));
    assertThat(file.remotePath(), equalTo(path));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.MAVEN_METADATA));

    MavenMetadataFile file3 = (MavenMetadataFile) file;

    assertThat(file3.dependency().name(), equalTo("jbundler"));
    assertThat(file3.isPrerelease(), equalTo(true));
  }

  @Test
  public void testMetadataXmlSnapshots() throws Exception {
    String path = "/maven/prereleases/rubygems/jbundler/9.2.3-SNAPSHOT/maven-metadata.xml";
    RubygemsFile file = fileSystem.get(path);
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get(path);
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo(path));
    assertThat(file.remotePath(), equalTo(path));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.MAVEN_METADATA_SNAPSHOT));

    MavenMetadataSnapshotFile file3 = (MavenMetadataSnapshotFile) file;

    assertThat(file3.version(), equalTo("9.2.3"));
  }

  @Test
  public void testGemfile() throws Exception {
    RubygemsFile file = fileSystem.get("/gems/jbundler-9.2.1.gem");
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get("/gems/j/jbundler-9.2.1.gem");
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo("/gems/j/jbundler-9.2.1.gem"));
    assertThat(file.remotePath(), equalTo("/gems/jbundler-9.2.1.gem"));
    assertThat(file.name(), equalTo("jbundler-9.2.1"));
    assertThat(file.type(), equalTo(FileType.GEM));

    GemFile file3 = (GemFile) file;

    assertThat((GemFile) file, equalTo(file3.gemspec().gem()));
    assertThat(file3.version(), equalTo(null));
    assertThat(file3.platform(), equalTo(null));
    assertThat(file3.filename(), equalTo("jbundler-9.2.1"));
    assertThat(file3.gemspec().name(), equalTo("jbundler-9.2.1"));
  }

  @Test
  public void testGemfile2() throws Exception {
    RubygemsFile file = layout.gemFile("jbundler", "9.2.1", "java");
    assertThat(file, notNullValue());
    RubygemsFile file2 = layout.gemFile("jbundler", "9.2.1", "java");
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo("/gems/j/jbundler-9.2.1-java.gem"));
    assertThat(file.remotePath(), equalTo("/gems/jbundler-9.2.1-java.gem"));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.GEM));

    GemFile file3 = (GemFile) file;

    assertThat((GemFile) file, equalTo(file3.gemspec().gem()));
    assertThat(file3.filename(), equalTo("jbundler-9.2.1-java"));
    assertThat(file3.name(), equalTo("jbundler"));
    assertThat(file3.version(), equalTo("9.2.1"));
    assertThat(file3.platform(), equalTo("java"));
    assertThat(file3.gemspec().name(), equalTo("jbundler"));
  }

  @Test
  public void testGemspecfile() throws Exception {
    RubygemsFile file = fileSystem.get("/quick/Marshal.4.8/jbundler-9.2.1.gemspec.rz");
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get("/quick/Marshal.4.8/j/jbundler-9.2.1.gemspec.rz");
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo("/quick/Marshal.4.8/j/jbundler-9.2.1.gemspec.rz"));
    assertThat(file.remotePath(), equalTo("/quick/Marshal.4.8/jbundler-9.2.1.gemspec.rz"));
    assertThat(file.name(), equalTo("jbundler-9.2.1"));
    assertThat(file.type(), equalTo(FileType.GEMSPEC));

    GemspecFile file3 = (GemspecFile) file;

    assertThat((GemspecFile) file, equalTo(file3.gem().gemspec()));
    assertThat(file3.version(), equalTo(null));
    assertThat(file3.platform(), equalTo(null));
    assertThat(file3.filename(), equalTo("jbundler-9.2.1"));
    assertThat(file3.gem().name(), equalTo("jbundler-9.2.1"));
  }

  @Test
  public void testGemspecfile2() throws Exception {
    RubygemsFile file = layout.gemspecFile("jbundler", "9.2.1", "java");
    assertThat(file, notNullValue());
    RubygemsFile file2 = layout.gemspecFile("jbundler", "9.2.1", "java");
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo("/quick/Marshal.4.8/j/jbundler-9.2.1-java.gemspec.rz"));
    assertThat(file.remotePath(), equalTo("/quick/Marshal.4.8/jbundler-9.2.1-java.gemspec.rz"));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.GEMSPEC));

    GemspecFile file3 = (GemspecFile) file;

    assertThat((GemspecFile) file, equalTo(file3.gem().gemspec()));
    assertThat(file3.filename(), equalTo("jbundler-9.2.1-java"));
    assertThat(file3.name(), equalTo("jbundler"));
    assertThat(file3.version(), equalTo("9.2.1"));
    assertThat(file3.platform(), equalTo("java"));
    assertThat(file3.gem().name(), equalTo("jbundler"));
  }

  @Test
  public void testDependencyFile() throws Exception {
    RubygemsFile file = fileSystem.get("/api/v1/dependencies/jbundler.ruby");
    assertThat(file, notNullValue());
    RubygemsFile file2 = fileSystem.get("/api/v1/dependencies?gems=jbundler");
    assertThat(file, equalTo(file2));
    file2 = fileSystem.get("/api/v1/dependencies/j/jbundler.ruby");
    assertThat(file, equalTo(file2));
    assertThat(file.storagePath(), equalTo("/api/v1/dependencies/jbundler.ruby"));
    assertThat(file.remotePath(), equalTo("/api/v1/dependencies?gems=jbundler"));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.DEPENDENCY));
  }

  @Test
  public void testBundlerApiFile1() throws Exception {
    RubygemsFile file = fileSystem.get("/api/v1/dependencies?gems=jbundler");
    assertThat(file, notNullValue());
    assertThat(file.storagePath(), equalTo("/api/v1/dependencies/jbundler.ruby"));
    assertThat(file.remotePath(), equalTo("/api/v1/dependencies?gems=jbundler"));
    assertThat(file.name(), equalTo("jbundler"));
    assertThat(file.type(), equalTo(FileType.DEPENDENCY));
  }

  @Test
  public void testBundlerApiFile2() throws Exception {
    RubygemsFile file = fileSystem.get("/api/v1/dependencies?gems=jbundler,bundler");
    assertThat(file, notNullValue());
    assertThat(file.storagePath(), equalTo("/api/v1/dependencies/8d44e63a6cf933622c5f41a73371a1945e17a2db.gems"));
    assertThat(file.name(), nullValue());
    assertThat(file.remotePath(), equalTo("/api/v1/dependencies?gems=jbundler,bundler"));
    assertThat(((BundlerApiFile) file).gemnames(), equalTo(new String[]{"jbundler", "bundler"}));
    assertThat(file.type(), equalTo(FileType.BUNDLER_API));
  }

  @Test
  public void testBundlerApiFile3() throws Exception {
    RubygemsFile file = fileSystem.get("/api/v1/dependencies?gems=jbundler%2Cbundler");
    assertThat(file, notNullValue());
    assertThat(file.storagePath(), equalTo("/api/v1/dependencies/8d44e63a6cf933622c5f41a73371a1945e17a2db.gems"));
    assertThat(file.name(), nullValue());
    assertThat(file.remotePath(), equalTo("/api/v1/dependencies?gems=jbundler,bundler"));
    assertThat(((BundlerApiFile) file).gemnames(), equalTo(new String[]{"jbundler", "bundler"}));
    assertThat(file.type(), equalTo(FileType.BUNDLER_API));
  }

  @Test
  public void testSpecsIndexFile() throws Exception {
    RubygemsFile file = fileSystem.get("/specs.4.8.gz");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.SPECS_INDEX_ZIPPED));
    RubygemsFile file2 = fileSystem.get("/specs.4.8");
    assertThat(file2.type(), equalTo(FileType.SPECS_INDEX));

    assertThat((SpecsIndexZippedFile) file,
        equalTo(((SpecsIndexFile) file2).zippedSpecsIndexFile()));
    assertThat((SpecsIndexFile) file2,
        equalTo(((SpecsIndexZippedFile) file).unzippedSpecsIndexFile()));

    assertThat(file.storagePath(), equalTo("/specs.4.8.gz"));
    assertThat(file.remotePath(), equalTo("/specs.4.8.gz"));
    assertThat(file2.storagePath(), equalTo("/specs.4.8"));
    assertThat(file2.remotePath(), equalTo("/specs.4.8"));
    assertThat(file.name(), equalTo("specs"));

    SpecsIndexZippedFile file3 = (SpecsIndexZippedFile) file;
    assertThat(file3.specsType(), equalTo(SpecsIndexType.RELEASE));
    assertThat(file3.storagePath(), equalTo(SpecsIndexType.RELEASE.filepathGzipped()));

    SpecsIndexFile file4 = (SpecsIndexFile) file2;
    assertThat(file4.specsType(), equalTo(SpecsIndexType.RELEASE));
    assertThat(file4.storagePath(), equalTo(SpecsIndexType.RELEASE.filepath()));
  }

  @Test
  public void testLatestSpecsIndexFile() throws Exception {
    RubygemsFile file = fileSystem.get("/latest_specs.4.8.gz");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.SPECS_INDEX_ZIPPED));
    RubygemsFile file2 = fileSystem.get("/latest_specs.4.8");
    assertThat(file2.type(), equalTo(FileType.SPECS_INDEX));

    assertThat((SpecsIndexZippedFile) file,
        equalTo(((SpecsIndexFile) file2).zippedSpecsIndexFile()));
    assertThat((SpecsIndexFile) file2,
        equalTo(((SpecsIndexZippedFile) file).unzippedSpecsIndexFile()));

    assertThat(file.storagePath(), equalTo("/latest_specs.4.8.gz"));
    assertThat(file.remotePath(), equalTo("/latest_specs.4.8.gz"));
    assertThat(file2.storagePath(), equalTo("/latest_specs.4.8"));
    assertThat(file2.remotePath(), equalTo("/latest_specs.4.8"));
    assertThat(file.name(), equalTo("latest_specs"));

    SpecsIndexZippedFile file3 = (SpecsIndexZippedFile) file;
    assertThat(file3.specsType(), equalTo(SpecsIndexType.LATEST));
    assertThat(file3.storagePath(), equalTo(SpecsIndexType.LATEST.filepathGzipped()));

    SpecsIndexFile file4 = (SpecsIndexFile) file2;
    assertThat(file4.specsType(), equalTo(SpecsIndexType.LATEST));
    assertThat(file4.storagePath(), equalTo(SpecsIndexType.LATEST.filepath()));
  }

  @Test
  public void testPrereleasedSpecsIndexFile() throws Exception {
    RubygemsFile file = fileSystem.get("/prerelease_specs.4.8.gz");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.SPECS_INDEX_ZIPPED));
    RubygemsFile file2 = fileSystem.get("/prerelease_specs.4.8");
    assertThat(file2.type(), equalTo(FileType.SPECS_INDEX));

    assertThat((SpecsIndexZippedFile) file,
        equalTo(((SpecsIndexFile) file2).zippedSpecsIndexFile()));
    assertThat((SpecsIndexFile) file2,
        equalTo(((SpecsIndexZippedFile) file).unzippedSpecsIndexFile()));

    assertThat(file.storagePath(), equalTo("/prerelease_specs.4.8.gz"));
    assertThat(file.remotePath(), equalTo("/prerelease_specs.4.8.gz"));
    assertThat(file2.storagePath(), equalTo("/prerelease_specs.4.8"));
    assertThat(file2.remotePath(), equalTo("/prerelease_specs.4.8"));
    assertThat(file.name(), equalTo("prerelease_specs"));

    SpecsIndexZippedFile file3 = (SpecsIndexZippedFile) file;
    assertThat(file3.specsType(), equalTo(SpecsIndexType.PRERELEASE));
    assertThat(file3.storagePath(), equalTo(SpecsIndexType.PRERELEASE.filepathGzipped()));

    SpecsIndexFile file4 = (SpecsIndexFile) file2;
    assertThat(file4.specsType(), equalTo(SpecsIndexType.PRERELEASE));
    assertThat(file4.storagePath(), equalTo(SpecsIndexType.PRERELEASE.filepath()));
  }

  @Test
  public void testNotFile() throws Exception {
    RubygemsFile file = fileSystem.get("/prereleased_specs.4.8.gz");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.NOT_FOUND));
    file = fileSystem.get("/pre_specs.4.8");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.NOT_FOUND));
    file = fileSystem.get("/gems/something.json");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.NOT_FOUND));
    file = fileSystem.get("/something/index.html");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.NOT_FOUND));
  }

  @Test
  public void testDirectory() throws Exception {
    RubygemsFile file = fileSystem.get("/api");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    RubygemsFile file2 = fileSystem.get("/api/");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/api/v1");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("/api/v1/");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/api/v1/dependencies/");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));

    file = fileSystem.get("/api/v1/dependencies/a/");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("/api/v1/dependencies/a");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/gems");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("/gems/");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/gems/v");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("/gems/v/");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/quick");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("/quick/");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/quick/Marshal.4.8");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("/quick/Marshal.4.8/");
    assertThat(file, equalTo(file2));

    file = fileSystem.get("/quick/Marshal.4.8/-");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.DIRECTORY));
    file2 = fileSystem.get("/quick/Marshal.4.8/-/");
    assertThat(file, equalTo(file2));
  }


  @Test
  public void testNoContent() throws Exception {
    RubygemsFile file = fileSystem.get("/api/v1/dependencies");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.NO_CONTENT));
    assertThat(file.get(), nullValue());

    file = fileSystem.get("/api/v1/dependencies?gems=");
    assertThat(file, notNullValue());
    assertThat(file.type(), equalTo(FileType.NO_CONTENT));
    assertThat(file.get(), nullValue());
  }
}
