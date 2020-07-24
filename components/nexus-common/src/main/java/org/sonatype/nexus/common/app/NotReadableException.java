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
package org.sonatype.nexus.common.app;

/**
 * Thrown when the application is unable to accept reads.
 *
 * This may be because the underlying storage is not accessible.
 *
 * @since 3.21
 */
public class NotReadableException
    extends IllegalStateException
{
  private static final long serialVersionUID = 3425411938965871948L;

  public NotReadableException(final String message) {
    super(message);
  }

  public NotReadableException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
