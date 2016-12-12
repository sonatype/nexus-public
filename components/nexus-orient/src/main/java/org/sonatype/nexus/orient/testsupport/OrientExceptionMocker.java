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
package org.sonatype.nexus.orient.testsupport;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Helper class to simplify mocking of Orient exceptions.
 *
 * @since 3.2
 */
public class OrientExceptionMocker
{
  private OrientExceptionMocker() {
    // empty
  }

  /**
   * Mocks the given Orient exception type so it can participate in unit tests without
   * depending on internal state such as {@link ODatabaseRecordThreadLocal#INSTANCE}.
   */
  public static <E extends OException> E mockOrientException(final Class<E> exceptionType) {
    E exception = mock(exceptionType);
    when(exception.getStackTrace()).thenReturn(new StackTraceElement[0]);
    return exception;
  }
}
