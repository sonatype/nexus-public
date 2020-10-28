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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.sonatype.nexus.repository.maven.internal.Constants.INDEX_MAIN_CHUNK_FILE_PATH;
import static org.sonatype.nexus.repository.maven.internal.Constants.INDEX_PROPERTY_FILE_PATH;

/**
 * UT for {@link Maven2MavenPathParser}
 *
 * @since 3.0
 */
public class Maven2MavenPathParserTest
    extends TestSupport
{
  private final Maven2MavenPathParser pathParser = new Maven2MavenPathParser();

  private long parseTimestamp(final String ts) throws ParseException {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return sdf.parse(ts).getTime();
  }

  @Test
  public void artifact() throws Exception
  {
    MavenPath mavenPath;

    mavenPath = pathParser.parsePath("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-25-javadoc.jar");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-25-javadoc.jar"));
    assertThat(mavenPath.getFileName(), equalTo("jruby-1.0RC1-20070504.160758-25-javadoc.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.jruby"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jruby"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0RC1-20070504.160758-25"));
    assertThat(mavenPath.getCoordinates().getTimestamp(), equalTo(parseTimestamp("20070504.160758")));
    assertThat(mavenPath.getCoordinates().getBuildNumber(), equalTo(25));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0RC1-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/com/sun/xml/ws/jaxws-local-transport/2.1.3/jaxws-local-transport-2.1.3.pom.md5");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "com/sun/xml/ws/jaxws-local-transport/2.1.3/jaxws-local-transport-2.1.3.pom.md5"));
    assertThat(mavenPath.getFileName(), equalTo("jaxws-local-transport-2.1.3.pom.md5"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.MD5));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("com.sun.xml.ws"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jaxws-local-transport"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("2.1.3"));
    assertThat(mavenPath.getCoordinates().getTimestamp(), nullValue());
    assertThat(mavenPath.getCoordinates().getBuildNumber(), nullValue());
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("2.1.3"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom.md5"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar"));
    assertThat(mavenPath.getFileName(), equalTo("jruby-1.0RC1-20070504.160758-2.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.jruby"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jruby"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0RC1-20070504.160758-2"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0RC1-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar.md5");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar.md5"));
    assertThat(mavenPath.getFileName(), equalTo("jruby-1.0RC1-20070504.160758-2.jar.md5"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.MD5));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.jruby"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jruby"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0RC1-20070504.160758-2"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0RC1-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.md5"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath =
        pathParser.parsePath(
            "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT.jar.sha1");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT.jar.sha1"));
    assertThat(mavenPath.getFileName(), equalTo("dsms-intervention-service-2.4.2-64-SNAPSHOT.jar.sha1"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.SHA1));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("com.stchome.products.dsms.services"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("dsms-intervention-service"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("2.4.2-64-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("2.4.2-64-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.sha1"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath =
        pathParser.parsePath(
            "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT-javadoc.jar.sha1");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT-javadoc.jar.sha1"));
    assertThat(mavenPath.getFileName(), equalTo("dsms-intervention-service-2.4.2-64-SNAPSHOT-javadoc.jar.sha1"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.SHA1));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("com.stchome.products.dsms.services"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("dsms-intervention-service"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("2.4.2-64-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("2.4.2-64-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.sha1"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/org/jruby/jruby/1.0/jruby-1.0-javadoc.jar");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/jruby/jruby/1.0/jruby-1.0-javadoc.jar"));
    assertThat(mavenPath.getFileName(), equalTo("jruby-1.0-javadoc.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.jruby"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jruby"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/org/jruby/jruby/1.0/jruby-1.0-javadoc.jar.sha1");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/jruby/jruby/1.0/jruby-1.0-javadoc.jar.sha1"));
    assertThat(mavenPath.getFileName(), equalTo("jruby-1.0-javadoc.jar.sha1"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.SHA1));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.jruby"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jruby"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar.sha1"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/activemq/activemq-core/1.2/activemq-core-1.2.pom");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "activemq/activemq-core/1.2/activemq-core-1.2.pom"));
    assertThat(mavenPath.getFileName(), equalTo("activemq-core-1.2.pom"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("activemq"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("activemq-core"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.2"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.2"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/junit/junit/3.8/junit-3.8.jar");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "junit/junit/3.8/junit-3.8.jar"));
    assertThat(mavenPath.getFileName(), equalTo("junit-3.8.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("junit"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("junit"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.8"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.8"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    // NEXUS-3148
    mavenPath = pathParser.parsePath("/foo1/foo1/0.0.1SNAPSHOT/foo1-0.0.1SNAPSHOT.pom");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "foo1/foo1/0.0.1SNAPSHOT/foo1-0.0.1SNAPSHOT.pom"));
    assertThat(mavenPath.getFileName(), equalTo("foo1-0.0.1SNAPSHOT.pom"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("0.0.1SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("0.0.1SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/foo1/foo1/0.0.1.SNAPSHOT/foo1-0.0.1.SNAPSHOT.pom");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "foo1/foo1/0.0.1.SNAPSHOT/foo1-0.0.1.SNAPSHOT.pom"));
    assertThat(mavenPath.getFileName(), equalTo("foo1-0.0.1.SNAPSHOT.pom"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("0.0.1.SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("0.0.1.SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());
    // NEXUS-3148 ends

    mavenPath = pathParser.parsePath("/foo1/foo1/0.0.1-SNAPSHOT/foo1-0.0.1-SNAPSHOT-jdk14.jar");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "foo1/foo1/0.0.1-SNAPSHOT/foo1-0.0.1-SNAPSHOT-jdk14.jar"));
    assertThat(mavenPath.getFileName(), equalTo("foo1-0.0.1-SNAPSHOT-jdk14.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("0.0.1-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("0.0.1-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("jdk14"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/foo1/foo1/1.0.0-beta-4-SNAPSHOT/foo1-1.0.0-beta-4-20080623.175436-1.jar");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "foo1/foo1/1.0.0-beta-4-SNAPSHOT/foo1-1.0.0-beta-4-20080623.175436-1.jar"));
    assertThat(mavenPath.getFileName(), equalTo("foo1-1.0.0-beta-4-20080623.175436-1.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("foo1"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-4-20080623.175436-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-4-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());


    mavenPath =
        pathParser.parsePath(
            "/org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc"));
    assertThat(mavenPath.getFileName(), equalTo("maven-artifact-3.0-20080411.005221-75.pom.asc"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.apache.maven.artifact"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("maven-artifact"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.0-20080411.005221-75"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom.asc"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), equalTo(SignatureType.GPG));

    mavenPath =
        pathParser.parsePath(
            "/org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc.sha1");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc.sha1"));
    assertThat(mavenPath.getFileName(), equalTo("maven-artifact-3.0-20080411.005221-75.pom.asc.sha1"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.SHA1));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.apache.maven.artifact"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("maven-artifact"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.0-20080411.005221-75"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom.asc.sha1"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), equalTo(SignatureType.GPG));

    mavenPath =
        pathParser.parsePath(
            "/org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75-some.strange.classifier.pom.asc.sha1");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75-some.strange.classifier.pom.asc.sha1"));
    assertThat(mavenPath.getFileName(),
        equalTo("maven-artifact-3.0-20080411.005221-75-some.strange.classifier.pom.asc.sha1"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.SHA1));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.apache.maven.artifact"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("maven-artifact"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("3.0-20080411.005221-75"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("some.strange.classifier"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom.asc.sha1"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), equalTo(SignatureType.GPG));

    mavenPath = pathParser.parsePath("/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.pom.md5.sha1");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getFileName(), equalTo("jsr305-1.3.9.pom.md5.sha1"));
    assertThat(mavenPath.getPath(), equalTo("com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.pom.md5.sha1"));
    assertThat(mavenPath.getHashType(), equalTo(HashType.SHA1));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("com.google.code.findbugs"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jsr305"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.3.9"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.3.9"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom.md5.sha1"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());

    mavenPath = pathParser.parsePath("/com/netflix/spectator/spectator-ext-ipc/0.103.0/spectator-ext-ipc-0.103.0.pom.md5.asc");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getFileName(), equalTo("spectator-ext-ipc-0.103.0.pom.md5.asc"));
    assertThat(mavenPath.getPath(), equalTo("com/netflix/spectator/spectator-ext-ipc/0.103.0/spectator-ext-ipc-0.103.0.pom.md5.asc"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("com.netflix.spectator"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("spectator-ext-ipc"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("0.103.0"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("0.103.0"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom.md5.asc"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), equalTo(SignatureType.GPG));
  }

  @Test
  public void badlyBuiltSnapshot() throws Exception {
    MavenPath mavenPath = pathParser
        .parsePath("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-SNAPSHOT-20070504.160758-25-javadoc.jar");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-SNAPSHOT-20070504.160758-25-javadoc.jar"));
    assertThat(mavenPath.getFileName(), equalTo("jruby-1.0RC1-SNAPSHOT-20070504.160758-25-javadoc.jar"));
    assertThat(mavenPath.getHashType(), nullValue());
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo("org.jruby"));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo("jruby"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0RC1-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0RC1-20070504.160758-25"));
    assertThat(mavenPath.getCoordinates().getTimestamp(), equalTo(parseTimestamp("20070504.160758")));
    assertThat(mavenPath.getCoordinates().getBuildNumber(), equalTo(25));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0RC1-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getSignatureType(), nullValue());
  }

  @Test
  public void metadata() throws Exception
  {
    MavenPath mavenPath;

    mavenPath = pathParser.parsePath("/something/that/looks/maven-metadata.xml");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(true));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/something/that/looks/like-SNAPSHOT/maven-metadata.xml.sha1");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(true));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/org/codehaus/plexus/plexus-container-default/maven-metadata.xml.md5");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(true));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/org/jruby/jruby/1.0/maven-metadata.xml");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(true));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/org/jruby/jruby/1.0-SNAPSHOT/maven-metadata.xml");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(true));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));
  }

  @Test
  public void index() {
    MavenPath mavenPath;

    mavenPath = pathParser.parsePath(INDEX_PROPERTY_FILE_PATH);
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(true));
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath(INDEX_MAIN_CHUNK_FILE_PATH);
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(true));
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
  }

  @Test
  public void other() throws Exception
  {
    MavenPath mavenPath;

    // NEXUS-4132
    mavenPath = pathParser.parsePath(
        "/com/electrabel/connection-register-ear/1.2-SNAPSHOT/connection-register-ear-1.2-20101214.143755.ear");
    assertThat(mavenPath.getCoordinates(), nullValue()); // filename lacks the -BBB build number
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath =
        pathParser.parsePath(
            "/org/apache/maven/plugins/maven-dependency-plugin/2.0-SNAPSHOT/maven-dependency-plugin-2.0-alpha-1-20070109.165112-13.jar");
    assertThat(mavenPath.getCoordinates(), notNullValue()); // baseVersion != version mismatch
    assertFalse(mavenPath.getCoordinates().getBaseVersion().startsWith("2.0-alpha"));
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/some/stupid/path");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/some/stupid/path/more/in/it");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/something/that/looks/");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/something/that/looks/like-an-artifact.blah");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("/something/that/looks/like-an-artifact.pom");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("org/apache/maven/scm/maven-scm");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));

    mavenPath = pathParser.parsePath("org/apache/geronimo/javamail/geronimo-javamail_1.4_mail");
    assertThat(mavenPath.getCoordinates(), nullValue());
    assertThat(pathParser.isRepositoryMetadata(mavenPath), equalTo(false));
    assertThat(pathParser.isRepositoryIndex(mavenPath), equalTo(false));
  }


  @Test
  public void extremeExtAndClassifier() throws Exception
  {
    MavenPath mavenPath;

    mavenPath =
        pathParser.parsePath("/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5/nexus-webapp-1.0.0-beta-5.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-5"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(false));

    mavenPath =
        pathParser.parsePath("/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5/nexus-webapp-1.0.0-beta-5-bundle.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("bundle"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-5"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(false));

    mavenPath =
        pathParser.parsePath(
            "/org/codehaus/tycho/tycho-distribution/0.3.0-SNAPSHOT/tycho-distribution-0.3.0-SNAPSHOT-bin.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("bin"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("0.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("0.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));

    mavenPath =
        pathParser.parsePath("/org/codehaus/tycho/tycho-distribution/SNAPSHOT/tycho-distribution-SNAPSHOT-bin.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("bin"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));

    mavenPath =
        pathParser.parsePath(
            "/org/codehaus/tycho/tycho-distribution/0.3.0-SNAPSHOT/tycho-distribution-0.3.0-20080818.153246-33-bin.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("bin"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("0.3.0-20080818.153246-33"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("0.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));

    mavenPath =
        pathParser.parsePath("/org/sonatype/nexus/nexus-webapp/1.0.0-beta-4.2/nexus-webapp-1.0.0-beta-4.2-javadoc.jar");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("javadoc"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-4.2"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-4.2"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(false));

    mavenPath =
        pathParser.parsePath(
            "/org/codehaus/tycho/tycho-distribution/0.3.0-SNAPSHOT/tycho-distribution-0.3.0-20080818.153246-33-bin.zip");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("zip"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("bin"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("0.3.0-20080818.153246-33"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("0.3.0-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.jar");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("jar"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("cli"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-6-20080809.181715-2"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-6-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));
  }

  @Test
  public void parseExtension() {
    MavenPath mavenPath;

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.anyext");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("anyext"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.anyext.sha1");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("anyext.sha1"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.anyext.md5");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("anyext.md5"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.anyext.asc");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("anyext.asc"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.anyext.asc.md5");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("anyext.asc.md5"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.tar.anyext");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.anyext"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.cpio.anyext");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("cpio.anyext"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.anyext.zip");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("zip"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.nk.os");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("nk.os"));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.unknown.os");
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("os"));
  }


  @Test
  public void extremeSnapshot() throws Exception
  {
    MavenPath mavenPath;

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT/nexus-webapp-1.0.0-beta-5-SNAPSHOT.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-5-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT-1234/nexus-webapp-1.0.0-beta-5-SNAPSHOT-1234.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-5-SNAPSHOT-1234"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5-SNAPSHOT-1234"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(false));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT/nexus-webapp-1.0.0-beta-5-SNAPSHOT-bundle.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("bundle"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-5-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));

    mavenPath =
        pathParser.parsePath(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT-1234/nexus-webapp-1.0.0-beta-5-SNAPSHOT-1234-bundle.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo("bundle"));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-5-SNAPSHOT-1234"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5-SNAPSHOT-1234"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(false));

    mavenPath = pathParser.parsePath("/org/sonatype/nexus-3148/1.0.SNAPSHOT/nexus-3148-1.0.20100111.064938-1.pom");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("pom"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.20100111.064938-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));
  }

  @Test
  public void snapshot_validFormat() throws Exception
  {
    MavenPath mavenPath = pathParser.parsePath(
        "org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT/nexus-webapp-1.0.0-beta-5-20171208.202054-1.tar.gz");
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo("tar.gz"));
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo("1.0.0-beta-5-20171208.202054-1"));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));
    assertThat(mavenPath.getCoordinates().getTimestamp(), notNullValue());
    assertThat(mavenPath.getCoordinates().getBuildNumber(), notNullValue());
  }

  @Test
  public void snapshot_invalidFormat() throws Exception
  {
    MavenPath mavenPath = pathParser.parsePath(
        "org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT/nexus-webapp-1.0.0-beta-5-20171208-test.tar.gz");
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(
        "org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT/nexus-webapp-1.0.0-beta-5-20171208-test.tar.gz"));
    assertThat(mavenPath.getFileName(), equalTo("nexus-webapp-1.0.0-beta-5-20171208-test.tar.gz"));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().getExtension(), notNullValue());
    assertThat(mavenPath.getCoordinates().getClassifier(), nullValue());
    assertThat(mavenPath.getCoordinates().getVersion(), notNullValue());
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo("1.0.0-beta-5-SNAPSHOT"));
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(true));
    assertThat(mavenPath.getCoordinates().getTimestamp(), nullValue());
    assertThat(mavenPath.getCoordinates().getBuildNumber(), nullValue());
  }
}
