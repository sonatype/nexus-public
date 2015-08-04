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
package org.sonatype.nexus.proxy.maven;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.Gav.HashType;
import org.sonatype.nexus.proxy.maven.gav.Gav.SignatureType;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.gav.M2GavCalculator;
import org.sonatype.nexus.proxy.registry.ContentClass;

import junit.framework.TestCase;

/**
 * Tests for https://issues.sonatype.org/browse/NEXUS-6185 patch of
 * LayoutConverterShadowRepository.transformM2toM1() to correctly handle
 * Timestamped Snapshots to Maven1.
 *
 * NEXUS-6185 Timestamped M2 Snapshots should be consumable over M1 shadow
 */
public class Nexus6185TransformM2toM1WithTimestampTest
    extends TestCase
{

  /**
   * Dummy LayoutConverterShadowRepository created for UT to test single method,
   * and not need a real repository instance.
   */
  public static class Nexus6185LayoutConverterShadowRepository
      extends LayoutConverterShadowRepository
  {

    @Override
    public GavCalculator getM2GavCalculator() {
      return new M2GavCalculator();
    }

    @Override
    public GavCalculator getGavCalculator() {
      throw new RuntimeException("method not implemented");
    }

    @Override
    public ContentClass getRepositoryContentClass() {
      throw new RuntimeException("method not implemented");
    }

    @Override
    public ContentClass getMasterRepositoryContentClass() {
      throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean isMavenMetadataPath(final String path) {
      throw new RuntimeException("method not implemented");
    }

    @Override
    protected List<String> transformMaster2Shadow(final String path) {
      throw new RuntimeException("method not implemented");
    }

    @Override
    protected List<String> transformShadow2Master(final String path) {
      throw new RuntimeException("method not implemented");
    }

    @Override
    protected Configurator getConfigurator() {
      throw new RuntimeException("method not implemented");
    }

    @Override
    protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
      throw new RuntimeException("method not implemented");
    }
  }

  private M2GavCalculator gavCalculator;

  private LayoutConverterShadowRepository layoutConverter;

  private final SimpleDateFormat formatter = new SimpleDateFormat(
      "yyyyMMdd.HHmmss");

  @Override
  public void setUp() throws Exception {
    super.setUp();
    layoutConverter = new Nexus6185LayoutConverterShadowRepository();
    gavCalculator = new M2GavCalculator();
  }

  protected Long parseTimestamp(final String timeStamp) throws ParseException {
    if (timeStamp == null) {
      return null;
    }
    else {
      return Long.valueOf(formatter.parse(timeStamp).getTime());
    }
  }

  public void testGavJar() throws Exception {
    Gav gav;
    gav = gavCalculator
        .pathToGav("/portal/portal/1.2-SNAPSHOT/portal-1.2-20131207.174838-3.jar");

    assertEquals("portal", gav.getGroupId());
    assertEquals("portal", gav.getArtifactId());
    assertEquals("1.2-20131207.174838-3", gav.getVersion());
    assertEquals("1.2-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(3), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20131207.174838"),
        gav.getSnapshotTimeStamp());
    assertEquals("portal-1.2-20131207.174838-3.jar", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());
    assertEquals(false, gav.isSignature());
    assertEquals(null, gav.getSignatureType());

  }

  public void testGavHash() throws Exception {
    Gav gav;
    gav = gavCalculator
        .pathToGav("/portal/portal/1.2-SNAPSHOT/portal-1.2-20131207.174838-3.jar.md5");

    assertEquals("portal", gav.getGroupId());
    assertEquals("portal", gav.getArtifactId());
    assertEquals("1.2-20131207.174838-3", gav.getVersion());
    assertEquals("1.2-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(3), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20131207.174838"),
        gav.getSnapshotTimeStamp());
    assertEquals("portal-1.2-20131207.174838-3.jar.md5", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(HashType.md5, gav.getHashType());
    assertEquals(false, gav.isSignature());
    assertEquals(null, gav.getSignatureType());

    gav = gavCalculator
        .pathToGav("/portal/portal/1.2-SNAPSHOT/portal-1.2-20131207.174838-3.jar.sha1");

    assertEquals("portal", gav.getGroupId());
    assertEquals("portal", gav.getArtifactId());
    assertEquals("1.2-20131207.174838-3", gav.getVersion());
    assertEquals("1.2-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(3), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20131207.174838"),
        gav.getSnapshotTimeStamp());
    assertEquals("portal-1.2-20131207.174838-3.jar.sha1", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(true, gav.isHash());
    assertEquals(HashType.sha1, gav.getHashType());
    assertEquals(false, gav.isSignature());
    assertEquals(null, gav.getSignatureType());

  }

  public void testGavEjbClient() throws Exception {
    Gav gav;
    gav = gavCalculator
        .pathToGav("/portal/portal-ejb/1.2-SNAPSHOT/portal-ejb-1.2-20131207.174838-3-client.jar");

    assertEquals("portal", gav.getGroupId());
    assertEquals("portal-ejb", gav.getArtifactId());
    assertEquals("1.2-20131207.174838-3", gav.getVersion());
    assertEquals("1.2-SNAPSHOT", gav.getBaseVersion());
    assertEquals("client", gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(3), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20131207.174838"),
        gav.getSnapshotTimeStamp());
    assertEquals("portal-ejb-1.2-20131207.174838-3-client.jar",
        gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());
    assertEquals(false, gav.isSignature());
    assertEquals(null, gav.getSignatureType());

  }

  public void testGavSignature() throws Exception {
    Gav gav;
    gav = gavCalculator
        .pathToGav("/portal/portal/1.2-SNAPSHOT/portal-1.2-20131207.174838-3.jar.asc");

    assertEquals("portal", gav.getGroupId());
    assertEquals("portal", gav.getArtifactId());
    assertEquals("1.2-20131207.174838-3", gav.getVersion());
    assertEquals("1.2-SNAPSHOT", gav.getBaseVersion());
    assertEquals(null, gav.getClassifier());
    assertEquals("jar", gav.getExtension());
    assertEquals(Integer.valueOf(3), gav.getSnapshotBuildNumber());
    assertEquals(parseTimestamp("20131207.174838"),
        gav.getSnapshotTimeStamp());
    assertEquals("portal-1.2-20131207.174838-3.jar.asc", gav.getName());
    assertEquals(true, gav.isSnapshot());
    assertEquals(false, gav.isHash());
    assertEquals(null, gav.getHashType());
    assertEquals(true, gav.isSignature());
    assertEquals(SignatureType.gpg, gav.getSignatureType());

  }

  /**
   * Test all kinds of extensions with hashes and signatures to make sure they
   * end up in the right folder.
   */
  public void testTransformM2toM1PomJarWarEar() {
    final String group = "/portal/";
    final String maven2Path = "portal/1.2-SNAPSHOT";
    final String artifact = "/portal-1.2-";
    final String timestamp = "20131207.174838-3.";

    // try all kinds of extensions
    final Collection<String> extensions = new ArrayList<String>();
    extensions.add("pom");
    extensions.add("jar");
    extensions.add("war");
    extensions.add("ear");
    extensions.add("js");

    for (final String extension : extensions) {

      // artifact must end up under its extension
      // System.out.println(extension);
      String path = group + maven2Path + artifact + timestamp + extension;
      List<String> results = layoutConverter.transformM2toM1(path, null);
      for (final String result : results) {
        // System.out.println(result);
        // /portal/poms/portal-1.2-SNAPSHOT.pom
        assertEquals(group + extension + "s" + artifact + "SNAPSHOT."
            + extension, result);
      }

      // with all kinds of hashes
      final Collection<String> hashes = new ArrayList<String>();
      hashes.add(".md5");
      hashes.add(".sha1");
      hashes.add(".asc");

      for (final String hash : hashes) {

        // hash must end up with its artifact
        path = group + maven2Path + artifact + timestamp + extension
            + hash;
        results = layoutConverter.transformM2toM1(path, null);
        for (final String result : results) {
          // System.out.println(result);
          // /portal/poms/portal-1.2-SNAPSHOT.pom
          assertEquals(group + extension + "s" + artifact
              + "SNAPSHOT." + extension + hash, result);
        }
      }
    }
  }

  /**
   * Special Test for ejb-client jars as the need to be found in the
   * group/ejbs folder, not in the jars folder. Test with hashes and
   * signatures to make sure they end up in the right folder. Relates to
   * https://issues.sonatype.org/browse/NEXUS-2450
   */
  public void testTransformM2toM1JarEjbClient() {
    final String group = "/portal/";
    final String maven2Path = "portal-ejb/3.0.0-SNAPSHOT";
    final String artifact = "/portal-ejb-3.0.0-";
    final String timestamp = "20131207.174838-3";
    final String classifier = "-client.";

    // ejb-clients are only JARs but they must end up in the ejbs folder!
    final String extension = "jar";

    final List<String> extraFolders = new ArrayList<String>(1);
    extraFolders.add("ejbs");

    // artifact must end up under its extension
    // System.out.println("testing ejb-client jar");
    String path = group + maven2Path + artifact + timestamp + classifier
        + extension;
    List<String> results = layoutConverter.transformM2toM1(path,
        extraFolders);
    for (final String result : results) {
      // System.out.println(result);
      // "/portal/ejbs/portal-ejb-3.0.0-SNAPSHOT-client.jar
    }
    assertTrue(results
        .contains("/portal/ejbs/portal-ejb-3.0.0-SNAPSHOT-client.jar"));

    // md5 must end up with its artifact

    // try all kinds of hashes
    final Collection<String> hashes = new ArrayList<String>();
    hashes.add(".md5");
    hashes.add(".sha1");
    hashes.add(".asc");

    for (final String hash : hashes) {

      // hash must end up with its artifact
      path = group + maven2Path + artifact + timestamp + classifier
          + extension + hash;
      results = layoutConverter.transformM2toM1(path, extraFolders);
      for (final String result : results) {
        // System.out.println(result);
        // "/portal/ejbs/portal-ejb-3.0.0-SNAPSHOT-client.jar.HASHTYP
      }
      assertTrue(results
          .contains("/portal/ejbs/portal-ejb-3.0.0-SNAPSHOT-client.jar"
              + hash));
    }
  }
}
