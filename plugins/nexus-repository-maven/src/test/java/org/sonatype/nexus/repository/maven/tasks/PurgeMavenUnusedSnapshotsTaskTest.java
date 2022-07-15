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
package org.sonatype.nexus.repository.maven.tasks;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.VersionPolicy.MIXED;
import static org.sonatype.nexus.repository.maven.VersionPolicy.RELEASE;
import static org.sonatype.nexus.repository.maven.VersionPolicy.SNAPSHOT;

public class PurgeMavenUnusedSnapshotsTaskTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private MavenFacet mavenFacet;

  private final Type groupType = new GroupType();

  private final Type hostedType = new HostedType();

  private final Format maven2Format = new Maven2Format();

  @Mock
  private Format dockerFormat;

  private final PurgeMavenUnusedSnapshotsTask underTest =
      new PurgeMavenUnusedSnapshotsTask(groupType, hostedType, maven2Format);

  @Before
  public void setup() {
    when(repository.facet(MavenFacet.class)).thenReturn(mavenFacet);
  }

  @Test
  public void appliesToMavenHostedSnapshot() {
    when(repository.getFormat()).thenReturn(maven2Format);
    when(repository.getType()).thenReturn(hostedType);
    when(mavenFacet.getVersionPolicy()).thenReturn(SNAPSHOT);
    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  public void appliesToMavenGroupSnapshot() {
    when(repository.getFormat()).thenReturn(maven2Format);
    when(repository.getType()).thenReturn(groupType);
    when(mavenFacet.getVersionPolicy()).thenReturn(SNAPSHOT);
    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  public void appliesToMavenHostedMixed() {
    when(repository.getFormat()).thenReturn(maven2Format);
    when(repository.getType()).thenReturn(hostedType);
    when(mavenFacet.getVersionPolicy()).thenReturn(MIXED);
    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  public void appliesToMavenHostedNoVersionPolicy() {
    when(repository.getFormat()).thenReturn(maven2Format);
    when(repository.getType()).thenReturn(hostedType);
    when(mavenFacet.getVersionPolicy()).thenReturn(null);
    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  public void doesNotApplyToDockerGroupSnapshot() {
    when(repository.getFormat()).thenReturn(dockerFormat);
    when(repository.getType()).thenReturn(groupType);
    when(mavenFacet.getVersionPolicy()).thenReturn(SNAPSHOT);
    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  public void doesNotApplyToMavenHostedRelease() {
    when(repository.getFormat()).thenReturn(maven2Format);
    when(repository.getType()).thenReturn(hostedType);
    when(mavenFacet.getVersionPolicy()).thenReturn(RELEASE);
    assertThat(underTest.appliesTo(repository), is(false));
  }
}
