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
package org.sonatype.nexus.coreui.internal.blobstore;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class S3FailoverStateContributorTest
    extends TestSupport
{
  @Mock
  private DatabaseCheck databaseCheck;

  private S3FailoverStateContributor underTest;

  @Test
  public void failoverAvailableNotZdu() {
    underTest = new S3FailoverStateContributor(databaseCheck, false);
    assertThat(underTest.getState(), hasEntry("S3FailoverEnabled", true));
    verifyNoInteractions(databaseCheck);
  }

  @Test
  public void failoverNotAvailableZduAndNotInVersion() {
    when(databaseCheck.isAtLeast("2.6")).thenReturn(false);
    underTest = new S3FailoverStateContributor(databaseCheck, true);
    assertThat(underTest.getState(), hasEntry("S3FailoverEnabled", false));
    verify(databaseCheck).isAtLeast("2.6");
    verifyNoMoreInteractions(databaseCheck);
  }

  @Test
  public void failoverAvailableZduAndMinimumVersion() {
    when(databaseCheck.isAtLeast("2.6")).thenReturn(true);
    underTest = new S3FailoverStateContributor(databaseCheck, true);
    assertThat(underTest.getState(), hasEntry("S3FailoverEnabled", true));
    verify(databaseCheck).isAtLeast("2.6");
    verifyNoMoreInteractions(databaseCheck);
  }
}
