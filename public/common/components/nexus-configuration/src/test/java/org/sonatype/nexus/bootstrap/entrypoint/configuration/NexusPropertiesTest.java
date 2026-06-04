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
package org.sonatype.nexus.bootstrap.entrypoint.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class NexusPropertiesTest
{
  private static final String ANALYTICS_PROPERTY = "nexus.analytics.enabled";

  private static final String TRUE = "true";

  @BeforeEach
  void setUp() {
    System.clearProperty(ANALYTICS_PROPERTY);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty(ANALYTICS_PROPERTY);
  }

  @Test
  void enforceCommunityEditionAnalytics_SetsPropertyToTrue() {
    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    assertThat(nexusProperties.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  @Test
  void enforceCommunityEditionAnalytics_SetsSystemPropertyToTrue() {
    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    assertThat(System.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  @Test
  void enforceCommunityEditionAnalytics_OverridesExistingFalseValue() {
    System.setProperty(ANALYTICS_PROPERTY, "false");

    NexusProperties nexusProperties = createNexusProperties();

    nexusProperties.enforceCommunityEditionAnalytics();

    assertThat(nexusProperties.getProperty(ANALYTICS_PROPERTY), is(TRUE));
    assertThat(System.getProperty(ANALYTICS_PROPERTY), is(TRUE));
  }

  private NexusProperties createNexusProperties() {
    return new NexusProperties(new PropertyMap());
  }
}
