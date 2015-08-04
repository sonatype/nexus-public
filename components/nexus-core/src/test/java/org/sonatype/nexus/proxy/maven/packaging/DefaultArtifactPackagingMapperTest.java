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
package org.sonatype.nexus.proxy.maven.packaging;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

public class DefaultArtifactPackagingMapperTest
    extends TestSupport
{
  @Mock
  private NexusConfiguration nexusConfiguration;
  
  @Before
  public void prepare() {
    when(nexusConfiguration.getConfigurationDirectory()).thenReturn(util.createTempDir());
  }
  
  @Test
  public void defaults() {
    final ArtifactPackagingMapper apm = new DefaultArtifactPackagingMapper(nexusConfiguration);
    assertThat(apm.getExtensionForPackaging("jar"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("ejb-client"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("ejb"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("rar"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("par"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("maven-plugin"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("maven-archetype"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("plexus-application"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("eclipse-plugin"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("eclipse-feature"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("eclipse-application"), equalTo("zip"));
    assertThat(apm.getExtensionForPackaging("nexus-plugin"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("java-source"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("javadoc"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("test-jar"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("bundle"), equalTo("jar"));
  }

  @Test
  public void overrides() {
    final ArtifactPackagingMapper apm = new DefaultArtifactPackagingMapper(nexusConfiguration);
    apm.setPropertiesFile(util.resolveFile("src/test/resources/packaging2extension-mapping.properties"));
    assertThat(apm.getExtensionForPackaging("jar"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("ejb-client"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("ejb"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("rar"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("par"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("maven-plugin"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("maven-archetype"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("plexus-application"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("eclipse-plugin"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("eclipse-feature"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("eclipse-application"), equalTo("zip"));
    assertThat(apm.getExtensionForPackaging("nexus-plugin"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("java-source"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("javadoc"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("test-jar"), equalTo("jar"));
    assertThat(apm.getExtensionForPackaging("bundle"), equalTo("foo")); // overridden!
    assertThat(apm.getExtensionForPackaging("one"), equalTo("1")); // user specified
    assertThat(apm.getExtensionForPackaging("two"), equalTo("2")); // user specified
  }
}
