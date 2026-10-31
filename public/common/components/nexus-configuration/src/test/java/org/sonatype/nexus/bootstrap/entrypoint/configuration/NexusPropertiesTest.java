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
  // Deliberately synthetic keys so this suite never leaks into real feature-flag state, even
  // though the tests exercise setPropertyAndSyncSystem's system-properties write.
  private static final String KEY_A = "nexus.test.property.a";

  private static final String KEY_B = "nexus.test.property.b";

  private String priorKeyA;

  private String priorKeyB;

  @BeforeEach
  void snapshotSystemProperties() {
    priorKeyA = System.getProperty(KEY_A);
    priorKeyB = System.getProperty(KEY_B);
    System.clearProperty(KEY_A);
    System.clearProperty(KEY_B);
  }

  @AfterEach
  void restoreSystemProperties() {
    restore(KEY_A, priorKeyA);
    restore(KEY_B, priorKeyB);
  }

  @Test
  void setPropertyAndSyncSystem_writesToBothMapAndSystemProperties() {
    NexusProperties nexusProperties = new NexusProperties(new PropertyMap());

    nexusProperties.setPropertyAndSyncSystem(KEY_A, "true");
    nexusProperties.setPropertyAndSyncSystem(KEY_B, "value-b");

    assertThat(nexusProperties.getProperty(KEY_A), is("true"));
    assertThat(nexusProperties.getProperty(KEY_B), is("value-b"));
    assertThat(System.getProperty(KEY_A), is("true"));
    assertThat(System.getProperty(KEY_B), is("value-b"));
  }

  @Test
  void setPropertyAndSyncSystem_overridesExistingValuesInBothStores() {
    PropertyMap map = new PropertyMap();
    map.put(KEY_A, "false");
    System.setProperty(KEY_A, "false");
    NexusProperties nexusProperties = new NexusProperties(map);

    nexusProperties.setPropertyAndSyncSystem(KEY_A, "true");

    assertThat(nexusProperties.getProperty(KEY_A), is("true"));
    assertThat(System.getProperty(KEY_A), is("true"));
  }

  private static void restore(final String key, final String prior) {
    if (prior == null) {
      System.clearProperty(key);
    }
    else {
      System.setProperty(key, prior);
    }
  }
}
