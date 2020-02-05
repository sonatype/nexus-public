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
package org.sonatype.nexus.security.user;

/**
 * @since 3.21
 */
public class DuplicateUserException
    extends RuntimeException
{
  private static final String ERROR_TEXT = "User %s already exists.";

  public DuplicateUserException(final String userId, final Throwable cause) {
    super(String.format(ERROR_TEXT, userId), cause);
  }

  public DuplicateUserException(final String userId) {
    this(userId, null);
  }
}
