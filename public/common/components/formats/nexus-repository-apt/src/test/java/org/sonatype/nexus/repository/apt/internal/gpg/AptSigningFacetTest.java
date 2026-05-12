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
package org.sonatype.nexus.repository.apt.internal.gpg;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AptSigningFacet}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AptSigningFacetTest
{
  @Mock
  private Repository repository;

  @Mock
  private ConfigurationFacet configurationFacet;

  @Mock
  private Configuration configuration;

  private AptSigningFacet underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new AptSigningFacet();
    underTest.attach(repository);

    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
  }

  @Test
  public void testIsConfigured_WithValidKeypair_ReturnsTrue() throws Exception {
    // Arrange
    AptSigningFacet.Config config = new AptSigningFacet.Config();
    config.keypair = "valid-keypair";
    config.passphrase = "passphrase";

    when(configurationFacet.readSection(any(), eq(AptSigningFacet.CONFIG_KEY), eq(AptSigningFacet.Config.class)))
        .thenReturn(config);

    underTest.doConfigure(configuration);

    // Act
    boolean result = underTest.isConfigured();

    // Assert
    assertThat(result, is(true));
  }

  @Test
  public void testIsConfigured_WithNullKeypair_ReturnsFalse() throws Exception {
    // Arrange
    AptSigningFacet.Config config = new AptSigningFacet.Config();
    config.keypair = null; // This is the NPE scenario
    config.passphrase = "passphrase";

    when(configurationFacet.readSection(any(), eq(AptSigningFacet.CONFIG_KEY), eq(AptSigningFacet.Config.class)))
        .thenReturn(config);

    underTest.doConfigure(configuration);

    // Act
    boolean result = underTest.isConfigured();

    // Assert
    assertThat("Should handle null keypair without NPE", result, is(false));
  }

  @Test
  public void testIsConfigured_WithEmptyKeypair_ReturnsFalse() throws Exception {
    // Arrange
    AptSigningFacet.Config config = new AptSigningFacet.Config();
    config.keypair = "";
    config.passphrase = "passphrase";

    when(configurationFacet.readSection(any(), eq(AptSigningFacet.CONFIG_KEY), eq(AptSigningFacet.Config.class)))
        .thenReturn(config);

    underTest.doConfigure(configuration);

    // Act
    boolean result = underTest.isConfigured();

    // Assert
    assertThat(result, is(false));
  }

  @Test
  public void testIsConfigured_WithBlankKeypair_ReturnsFalse() throws Exception {
    // Arrange
    AptSigningFacet.Config config = new AptSigningFacet.Config();
    config.keypair = "   "; // Only whitespace
    config.passphrase = "passphrase";

    when(configurationFacet.readSection(any(), eq(AptSigningFacet.CONFIG_KEY), eq(AptSigningFacet.Config.class)))
        .thenReturn(config);

    underTest.doConfigure(configuration);

    // Act
    boolean result = underTest.isConfigured();

    // Assert
    assertThat(result, is(false));
  }

  @Test
  public void testIsConfigured_WithNullConfig_ReturnsFalse() throws Exception {
    // Arrange
    when(configurationFacet.readSection(any(), eq(AptSigningFacet.CONFIG_KEY), eq(AptSigningFacet.Config.class)))
        .thenReturn(null);

    underTest.doConfigure(configuration);

    // Act
    boolean result = underTest.isConfigured();

    // Assert
    assertThat(result, is(false));
  }

  @Test
  public void testIsConfigured_BeforeConfigure_ReturnsFalse() {
    // Act - isConfigured() called before doConfigure()
    boolean result = underTest.isConfigured();

    // Assert
    assertThat("Should return false when config not initialized", result, is(false));
  }
}
