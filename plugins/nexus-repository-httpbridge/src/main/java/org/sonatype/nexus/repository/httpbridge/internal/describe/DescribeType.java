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
package org.sonatype.nexus.repository.httpbridge.internal.describe;

import org.sonatype.nexus.common.text.Strings2;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describe type.
 *
 * @since 3.0
 */
public enum DescribeType
{
  HTML,
  JSON;

  /**
   * Parse type from flags.  Either explicit HTML or JSON, or anything else will default to HTML.
   */
  public static DescribeType parse(String flags) {
    checkNotNull(flags);
    flags = Strings2.upper(flags).trim();
    if (flags.isEmpty()) {
      return HTML;
    }
    try {
      return valueOf(flags);
    }
    catch (IllegalArgumentException e) {
      return HTML;
    }
  }
}
