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
package org.sonatype.nexus.datastore.h2.task;

import org.sonatype.nexus.common.db.DatabaseCheck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link H2BackupTaskDescriptor}.
 *
 * <p>
 * Regression coverage for NEXUS-39391: the H2 backup task descriptor must only be
 * visible and exposed on H2-backed installations.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class H2BackupTaskDescriptorTest
{
  @Mock
  private DatabaseCheck databaseCheck;

  @Test
  public void isVisibleAndExposedOnH2() {
    when(databaseCheck.isPostgresql()).thenReturn(false);

    H2BackupTaskDescriptor descriptor = new H2BackupTaskDescriptor(databaseCheck);

    assertThat(descriptor.isVisible(), is(true));
    assertThat(descriptor.isExposed(), is(true));
  }

  @Test
  public void isHiddenAndNotExposedOnPostgres() {
    when(databaseCheck.isPostgresql()).thenReturn(true);

    H2BackupTaskDescriptor descriptor = new H2BackupTaskDescriptor(databaseCheck);

    assertThat(descriptor.isVisible(), is(false));
    assertThat(descriptor.isExposed(), is(false));
  }
}
