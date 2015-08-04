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
package org.sonatype.nexus.proxy.maven.gav;


import java.text.ParseException;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

public class M2GavCalculatorTest
    extends TestCase
{
  private M2GavCalculator gavCalculator;

  private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    gavCalculator = new M2GavCalculator();
  }

  protected Long parseTimestamp(String timeStamp)
      throws ParseException
  {
    if (timeStamp == null) {
      return null;
    }
    else {
      return Long.valueOf(formatter.parse(timeStamp).getTime());
    }
  }

  public void testGav()
      throws Exception
  {
    Gav gav;
    String path;

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-25-javadoc.jar");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0RC1-20070504.160758-25", gav.getVersion());
    assertEquals("1.0RC1-SNAPSHOT", gav.getBaseVersion());
    assertEquals("javadoc", gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(25), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20070504.160758"), gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0RC1-20070504.160758-25-javadoc.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-25-javadoc.jar", path);

    gav =
        gavCalculator.pathToGav("/com/sun/xml/ws/jaxws-local-transport/2.1.3/jaxws-local-transport-2.1.3.pom.md5");

    assertEquals("com.sun.xml.ws", gav.getGroupId());
    assertEquals("jaxws-local-transport", gav.getArtifactId());
    assertEquals("2.1.3", gav.getVersion());
    assertEquals("2.1.3", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("jaxws-local-transport-2.1.3.pom.md5", gav.getName());
    assertEquals(false, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(Gav.HashType.md5, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/com/sun/xml/ws/jaxws-local-transport/2.1.3/jaxws-local-transport-2.1.3.pom.md5", path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0RC1-20070504.160758-2", gav.getVersion());
    assertEquals("1.0RC1-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(2), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20070504.160758"), gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0RC1-20070504.160758-2.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar", path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar.md5");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0RC1-20070504.160758-2", gav.getVersion());
    assertEquals("1.0RC1-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(2), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20070504.160758"), gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0RC1-20070504.160758-2.jar.md5", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(Gav.HashType.md5, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar.md5", path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0RC1-20070504.160758-2", gav.getVersion());
    assertEquals("1.0RC1-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(2), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20070504.160758"), gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0RC1-20070504.160758-2.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2.jar", path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2-sources.jar");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0RC1-20070504.160758-2", gav.getVersion());
    assertEquals("1.0RC1-SNAPSHOT", gav.getBaseVersion());
    assertEquals("sources", gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(2), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20070504.160758"), gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0RC1-20070504.160758-2-sources.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0RC1-SNAPSHOT/jruby-1.0RC1-20070504.160758-2-sources.jar", path);

    gav =
        gavCalculator.pathToGav(
            "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT.jar.sha1");

    assertEquals("com.stchome.products.dsms.services", gav.getGroupId());
    assertEquals("dsms-intervention-service", gav.getArtifactId());
    assertEquals("2.4.2-64-SNAPSHOT", gav.getVersion());
    assertEquals("2.4.2-64-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("dsms-intervention-service-2.4.2-64-SNAPSHOT.jar.sha1", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(Gav.HashType.sha1, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals(
        "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT.jar.sha1",
        path);

    gav =
        gavCalculator.pathToGav(
            "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT-javadoc.jar.sha1");

    assertEquals("com.stchome.products.dsms.services", gav.getGroupId());
    assertEquals("dsms-intervention-service", gav.getArtifactId());
    assertEquals("2.4.2-64-SNAPSHOT", gav.getVersion());
    assertEquals("2.4.2-64-SNAPSHOT", gav.getBaseVersion());
    assertEquals("javadoc", gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("dsms-intervention-service-2.4.2-64-SNAPSHOT-javadoc.jar.sha1", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(Gav.HashType.sha1, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals(
        "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT-javadoc.jar.sha1",
        path);

    gav =
        gavCalculator.pathToGav(
            "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT.jar");

    assertEquals("com.stchome.products.dsms.services", gav.getGroupId());
    assertEquals("dsms-intervention-service", gav.getArtifactId());
    assertEquals("2.4.2-64-SNAPSHOT", gav.getVersion());
    assertEquals("2.4.2-64-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("dsms-intervention-service-2.4.2-64-SNAPSHOT.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals(
        "/com/stchome/products/dsms/services/dsms-intervention-service/2.4.2-64-SNAPSHOT/dsms-intervention-service-2.4.2-64-SNAPSHOT.jar",
        path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0/jruby-1.0-javadoc.jar");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0", gav.getVersion());
    assertEquals("1.0", gav.getBaseVersion());
    assertEquals("javadoc", gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0-javadoc.jar", gav.getName());
    assertEquals(false, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0/jruby-1.0-javadoc.jar", path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0/jruby-1.0-javadoc.jar.sha1");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0", gav.getVersion());
    assertEquals("1.0", gav.getBaseVersion());
    assertEquals("javadoc", gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0-javadoc.jar.sha1", gav.getName());
    assertEquals(false, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(Gav.HashType.sha1, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0/jruby-1.0-javadoc.jar.sha1", path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0/jruby-1.0.jar");

    assertEquals("org.jruby", gav.getGroupId());
    assertEquals("jruby", gav.getArtifactId());
    assertEquals("1.0", gav.getVersion());
    assertEquals("1.0", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("jruby-1.0.jar", gav.getName());
    assertEquals(false, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/jruby/jruby/1.0/jruby-1.0.jar", path);

    gav = gavCalculator.pathToGav("/activemq/activemq-core/1.2/activemq-core-1.2.pom");

    assertEquals("activemq", gav.getGroupId());
    assertEquals("activemq-core", gav.getArtifactId());
    assertEquals("1.2", gav.getVersion());
    assertEquals("1.2", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("activemq-core-1.2.pom", gav.getName());
    assertEquals(false, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/activemq/activemq-core/1.2/activemq-core-1.2.pom", path);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0/maven-metadata.xml");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("/org/jruby/jruby/1.0-SNAPSHOT/maven-metadata.xml");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("/junit/junit/3.8/junit-3.8.jar");
    assertEquals("junit", gav.getGroupId());
    assertEquals("junit", gav.getArtifactId());
    assertEquals("3.8", gav.getVersion());
    assertEquals("3.8", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("junit-3.8.jar", gav.getName());
    assertEquals(false, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    gav = gavCalculator.pathToGav("/foo1/foo1/0.0.1-SNAPSHOT/foo1-0.0.1-SNAPSHOT.pom");
    assertEquals("foo1", gav.getGroupId());
    assertEquals("foo1", gav.getArtifactId());
    assertEquals("0.0.1-SNAPSHOT", gav.getVersion());
    assertEquals("0.0.1-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("foo1-0.0.1-SNAPSHOT.pom", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/foo1/foo1/0.0.1-SNAPSHOT/foo1-0.0.1-SNAPSHOT.pom", path);

    // NEXUS-3148
    gav = gavCalculator.pathToGav("/foo1/foo1/0.0.1SNAPSHOT/foo1-0.0.1SNAPSHOT.pom");
    assertEquals("foo1", gav.getGroupId());
    assertEquals("foo1", gav.getArtifactId());
    assertEquals("0.0.1SNAPSHOT", gav.getVersion());
    assertEquals("0.0.1SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("foo1-0.0.1SNAPSHOT.pom", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/foo1/foo1/0.0.1SNAPSHOT/foo1-0.0.1SNAPSHOT.pom", path);

    gav = gavCalculator.pathToGav("/foo1/foo1/0.0.1.SNAPSHOT/foo1-0.0.1.SNAPSHOT.pom");
    assertEquals("foo1", gav.getGroupId());
    assertEquals("foo1", gav.getArtifactId());
    assertEquals("0.0.1.SNAPSHOT", gav.getVersion());
    assertEquals("0.0.1.SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("foo1-0.0.1.SNAPSHOT.pom", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/foo1/foo1/0.0.1.SNAPSHOT/foo1-0.0.1.SNAPSHOT.pom", path);

    // NEXUS-3148 ends

    gav = gavCalculator.pathToGav("/foo1/foo1/0.0.1-SNAPSHOT/foo1-0.0.1-SNAPSHOT-jdk14.jar");
    assertEquals("foo1", gav.getGroupId());
    assertEquals("foo1", gav.getArtifactId());
    assertEquals("0.0.1-SNAPSHOT", gav.getVersion());
    assertEquals("0.0.1-SNAPSHOT", gav.getBaseVersion());
    assertEquals("jdk14", gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("foo1-0.0.1-SNAPSHOT-jdk14.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/foo1/foo1/0.0.1-SNAPSHOT/foo1-0.0.1-SNAPSHOT-jdk14.jar", path);

    gav = gavCalculator.pathToGav("/foo1/foo1/1.0.0-beta-4-SNAPSHOT/foo1-1.0.0-beta-4-20080623.175436-1.jar");
    assertEquals("foo1", gav.getGroupId());
    assertEquals("foo1", gav.getArtifactId());
    assertEquals("1.0.0-beta-4-20080623.175436-1", gav.getVersion());
    assertEquals("1.0.0-beta-4-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(1), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20080623.175436"), gav.getSnapshotTimeStamp());
    assertEquals("foo1-1.0.0-beta-4-20080623.175436-1.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/foo1/foo1/1.0.0-beta-4-SNAPSHOT/foo1-1.0.0-beta-4-20080623.175436-1.jar", path);

    gav =
        gavCalculator.pathToGav(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-4-SNAPSHOT/nexus-webapp-1.0.0-beta-4-20080623.203653-349.pom");
    assertEquals("org.sonatype.nexus", gav.getGroupId());
    assertEquals("nexus-webapp", gav.getArtifactId());
    assertEquals("1.0.0-beta-4-20080623.203653-349", gav.getVersion());
    assertEquals("1.0.0-beta-4-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(Integer.valueOf(349), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20080623.203653"), gav.getSnapshotTimeStamp());
    assertEquals("nexus-webapp-1.0.0-beta-4-20080623.203653-349.pom", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals(
        "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-4-SNAPSHOT/nexus-webapp-1.0.0-beta-4-20080623.203653-349.pom",
        path);

    gav =
        gavCalculator.pathToGav(
            "/org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc");
    assertEquals("org.apache.maven.artifact", gav.getGroupId());
    assertEquals("maven-artifact", gav.getArtifactId());
    assertEquals("3.0-20080411.005221-75", gav.getVersion());
    assertEquals("3.0-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(Integer.valueOf(75), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20080411.005221"), gav.getSnapshotTimeStamp());
    assertEquals("maven-artifact-3.0-20080411.005221-75.pom.asc", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());
    assertEquals(true, gav.isSignature());
    assertEquals(Gav.SignatureType.gpg, gav.getSignatureType());

    path = gavCalculator.gavToPath(gav);
    assertEquals(
        "/org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc",
        path);

    gav =
        gavCalculator.pathToGav(
            "/org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc.sha1");
    assertEquals("org.apache.maven.artifact", gav.getGroupId());
    assertEquals("maven-artifact", gav.getArtifactId());
    assertEquals("3.0-20080411.005221-75", gav.getVersion());
    assertEquals("3.0-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(Integer.valueOf(75), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20080411.005221"), gav.getSnapshotTimeStamp());
    assertEquals("maven-artifact-3.0-20080411.005221-75.pom.asc.sha1", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(Gav.HashType.sha1, gav.getHashType());
    assertEquals(true, gav.isSignature());
    assertEquals(Gav.SignatureType.gpg, gav.getSignatureType());

    path = gavCalculator.gavToPath(gav);
    assertEquals(
        "/org/apache/maven/artifact/maven-artifact/3.0-SNAPSHOT/maven-artifact-3.0-20080411.005221-75.pom.asc.sha1",
        path);

    gav =
        gavCalculator
            .pathToGav("/org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20080302.032223-6-bin.zip.sha1");
    assertEquals("org.apache.maven", gav.getGroupId());
    assertEquals("maven-core", gav.getArtifactId());
    assertEquals("2.0.9-20080302.032223-6", gav.getVersion());
    assertEquals("2.0.9-SNAPSHOT", gav.getBaseVersion());
    assertEquals("bin", gav.getClassifier());
    assertEquals("zip", gav.getExtension());
    assertEquals(Integer.valueOf(6), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20080302.032223"), gav.getSnapshotTimeStamp());
    assertEquals("maven-core-2.0.9-20080302.032223-6-bin.zip.sha1", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(Gav.HashType.sha1, gav.getHashType());
    assertEquals(false, gav.isSignature());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20080302.032223-6-bin.zip.sha1",
        path);

    gav =
        gavCalculator
            .pathToGav("/org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20080302.032223-6-bin.tar.gz");
    assertEquals("org.apache.maven", gav.getGroupId());
    assertEquals("maven-core", gav.getArtifactId());
    assertEquals("2.0.9-20080302.032223-6", gav.getVersion());
    assertEquals("2.0.9-SNAPSHOT", gav.getBaseVersion());
    assertEquals("bin", gav.getClassifier());
    assertEquals("tar.gz", gav.getExtension());
    assertEquals(Integer.valueOf(6), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20080302.032223"), gav.getSnapshotTimeStamp());
    assertEquals("maven-core-2.0.9-20080302.032223-6-bin.tar.gz", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());
    assertEquals(false, gav.isSignature());

    gav = gavCalculator.pathToGav("/org/sonatype/nexus/nexus-webapp/1.4.0/nexus-webapp-1.4.0-bundle.tar.gz");
    assertEquals("org.sonatype.nexus", gav.getGroupId());
    assertEquals("nexus-webapp", gav.getArtifactId());
    assertEquals("1.4.0", gav.getVersion());
    assertEquals("1.4.0", gav.getBaseVersion());
    assertEquals("bundle", gav.getClassifier());
    assertEquals("tar.gz", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("nexus-webapp-1.4.0-bundle.tar.gz", gav.getName());
    assertEquals(false, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());
    assertEquals(false, gav.isSignature());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/org/sonatype/nexus/nexus-webapp/1.4.0/nexus-webapp-1.4.0-bundle.tar.gz", path);

    gav = gavCalculator.pathToGav("/foo/artifact/SNAPSHOT/artifact-SNAPSHOT.jar");
    assertEquals("foo", gav.getGroupId());
    assertEquals("artifact", gav.getArtifactId());
    assertEquals("SNAPSHOT", gav.getVersion());
    assertEquals("SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(null, gav.getSnapshotBuildNumber());
    assertEquals(null, gav.getSnapshotTimeStamp());
    assertEquals("artifact-SNAPSHOT.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/foo/artifact/SNAPSHOT/artifact-SNAPSHOT.jar", path);

    gav = gavCalculator.pathToGav("/foo/artifact/SNAPSHOT/artifact-20080623.175436-1.jar");
    assertEquals("foo", gav.getGroupId());
    assertEquals("artifact", gav.getArtifactId());
    assertEquals("20080623.175436-1", gav.getVersion());
    assertEquals("SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(1), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20080623.175436"), gav.getSnapshotTimeStamp());
    assertEquals("artifact-20080623.175436-1.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    path = gavCalculator.gavToPath(gav);
    assertEquals("/foo/artifact/SNAPSHOT/artifact-20080623.175436-1.jar", path);
  }

  public void testNegGav()
      throws Exception
  {
    Gav gav;
    String path;

    // NEXUS-4132
    gav =
        gavCalculator.pathToGav(
            "/com/electrabel/connection-register-ear/1.2-SNAPSHOT/connection-register-ear-1.2-20101214.143755.ear");
    assertNull("Should fail, since the filename lacks the -BBB build number, hence, is not valid snapshot", gav);
    // NEXUS-4132 END
  }

  public void testGavExtreme()
      throws Exception
  {
    Gav gav;

    gav = gavCalculator.pathToGav("/");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("/some/stupid/path");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("/some/stupid/path/more/in/it");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("/something/that/looks/");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("/something/that/looks/like-an-artifact.blah");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("/something/that/looks/like-an-artifact.pom");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("org/apache/maven/scm/maven-scm");
    assertEquals(null, gav);

    gav = gavCalculator.pathToGav("org/apache/geronimo/javamail/geronimo-javamail_1.4_mail");
    assertEquals(null, gav);

    // this is metadata, will return null
    gav = gavCalculator.pathToGav("/something/that/looks/maven-metadata.xml");
    assertEquals(null, gav);

    // this is metadata, will return null
    gav = gavCalculator.pathToGav("/something/that/looks/like-SNAPSHOT/maven-metadata.xml");
    assertEquals(null, gav);

    // this is metadata, will return null
    gav = gavCalculator.pathToGav("/org/codehaus/plexus/plexus-container-default/maven-metadata.xml");
    assertEquals(null, gav);
  }

  public void testIssueNexus57()
      throws Exception
  {
    // broken path, baseVersion and version mismatch (2.0-SNAPSHOT vs 2.0-alpha-1...)
    Gav gav =
        gavCalculator.pathToGav(
            "/org/apache/maven/plugins/maven-dependency-plugin/2.0-SNAPSHOT/maven-dependency-plugin-2.0-alpha-1-20070109.165112-13.jar");

    assertNull("We expect null since baseVersion and version mismatch in path!", gav);
  }

  public void testGavExtensionAndClassifier()
      throws Exception
  {
    Gav gav;

    gav =
        gavCalculator.pathToGav("/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5/nexus-webapp-1.0.0-beta-5.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals(null, gav.getClassifier());
    assertEquals("1.0.0-beta-5", gav.getVersion());

    gav =
        gavCalculator
            .pathToGav("/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5/nexus-webapp-1.0.0-beta-5-bundle.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals("bundle", gav.getClassifier());
    assertEquals("1.0.0-beta-5", gav.getVersion());

    gav =
        gavCalculator.pathToGav(
            "/org/codehaus/tycho/tycho-distribution/0.3.0-SNAPSHOT/tycho-distribution-0.3.0-SNAPSHOT-bin.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals("bin", gav.getClassifier());
    assertEquals("0.3.0-SNAPSHOT", gav.getVersion());

    gav =
        gavCalculator
            .pathToGav("/org/codehaus/tycho/tycho-distribution/SNAPSHOT/tycho-distribution-SNAPSHOT-bin.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals("bin", gav.getClassifier());
    assertEquals("SNAPSHOT", gav.getVersion());

    gav =
        gavCalculator.pathToGav(
            "/org/codehaus/tycho/tycho-distribution/0.3.0-SNAPSHOT/tycho-distribution-0.3.0-20080818.153246-33-bin.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals("bin", gav.getClassifier());
    assertEquals("0.3.0-20080818.153246-33", gav.getVersion());

    gav =
        gavCalculator
            .pathToGav("/org/sonatype/nexus/nexus-webapp/1.0.0-beta-4.2/nexus-webapp-1.0.0-beta-4.2-javadoc.jar");
    assertNotNull(gav);
    assertEquals("jar", gav.getExtension());
    assertEquals("javadoc", gav.getClassifier());
    assertEquals("1.0.0-beta-4.2", gav.getVersion());

    gav =
        gavCalculator.pathToGav(
            "/org/codehaus/tycho/tycho-distribution/0.3.0-SNAPSHOT/tycho-distribution-0.3.0-20080818.153246-33-bin.zip");
    assertNotNull(gav);
    assertEquals("zip", gav.getExtension());
    assertEquals("bin", gav.getClassifier());
    assertEquals("0.3.0-20080818.153246-33", gav.getVersion());

    gav =
        gavCalculator.pathToGav(
            "/org/sonatype/nexus/tools/nexus-migration-app/1.0.0-beta-6-SNAPSHOT/nexus-migration-app-1.0.0-beta-6-20080809.181715-2-cli.jar");
    assertNotNull(gav);
    assertEquals("jar", gav.getExtension());
    assertEquals("cli", gav.getClassifier());
    assertEquals("1.0.0-beta-6-20080809.181715-2", gav.getVersion());
  }

  public void testGavSnapshotVersion()
      throws Exception
  {
    Gav gav;

    gav =
        gavCalculator.pathToGav(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT/nexus-webapp-1.0.0-beta-5-SNAPSHOT.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals(null, gav.getClassifier());
    assertEquals("1.0.0-beta-5-SNAPSHOT", gav.getVersion());
    assertTrue(gav.isSnapshot());

    gav =
        gavCalculator.pathToGav(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT-1234/nexus-webapp-1.0.0-beta-5-SNAPSHOT-1234.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals(null, gav.getClassifier());
    assertEquals("1.0.0-beta-5-SNAPSHOT-1234", gav.getVersion());
    assertFalse(gav.isSnapshot());

    gav =
        gavCalculator.pathToGav(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT/nexus-webapp-1.0.0-beta-5-SNAPSHOT-bundle.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals("bundle", gav.getClassifier());
    assertEquals("1.0.0-beta-5-SNAPSHOT", gav.getVersion());
    assertTrue(gav.isSnapshot());

    gav =
        gavCalculator.pathToGav(
            "/org/sonatype/nexus/nexus-webapp/1.0.0-beta-5-SNAPSHOT-1234/nexus-webapp-1.0.0-beta-5-SNAPSHOT-1234-bundle.tar.gz");
    assertNotNull(gav);
    assertEquals("tar.gz", gav.getExtension());
    assertEquals("bundle", gav.getClassifier());
    assertEquals("1.0.0-beta-5-SNAPSHOT-1234", gav.getVersion());
    assertFalse(gav.isSnapshot());
  }

  public void testGavLooseStrictedSnapshot()
      throws Exception
  {
    Gav gav;

    gav = gavCalculator.pathToGav("/org/sonatype/nexus-3148/1.0.SNAPSHOT/nexus-3148-1.0.20100111.064938-1.pom");
    assertNotNull(gav);
    assertEquals("pom", gav.getExtension());
    assertEquals("1.0.20100111.064938-1", gav.getVersion());
    assertEquals("1.0.SNAPSHOT", gav.getBaseVersion());
    assertEquals("org.sonatype", gav.getGroupId());
    assertEquals("nexus-3148", gav.getArtifactId());
    assertEquals(null, gav.getClassifier());
    assertEquals("pom", gav.getExtension());
    assertEquals(new Integer(1), gav.getSnapshotBuildNumber());
    // the timestamp is UTC, not EST timezoned!
    // also, the Gav is it seems TZ sensitive!!!
    assertEquals(parseTimestamp("20100111.064938"), gav.getSnapshotTimeStamp());
    assertEquals("nexus-3148-1.0.20100111.064938-1.pom", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());

    String path = gavCalculator.gavToPath(gav);
    assertEquals("/org/sonatype/nexus-3148/1.0.SNAPSHOT/nexus-3148-1.0.20100111.064938-1.pom", path);
  }
}
