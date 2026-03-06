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
package org.sonatype.nexus.repository.maven.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * UT for {@link MavenPath}
 *
 * @since 3.0
 */
public class MavenPathTest
    extends TestSupport
{
  private final Maven2MavenPathParser pathParser = new Maven2MavenPathParser();

  @Test
  public void pathWithBackslashes() {
    final String path = "\\commons-collections\\commons-collections\\2.0\\commons-collections-2.0.pom";
    String normalizedPath = "/commons-collections/commons-collections/2.0/commons-collections-2.0.pom";
    final MavenPath mavenPath = pathParser.parsePath(path);
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(normalizedPath.substring(1)));
    assertThat(mavenPath.getFileName(), equalTo("commons-collections-2.0.pom"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("commons-collections"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("commons-collections"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("2.0"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo(mavenPath.getCoordinates().getVersion()));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));

    assertThat(mavenPath.isSubordinate(), is(false));
    MavenPath mavenPathSha1 = mavenPath.hash(HashType.SHA1);
    assertThat(mavenPathSha1.isSubordinate(), is(true));
    assertThat(mavenPathSha1.subordinateOf(), equalTo(mavenPath));
    assertThat(mavenPathSha1.main(), equalTo(mavenPath));

    MavenPath coordinatesAscSha1 = mavenPath.signature(SignatureType.GPG).hash(HashType.SHA1);
    assertThat(coordinatesAscSha1.getPath(), equalTo(normalizedPath.substring(1) + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getFileName(), equalTo(mavenPath.getFileName() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getCoordinates().getExtension(),
        equalTo(mavenPath.getCoordinates().getExtension() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.isSubordinate(), is(true));
    assertThat(coordinatesAscSha1.subordinateOf().subordinateOf(), equalTo(mavenPath));
    assertThat(coordinatesAscSha1.main(), equalTo(mavenPath));

    MavenPath coordinates2 = coordinatesAscSha1.subordinateOf().subordinateOf();
    assertThat(coordinates2.getPath(), equalTo(normalizedPath.substring(1)));
    assertThat(coordinates2.getFileName(), equalTo(mavenPath.getFileName()));
    assertThat(coordinates2.getCoordinates().getExtension(), equalTo(mavenPath.getCoordinates().getExtension()));
    assertThat(coordinates2.isSubordinate(), is(false));
  }

  @Test
  public void pom() {
    final String path = "/org/eclipse/jetty/jetty-io/8.1.16.v20140903/jetty-io-8.1.16.v20140903.pom";
    final MavenPath mavenPath = pathParser.parsePath(path);
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(path.substring(1)));
    assertThat(mavenPath.getFileName(), equalTo("jetty-io-8.1.16.v20140903.pom"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.eclipse.jetty"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jetty-io"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("8.1.16.v20140903"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo(mavenPath.getCoordinates().getVersion()));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));

    assertThat(mavenPath.isSubordinate(), is(false));
    MavenPath mavenPathSha1 = mavenPath.hash(HashType.SHA1);
    assertThat(mavenPathSha1.isSubordinate(), is(true));
    assertThat(mavenPathSha1.subordinateOf(), equalTo(mavenPath));
    assertThat(mavenPathSha1.main(), equalTo(mavenPath));

    MavenPath coordinatesAscSha1 = mavenPath.signature(SignatureType.GPG).hash(HashType.SHA1);
    assertThat(coordinatesAscSha1.getPath(), equalTo(path.substring(1) + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getFileName(), equalTo(mavenPath.getFileName() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getCoordinates().getExtension(),
        equalTo(mavenPath.getCoordinates().getExtension() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.isSubordinate(), is(true));
    assertThat(coordinatesAscSha1.subordinateOf().subordinateOf(), equalTo(mavenPath));
    assertThat(coordinatesAscSha1.main(), equalTo(mavenPath));

    MavenPath coordinates2 = coordinatesAscSha1.subordinateOf().subordinateOf();
    assertThat(coordinates2.getPath(), equalTo(path.substring(1)));
    assertThat(coordinates2.getFileName(), equalTo(mavenPath.getFileName()));
    assertThat(coordinates2.getCoordinates().getExtension(), equalTo(mavenPath.getCoordinates().getExtension()));
    assertThat(coordinates2.isSubordinate(), is(false));
  }

  @Test
  public void pomSnapshot() {
    final String path = "/org/eclipse/jetty/jetty-io/8.1.16-SNAPSHOT/jetty-io-8.1.16-20140903.180000-1.pom";
    final MavenPath mavenPath = pathParser.parsePath(path);
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(path.substring(1)));
    assertThat(mavenPath.getFileName(), equalTo("jetty-io-8.1.16-20140903.180000-1.pom"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.eclipse.jetty"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jetty-io"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("8.1.16-20140903.180000-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("8.1.16-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));

    assertThat(mavenPath.isSubordinate(), is(false));
    MavenPath pathSha1 = mavenPath.hash(HashType.SHA1);
    assertThat(pathSha1.isSubordinate(), is(true));
    assertThat(pathSha1.subordinateOf(), equalTo(mavenPath));
    assertThat(pathSha1.main(), equalTo(mavenPath));

    MavenPath coordinatesAscSha1 = mavenPath.signature(SignatureType.GPG).hash(HashType.SHA1);
    assertThat(coordinatesAscSha1.getPath(), equalTo(path.substring(1) + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getFileName(), equalTo(mavenPath.getFileName() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getCoordinates().getExtension(),
        equalTo(mavenPath.getCoordinates().getExtension() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.isSubordinate(), is(true));
    assertThat(coordinatesAscSha1.subordinateOf().subordinateOf(), equalTo(mavenPath));
    assertThat(coordinatesAscSha1.main(), equalTo(mavenPath));

    MavenPath coordinates2 = coordinatesAscSha1.subordinateOf().subordinateOf();
    assertThat(coordinates2.getPath(), equalTo(path.substring(1)));
    assertThat(coordinates2.getFileName(), equalTo(mavenPath.getFileName()));
    assertThat(coordinates2.getCoordinates().getExtension(), equalTo(mavenPath.getCoordinates().getExtension()));
    assertThat(coordinates2.isSubordinate(), is(false));
  }

  @Test
  public void jarSnapshotHash() {
    final String path =
        "/org/apache/maven/maven-repository-metadata/3.3.0-SNAPSHOT/maven-repository-metadata-3.3.0-20150311.160242-1.jar.sha1";
    final MavenPath mavenPath = pathParser.parsePath(path);
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(path.substring(1)));
    assertThat(mavenPath.getFileName(),
        equalTo("maven-repository-metadata-3.3.0-20150311.160242-1.jar.sha1"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.SHA1));
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.apache.maven"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("maven-repository-metadata"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.3.0-20150311.160242-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.sha1"));

    assertThat(mavenPath.isSubordinate(), is(true));
    MavenPath main = mavenPath.main();
    assertThat(main.isSubordinate(), is(false));
    assertThat(mavenPath.subordinateOf(), equalTo(main));
    assertThat(mavenPath.main(), equalTo(main));

    MavenPath coordinatesAscSha1 = mavenPath.main().signature(SignatureType.GPG).hash(HashType.SHA1);
    assertThat(coordinatesAscSha1.getPath(), equalTo(mavenPath.main().getPath() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getFileName(), equalTo(mavenPath.main().getFileName() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.getCoordinates().getExtension(),
        equalTo(mavenPath.main().getCoordinates().getExtension() + ".asc.sha1"));
    assertThat(coordinatesAscSha1.isSubordinate(), is(true));
    assertThat(coordinatesAscSha1.subordinateOf().subordinateOf(), equalTo(mavenPath.subordinateOf()));
    assertThat(coordinatesAscSha1.main(), equalTo(mavenPath.main()));
  }

  @Test
  public void locatePom() {
    final String path =
        "/org/apache/maven/maven-repository-metadata/3.3.0-SNAPSHOT/maven-repository-metadata-3.3.0-20150311.160242-1.jar.sha1";
    final MavenPath mavenPath = pathParser.parsePath(path).locatePom();
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(
        "org/apache/maven/maven-repository-metadata/3.3.0-SNAPSHOT/maven-repository-metadata-3.3.0-20150311.160242-1.pom"));
    assertThat(mavenPath.getFileName(),
        equalTo("maven-repository-metadata-3.3.0-20150311.160242-1.pom"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.apache.maven"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("maven-repository-metadata"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.3.0-20150311.160242-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));
    assertThat(mavenPath.isSubordinate(), is(false));
  }

  @Test
  public void mavenPathShouldProperlyStripClassifier() {
    final String path = "/some/library/0.1/library-0.1-sources.jar";
    final MavenPath mavenPath = pathParser.parsePath(path);

    assertThat(mavenPath.locatePom().getPath(), equalTo("some/library/0.1/library-0.1.pom"));
  }

  @Test
  public void locateJar() {
    final String path =
        "/org/apache/maven/maven-repository-metadata/3.3.0-SNAPSHOT/maven-repository-metadata-3.3.0-20150311.160242-1.jar.sha1";
    final MavenPath mavenPath = pathParser.parsePath(path).locateMainArtifact("jar");
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(
        "org/apache/maven/maven-repository-metadata/3.3.0-SNAPSHOT/maven-repository-metadata-3.3.0-20150311.160242-1.jar"));
    assertThat(mavenPath.getFileName(),
        equalTo("maven-repository-metadata-3.3.0-20150311.160242-1.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.apache.maven"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("maven-repository-metadata"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.3.0-20150311.160242-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.isSubordinate(), is(false));
  }

  @Test
  public void locateJavadoc() {
    final String path =
        "/org/apache/maven/maven-repository-metadata/3.3.0-SNAPSHOT/maven-repository-metadata-3.3.0-20150311.160242-1.jar.sha1";
    final MavenPath mavenPath = pathParser.parsePath(path).locate("jar", "javadoc");
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(
        "org/apache/maven/maven-repository-metadata/3.3.0-SNAPSHOT/maven-repository-metadata-3.3.0-20150311.160242-1-javadoc.jar"));
    assertThat(mavenPath.getFileName(),
        equalTo("maven-repository-metadata-3.3.0-20150311.160242-1-javadoc.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.apache.maven"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("maven-repository-metadata"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.3.0-20150311.160242-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.isSubordinate(), is(false));
  }

  @Test
  public void testMavenPath_canFixPathItselfAndNotBeLazyAndRequireEveryoneElseToFixItForHim() {
    String path = "\\org\\some\\group\\id\\artifactId\\1.0.0\\artifactId-1.0.0.jar";
    MavenPath mavenPath = new MavenPath(path, null);
    assertThat(mavenPath.getPath(), is("org/some/group/id/artifactId/1.0.0/artifactId-1.0.0.jar"));
  }

  /**
   * Tests parsing and unwrapping of signature files for hash files (e.g., *.jar.md5.asc).
   * This is a regression test for NEXUS-50362 where such paths caused a StringIndexOutOfBoundsException.
   */
  @Test
  public void signatureOfHashFile() {
    // Test case: signature of an md5 hash file
    final String path = "/com/itextpdf/itext-xtra/5.5.13.2/itext-xtra-5.5.13.2-javadoc.jar.md5.asc";
    final MavenPath mavenPath = pathParser.parsePath(path);
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getPath(), equalTo(path.substring(1)));
    assertThat(mavenPath.getFileName(), equalTo("itext-xtra-5.5.13.2-javadoc.jar.md5.asc"));
    // The file ends with .asc, so hashType should be null (based on filename ending)
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("com.itextpdf"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("itext-xtra"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("5.5.13.2"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.md5.asc"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), equalTo(SignatureType.GPG));

    // The path is subordinate (it's a signature)
    assertThat(mavenPath.isSubordinate(), is(true));
    assertThat(mavenPath.isSignature(), is(true));

    // main() should unwrap to the base artifact without throwing exceptions
    MavenPath main = mavenPath.main();
    assertThat(main, is(notNullValue()));
    assertThat(main.isSubordinate(), is(false));
    assertThat(main.getPath(), equalTo("com/itextpdf/itext-xtra/5.5.13.2/itext-xtra-5.5.13.2-javadoc.jar"));
    assertThat(main.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(main.getCoordinates().getClassifier(), equalTo("javadoc"));
  }

  /**
   * Tests parsing of a simple hash signature (*.jar.asc) to ensure we didn't break existing behavior.
   */
  @Test
  public void signatureOfJarFile() {
    final String path = "/com/example/artifact/1.0/artifact-1.0.jar.asc";
    final MavenPath mavenPath = pathParser.parsePath(path);
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("com.example"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("artifact"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.asc"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), equalTo(SignatureType.GPG));

    assertThat(mavenPath.isSubordinate(), is(true));
    assertThat(mavenPath.isSignature(), is(true));

    MavenPath main = mavenPath.main();
    assertThat(main.getPath(), equalTo("com/example/artifact/1.0/artifact-1.0.jar"));
    assertThat(main.getCoordinates().getExtension(), equalTo("jar"));
  }

  /**
   * Tests parsing of a simple hash file (*.jar.md5) to ensure we didn't break existing behavior.
   */
  @Test
  public void hashOfJarFile() {
    final String path = "/com/example/artifact/1.0/artifact-1.0.jar.md5";
    final MavenPath mavenPath = pathParser.parsePath(path);
    assertThat(mavenPath, is(notNullValue()));

    assertThat(mavenPath.getHashType(), equalTo(HashType.MD5));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.md5"));

    assertThat(mavenPath.isSubordinate(), is(true));
    assertThat(mavenPath.isHash(), is(true));

    MavenPath main = mavenPath.main();
    assertThat(main.getPath(), equalTo("com/example/artifact/1.0/artifact-1.0.jar"));
    assertThat(main.getCoordinates().getExtension(), equalTo("jar"));
  }
}
