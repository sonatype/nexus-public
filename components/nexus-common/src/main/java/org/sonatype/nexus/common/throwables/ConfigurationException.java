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
package org.sonatype.nexus.common.throwables;

// FIXME: Remove this uber-generic over-used exception
// FIXME: Retained here in nexus-common only for short-term compatibility
// FIXME: To be removed in near future!

/**
 * Configuration exception.
 *
 * @deprecated No replacement, avoid using this exception
 */
@Deprecated
public class ConfigurationException
    extends RuntimeException
{
  public ConfigurationException() {
  }

  public ConfigurationException(final String message) {
    super(message);
  }

  public ConfigurationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ConfigurationException(final Throwable cause) {
    super(cause);
  }
}
