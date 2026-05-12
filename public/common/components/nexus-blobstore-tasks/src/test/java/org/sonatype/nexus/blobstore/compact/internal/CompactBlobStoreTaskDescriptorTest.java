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
package org.sonatype.nexus.blobstore.compact.internal;

import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.scheduling.TaskDescriptorSupport.MULTINODE_KEY;

class CompactBlobStoreTaskDescriptorTest

{
  private CompactBlobStoreTaskDescriptor underTest;

  private TaskConfiguration taskConfiguration = new TaskConfiguration();

  @Test
  void initializeConfiguration() {
    underTest = new CompactBlobStoreTaskDescriptor(true);
    underTest.initializeConfiguration(taskConfiguration);
    assertThat(taskConfiguration.getBoolean(MULTINODE_KEY, false), is(false));
  }

  @Test
  void testVisibleAndExposedWhenEnabled() {
    underTest = new CompactBlobStoreTaskDescriptor(true);

    assertThat("Task should be visible when enabled", underTest.isVisible(), is(true));
    assertThat("Task should be exposed when enabled", underTest.isExposed(), is(true));
  }

  @Test
  void testNotVisibleAndNotExposedWhenDisabled() {
    underTest = new CompactBlobStoreTaskDescriptor(false);

    assertThat("Task should not be visible when disabled", underTest.isVisible(), is(false));
    assertThat("Task should not be exposed when disabled", underTest.isExposed(), is(false));
  }
}
