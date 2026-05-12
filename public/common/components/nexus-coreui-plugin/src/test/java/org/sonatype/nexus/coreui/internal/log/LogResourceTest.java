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
package org.sonatype.nexus.coreui.internal.log;

import org.sonatype.nexus.common.log.LogMarker;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class LogResourceTest
{
  @Mock
  private LogMarker logMarker;

  @InjectMocks
  private LogResource underTest;

  @Test
  void markWithNullMessageUsesDefault() {
    underTest.mark(null);
    verify(logMarker).markLog(LogResource.DEFAULT_MARK);
  }

  @Test
  void markWithEmptyMessageUsesDefault() {
    underTest.mark("");
    verify(logMarker).markLog(LogResource.DEFAULT_MARK);
  }

  @Test
  void markWithCustomMessage() {
    String customMessage = "custom log mark";
    underTest.mark(customMessage);
    verify(logMarker).markLog(customMessage);
  }
}
