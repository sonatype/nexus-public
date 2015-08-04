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
package org.sonatype.nexus.test.utils;

import org.sonatype.nexus.test.utils.NexusRequestMatchers.HasCode;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsError;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsSuccess;

import org.hamcrest.Factory;

public class StatusMatchers
{

  @Factory
  public static <T> IsSuccess isSuccess() {
    return new IsSuccess();
  }

  @Factory
  public static <T> IsError isError() {
    return new IsError();
  }

  @Factory
  public static <T> HasCode hasStatusCode(int expectedCode) {
    return new HasCode(expectedCode);
  }

  @Factory
  public static <T> HasCode isNotFound() {
    return new HasCode(404);
  }
}
