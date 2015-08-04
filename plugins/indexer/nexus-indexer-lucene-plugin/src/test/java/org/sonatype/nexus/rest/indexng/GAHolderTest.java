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
package org.sonatype.nexus.rest.indexng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.nexus.rest.model.NexusNGArtifact;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * UT for GAHolder.
 *
 * @author ifedorenko :P
 */
public class GAHolderTest
    extends TestSupport
{

  private final VersionScheme versionScheme = new GenericVersionScheme();

  protected StringVersion fromString(String version) {
    try {
      final Version parsedVersion = versionScheme.parseVersion(version);
      return new StringVersion(version, parsedVersion);
    }
    catch (InvalidVersionSpecificationException e) {
      // this actually never happens, see GenericVersionScheme imple
      throw new IllegalStateException("Huh?", e);
    }
  }

  protected List<String> fromCollection(final Collection<NexusNGArtifact> coll) {
    final ArrayList<String> result = Lists.newArrayList();
    for (NexusNGArtifact nxhit : coll) {
      result.add(nxhit.getVersion());
    }
    return result;
  }

  public GAHolder prepare(String... versions) {
    final GAHolder result = new GAHolder();
    for (String version : versions) {
      final NexusNGArtifact nxhit = new NexusNGArtifact();
      nxhit.setVersion(version);
      final StringVersion sv = fromString(nxhit.getVersion());
      result.putVersionHit(sv, nxhit);
    }
    return result;
  }

  @Test
  public void releasesOnly() {
    {
      final GAHolder gaHolder = prepare("1.0", "1.1", "1.2");
      assertThat(gaHolder.getLatestSnapshot(), is(nullValue()));
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()), contains("1.2", "1.1", "1.0"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
    {
      final GAHolder gaHolder = prepare("1.1", "1.0", "1.2");
      assertThat(gaHolder.getLatestSnapshot(), is(nullValue()));
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()), contains("1.2", "1.1", "1.0"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
    {
      final GAHolder gaHolder = prepare("1.1-SONATYPE", "1.0", "1.2");
      assertThat(gaHolder.getLatestSnapshot(), is(nullValue()));
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()), contains("1.2", "1.1-SONATYPE", "1.0"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
    {
      final GAHolder gaHolder = prepare("1.1", "1.1-SONATYPE", "1.0", "1.2");
      assertThat(gaHolder.getLatestSnapshot(), is(nullValue()));
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2", "1.1-SONATYPE", "1.1", "1.0"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
  }

  /**
   * Since this class is fed from MI, that uses baseVersion, not using timestamped versions.
   */
  @Test
  public void snapshotsOnly() {
    {
      final GAHolder gaHolder = prepare("1.0-SNAPSHOT", "1.1-SNAPSHOT", "1.2-SNAPSHOT");
      assertThat(gaHolder.getLatestRelease(), is(nullValue()));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2-SNAPSHOT", "1.1-SNAPSHOT", "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestSnapshot())));
    }
    {
      final GAHolder gaHolder = prepare("1.1-SNAPSHOT", "1.0-SNAPSHOT", "1.2-SNAPSHOT");
      assertThat(gaHolder.getLatestRelease(), is(nullValue()));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2-SNAPSHOT", "1.1-SNAPSHOT", "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestSnapshot())));
    }
    {
      final GAHolder gaHolder = prepare("1.1-SONATYPE-SNAPSHOT", "1.0-SNAPSHOT", "1.2-SNAPSHOT");
      assertThat(gaHolder.getLatestRelease(), is(nullValue()));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2-SNAPSHOT", "1.1-SONATYPE-SNAPSHOT", "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestSnapshot())));
    }
    {
      final GAHolder gaHolder =
          prepare("1.1-SNAPSHOT", "1.1-SONATYPE-SNAPSHOT", "1.0-SNAPSHOT", "1.2-SNAPSHOT");
      assertThat(gaHolder.getLatestRelease(), is(nullValue()));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2-SNAPSHOT", "1.1-SONATYPE-SNAPSHOT", "1.1-SNAPSHOT", "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestSnapshot())));
    }
  }

  /**
   * Since this class is fed from MI, that uses baseVersion, not using timestamped versions.
   */
  @Test
  public void mixedVersion() {
    {
      final GAHolder gaHolder = prepare("1.0-SNAPSHOT", "1.1-SNAPSHOT", "1.2-SNAPSHOT", "1.1", "1.0", "1.2");
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2", "1.2-SNAPSHOT", "1.1", "1.1-SNAPSHOT", "1.0", "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
    {
      final GAHolder gaHolder = prepare("1.1-SNAPSHOT", "1.0-SNAPSHOT", "1.2-SNAPSHOT", "1.1", "1.0", "1.2");
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2", "1.2-SNAPSHOT", "1.1", "1.1-SNAPSHOT", "1.0", "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
    {
      final GAHolder gaHolder =
          prepare("1.1-SONATYPE-SNAPSHOT", "1.0-SNAPSHOT", "1.2-SNAPSHOT", "1.1-SONATYPE", "1.0", "1.2");
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2", "1.2-SNAPSHOT", "1.1-SONATYPE", "1.1-SONATYPE-SNAPSHOT", "1.0",
              "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
    {
      final GAHolder gaHolder =
          prepare("1.1-SNAPSHOT", "1.1-SONATYPE-SNAPSHOT", "1.0-SNAPSHOT", "1.2-SNAPSHOT", "1.1", "1.1-SONATYPE",
              "1.0", "1.2");
      assertThat(gaHolder.getLatestRelease().getVersion(), is("1.2"));
      assertThat(gaHolder.getLatestSnapshot().getVersion(), is("1.2-SNAPSHOT"));
      assertThat(fromCollection(gaHolder.getOrderedVersionHits()),
          contains("1.2", "1.2-SNAPSHOT", "1.1-SONATYPE", "1.1-SONATYPE-SNAPSHOT", "1.1", "1.1-SNAPSHOT",
              "1.0", "1.0-SNAPSHOT"));
      assertThat(System.identityHashCode(gaHolder.getLatestVersionHit()), is(
          System.identityHashCode(gaHolder.getLatestRelease())));
    }
  }

}
