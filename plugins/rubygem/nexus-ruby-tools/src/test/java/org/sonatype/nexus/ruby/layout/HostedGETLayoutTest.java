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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sonatype.nexus.ruby.Directory;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.RubyScriptingTestSupport;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.SimpleStorage.BytesStreamLocation;
import org.sonatype.nexus.ruby.layout.SimpleStorage.URLGzipStreamLocation;
import org.sonatype.nexus.ruby.layout.SimpleStorage.URLStreamLocation;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@RunWith(Parameterized.class)
public class HostedGETLayoutTest
    extends RubyScriptingTestSupport
{
  private static File proxyBase() throws IOException {
    File base = new File("target/proxy");
    FileUtils.deleteDirectory(base);
    return base;
  }

  private static File hostedBase() throws IOException {
    File source = new File("src/test/hostedrepo");
    File base = new File("target/repo");
    FileUtils.deleteDirectory(base);
    FileUtils.copyDirectory(source, base, true);
    return base;
  }

  @Parameters
  public static Collection<Object[]> stores() throws IOException {
    return Arrays.asList(new Object[][]{
        {new SimpleStorage(hostedBase())},
        {
            new CachingProxyStorage(proxyBase(), hostedBase().toURI().toURL())
            {

              protected URLStreamLocation toUrl(RubygemsFile file) throws MalformedURLException {
                return new URLStreamLocation(new URL(baseurl + file.storagePath()));
              }
            }
        }
    });
  }

  private final DefaultRubygemsFileSystem bootstrap;

  public HostedGETLayoutTest(Storage store) {
    bootstrap = new DefaultRubygemsFileSystem(
        new HostedGETLayout(rubygemsGateway(),
            store),
        null, null);
  }

  @Test
  public void testSpecsZippedIndex() throws Exception {
    String[] pathes = {
        "/specs.4.8.gz",
        "/prerelease_specs.4.8.gz",
        "/latest_specs.4.8.gz"
    };
    assertFiletypeWithPayload(pathes, FileType.SPECS_INDEX_ZIPPED, URLStreamLocation.class);
  }

  @Test
  public void testSpecsUnzippedIndex() throws Exception {
    String[] pathes = {
        "/specs.4.8",
        "/prerelease_specs.4.8",
        "/latest_specs.4.8"
    };
    assertFiletypeWithPayload(pathes, FileType.SPECS_INDEX, URLGzipStreamLocation.class);
  }

  @Test
  public void testSha1() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.gem.sha1",
        "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.pom.sha1",
        "/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-123213123.gem.sha1",
        "/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-123213123.pom.sha1",
        "/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.gem.sha1",
        "/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.pom.sha1"
    };
    String[] shas = {
        "6fabc32da123f7013b2db804273df428a50bc6a4",
        "604b091a025d1234a529517822b5db66cbec9b13",
        "b7311d2f46398dbe40fd9643f3d4e5d473574335",
        "054121dcccc572cdee2da2d15e1ca712a1bb77b3",
        "b7311d2f46398dbe40fd9643f3d4e5d473574335",
        "a83efdc872c7b453196ec3911236f6e2dbd45c60"
    };

    assertFiletypeWithPayload(pathes, FileType.SHA1, shas);

    // these files carry a timestamp of creation of the .ruby file
    pathes = new String[]{
        "/maven/releases/rubygems/zip/maven-metadata.xml.sha1",
        "/maven/prereleases/rubygems/pre/maven-metadata.xml.sha1",
        "/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/maven-metadata.xml.sha1",
        "/maven/releases/rubygems/pre/maven-metadata.xml.sha1"
    };
    assertFiletypeWithPayload(pathes, FileType.SHA1, BytesStreamLocation.class);
  }

  @Test
  public void testGemArtifact() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.gem",
        "/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.gem",
        "/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-123213123.gem"
    };
    assertFiletypeWithPayload(pathes, FileType.GEM_ARTIFACT, URLStreamLocation.class);
    pathes = new String[]{
        "/maven/releases/rubygems/hufflepuf/0.1.0/hufflepuf-0.1.0.gem",
        "/maven/releases/rubygems/hufflepuf/0.1.0/hufflepuf-0.2.0.gem"
    };
    RubygemsFile[] result = assertFiletypeWithPayload(pathes, FileType.GEM_ARTIFACT, URLStreamLocation.class);
    for (RubygemsFile file : result) {
      GemArtifactFile a = (GemArtifactFile) file;
      assertThat(a.gem(null).filename(), is("hufflepuf-" + a.version() + "-universal-java-1.5"));
    }
  }

  @Test
  public void testPom() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.pom",
        "/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.pom",
        "/maven/releases/rubygems/my/0.1.0/my-0.1.0.pom",
        "/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-123213123.pom"
    };
    String[] xmls = {
        loadPomResource("zip.pom"),
        loadPomResource("pre.pom"),
        loadPomResource("my.pom"),
        loadPomResource("pre-snapshot.pom")
    };
    assertFiletypeWithPayload(pathes, FileType.POM, xmls);
  }

  @Test
  public void testMavenMetadata() throws Exception {
    String[] pathes = {
        "/maven/releases/rubygems/zip/maven-metadata.xml",
        "/maven/releases/rubygems/pre/maven-metadata.xml",
        "/maven/prereleases/rubygems/pre/maven-metadata.xml"
    };
    String[] xmls = {
        "<metadata>\n"
            + "  <groupId>rubygems</groupId>\n"
            + "  <artifactId>zip</artifactId>\n"
            + "  <versioning>\n"
            + "    <versions>\n"
            + "      <version>2.0.2</version>\n"
            + "    </versions>\n"
            + "    <lastUpdated>2014</lastUpdated>\n"
            + "  </versioning>\n"
            + "</metadata>\n",

        "<metadata>\n"
            + "  <groupId>rubygems</groupId>\n"
            + "  <artifactId>pre</artifactId>\n"
            + "  <versioning>\n"
            + "    <versions>\n"
            + "    </versions>\n"
            + "    <lastUpdated>2014</lastUpdated>\n"
            + "  </versioning>\n"
            + "</metadata>\n",

        "<metadata>\n"
            + "  <groupId>rubygems</groupId>\n"
            + "  <artifactId>pre</artifactId>\n"
            + "  <versioning>\n"
            + "    <versions>\n"
            + "      <version>0.1.0.beta-SNAPSHOT</version>\n"
            + "    </versions>\n"
            + "    <lastUpdated>2014</lastUpdated>\n"
            + "  </versioning>\n"
            + "</metadata>\n"
    };
    assertFiletypeWithPayload(pathes, FileType.MAVEN_METADATA, xmls);
  }

  @Test
  public void testMavenMetadataSnapshot() throws Exception {
    String[] pathes = {"/maven/prereleases/rubygems/pre/0.1.0-SNAPSHOT/maven-metadata.xml"};
    String[] xmls = {
        "<metadata>\n"
            + "  <groupId>rubygems</groupId>\n"
            + "  <artifactId>pre</artifactId>\n"
            + "  <versioning>\n"
            + "    <versions>\n"
            + "      <snapshot>\n"
            + "        <timestamp>2014</timestamp>\n"
            + "        <buildNumber>1</buildNumber>\n"
            + "      </snapshot>\n"
            + "      <lastUpdated>2014</lastUpdated>\n"
            + "      <snapshotVersions>\n"
            + "        <snapshotVersion>\n"
            + "          <extension>gem</extension>\n"
            + "          <value>0.1.0-2014-1</value>\n"
            + "          <updated>2014</updated>\n"
            + "        </snapshotVersion>\n"
            + "        <snapshotVersion>\n"
            + "          <extension>pom</extension>\n"
            + "          <value>0.1.0-2014-1</value>\n"
            + "          <updated>2014</updated>\n"
            + "        </snapshotVersion>\n"
            + "      </snapshotVersions>\n"
            + "    </versions>\n"
            + "  </versioning>\n"
            + "</metadata>\n"
    };
    assertFiletypeWithPayload(pathes, FileType.MAVEN_METADATA_SNAPSHOT, xmls);
  }

  @Test
  public void testBundlerApi() throws Exception {
    String[] pathes = {"/api/v1/dependencies?gems=zip,pre"};
    assertFiletypeWithPayload(pathes, FileType.BUNDLER_API, BytesStreamLocation.class);
  }


  @Test
  public void testApiV1Gems() throws Exception {
    String[] pathes = {"/api/v1/gems"};
    assertForbidden(pathes);
  }

  @Test
  public void testApiV1ApiKey() throws Exception {
    String[] pathes = {"/api/v1/api_key"};
    assertFiletypeWithNullPayload(pathes, FileType.API_V1);
  }


  @Test
  public void testDependency() throws Exception {
    String[] pathes = {
        "/api/v1/dependencies?gems=zip", "/api/v1/dependencies/pre.ruby", "/api/v1/dependencies/z/zip.ruby"
    };
    assertFiletypeWithPayload(pathes, FileType.DEPENDENCY, URLStreamLocation.class);
  }

  @Test
  public void testGemspec() throws Exception {
    String[] pathes = {"/quick/Marshal.4.8/zip-2.0.2.gemspec.rz", "/quick/Marshal.4.8/z/zip-2.0.2.gemspec.rz"};
    assertFiletypeWithPayload(pathes, FileType.GEMSPEC, URLStreamLocation.class);
  }

  @Test
  public void testGem() throws Exception {
    String[] pathes = {"/gems/pre-0.1.0.beta.gem", "/gems/p/pre-0.1.0.beta.gem"};
    assertFiletypeWithPayload(pathes, FileType.GEM, URLStreamLocation.class);
  }

  @Test
  public void testDirectory() throws Exception {
    String[] pathes = {
        "/", "/api", "/api/", "/api/v1", "/api/v1/",
        "/api/v1/dependencies/", "/gems/", "/gems",
        "/maven/releases/rubygems/zip",
        "/maven/releases/rubygems/zip/2.0.2",
        "/maven/prereleases/rubygems/pre",
        "/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT",
        "/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT",
    };
    assertFiletypeWithNullPayload(pathes, FileType.DIRECTORY);

    assertDirectory("/", "api/", "quick/", "gems/", "maven/", "specs.4.8", "latest_specs.4.8", "prerelease_specs.4.8",
        "specs.4.8.gz", "latest_specs.4.8.gz", "prerelease_specs.4.8.gz");
    assertDirectory("/api", "v1", "quick", "gems");
    assertDirectory("/api/v1", "api_key", "dependencies");
    assertDirectory("/api/v1/dependencies/");//"hufflepuf.ruby", "pre.ruby", "zip.ruby" );
    assertDirectory("/api/quick", "Marshal.4.8");
    assertDirectory("/api/quick/Marshal.4.8");
    assertDirectory("/api/gems");
    assertDirectory("/quick", "Marshal.4.8");
    assertDirectory("/quick/Marshal.4.8");
    assertDirectory("/gems");//"hufflepuf.ruby", "pre.ruby", "zip.ruby" );
    assertDirectory("/maven", "prereleases", "releases");
    assertDirectory("/maven/prereleases", "rubygems");
    // the lookup will create a hufflepuf.ruby !
    assertDirectory("/maven/prereleases/rubygems/hufflepuf", "maven-metadata.xml", "maven-metadata.xml.sha1");
    assertDirectory("/maven/prereleases/rubygems", "hufflepuf", "my", "pre", "zip");
    assertDirectory("/maven/releases", "rubygems");
    assertDirectory("/maven/releases/rubygems", "hufflepuf", "my", "pre", "zip");
    assertDirectory("/maven/releases/rubygems/hufflepuf", "0.1.0", "0.2.0", "maven-metadata.xml",
        "maven-metadata.xml.sha1");
    assertDirectory("/maven/releases/rubygems/jbundler", "maven-metadata.xml", "maven-metadata.xml.sha1");
    assertDirectory("/maven/releases/rubygems/pre", "0.1.0.beta", "maven-metadata.xml", "maven-metadata.xml.sha1");
    assertDirectory("/maven/releases/rubygems/zip", "2.0.2", "maven-metadata.xml", "maven-metadata.xml.sha1");
    assertDirectory("/maven/releases/rubygems/hufflepuf/0.1.0",
        "hufflepuf-0.1.0.pom", "hufflepuf-0.1.0.pom.sha1", "hufflepuf-0.1.0.gem", "hufflepuf-0.1.0.gem.sha1");
    assertDirectory("/maven/releases/rubygems/hufflepuf/0.2.0",
        "hufflepuf-0.2.0.pom", "hufflepuf-0.2.0.pom.sha1", "hufflepuf-0.2.0.gem", "hufflepuf-0.2.0.gem.sha1");
    assertDirectory("/maven/releases/rubygems/pre/0.1.0.beta",
        "pre-0.1.0.beta.pom", "pre-0.1.0.beta.pom.sha1", "pre-0.1.0.beta.gem", "pre-0.1.0.beta.gem.sha1");
    assertDirectory("/maven/releases/rubygems/zip/2.0.2",
        "zip-2.0.2.pom", "zip-2.0.2.pom.sha1", "zip-2.0.2.gem", "zip-2.0.2.gem.sha1");
  }

  private void assertDirectory(String path, String... items) {
    RubygemsFile file = bootstrap.get(path);
    assertThat(path, file.type(), equalTo(FileType.DIRECTORY));
    assertThat(path, file.get(), nullValue());
    assertThat(path, file.hasException(), is(false));
    assertThat(path, cleanupList(((Directory) file).getItems()), equalTo(cleanupList(items)));
  }

  protected List<String> cleanupList(String... items) {
    List<String> list = new LinkedList<>();
    for (String item : items) {
      if (!item.startsWith("gems=")) {
        list.add(item);
      }
    }
    // normalize to cope with file-system listing order issues.
    Collections.sort(list);
    return list;
  }

  @Test
  public void testNoContent() throws Exception {
    String[] pathes = {
        "/api/v1/dependencies", "/api/v1/dependencies?gems=",
    };
    assertFiletypeWithNullPayload(pathes, FileType.NO_CONTENT);
  }

  @Test
  public void testNotFound() throws Exception {
    String[] pathes = {
        "/asa", "/asa/", "/api/a", "/api/v1ds", "/api/v1/ds",
        "/api/v1/dependencies/jbundler.jruby", "/api/v1/dependencies/b/bundler.jrubyd",
        "/api/v1/dependencies/basd/bundler.ruby",
        "/quick/Marshal.4.8/jbundler.jssaon.rz", "/quick/Marshal.4.8/b/bundler.gemspec.rzd",
        "/quick/Marshal.4.8/basd/bundler.gemspec.rz",
        "/gems/jbundler.jssaonrz", "/gems/b/bundler.gemsa",
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
    assertFiletypeWithNullPayload(pathes, FileType.NOT_FOUND);
  }

  protected void assertFiletype(String[] pathes, FileType type) {
    for (String path : pathes) {
      RubygemsFile file = bootstrap.get(path);
      assertThat(path, file.type(), equalTo(type));
      assertThat(path, file.get(), notNullValue());
      assertThat(path, file.hasException(), is(false));
    }
  }

  protected void assertFiletypeWithPayload(String[] pathes, FileType type, String[] payloads) {
    int index = 0;
    for (String path : pathes) {
      RubygemsFile file = bootstrap.get(path);
      assertThat(path, file.type(), equalTo(type));
      assertThat(path, file.get(), is(instanceOf(BytesStreamLocation.class)));
      assertThat(path, file.hasException(), is(false));
      assertThat(path, readPayload(file).replaceAll("[0-9]{8}\\.?[0-9]{6}", "2014"), equalTo(payloads[index++]));
    }
  }

  protected String readPayload(RubygemsFile file) {
    try {
      ByteArrayInputStream b = (ByteArrayInputStream)((BytesStreamLocation) file.get()).openStream();
      byte[] bb = new byte[b.available()];
      b.read(bb);
      return new String(bb);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected RubygemsFile[] assertFiletypeWithPayload(String[] pathes, FileType type, Class<?> payload) {
    RubygemsFile[] result = new RubygemsFile[pathes.length];
    int index = 0;
    for (String path : pathes) {
      RubygemsFile file = bootstrap.get(path);
      assertThat(path, file.type(), equalTo(type));
      assertThat(path, file.get(), is(instanceOf(payload)));
      assertThat(path, file.hasException(), is(false));
      result[index++] = file;
    }
    return result;
  }

  protected void assertFiletypeWithNullPayload(String[] pathes, FileType type) {
    for (String path : pathes) {
      RubygemsFile file = bootstrap.get(path);
      assertThat(path, file.type(), equalTo(type));
      assertThat(path, file.get(), nullValue());
      assertThat(path, file.hasException(), is(false));
    }
  }

  protected void assertIOException(String[] pathes, FileType type) {
    for (String path : pathes) {
      RubygemsFile file = bootstrap.get(path);
      assertThat(path, file.type(), equalTo(type));
      assertThat(path, file.getException(), is(instanceOf(IOException.class)));
    }
  }

  protected void assertForbidden(String[] pathes) {
    for (String path : pathes) {
      assertThat(path, bootstrap.get(path).forbidden(), is(true));
    }
  }
}
