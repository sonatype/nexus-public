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
package org.sonatype.nexus.repository.search;

import org.sonatype.nexus.rest.ValidationErrorsException;

/**
 * Rejects search parameters containing the NUL byte (0x00), which cannot bind to PostgreSQL
 * {@code ::tsquery}/{@code ::tsvector} and would otherwise surface as HTTP 500 (NEXUS-54105).
 */
public final class SearchFilterValidator
{
  private SearchFilterValidator() {
  }

  /**
   * Validates a single search-parameter value.
   *
   * @param value the value to check; {@code null} is accepted and treated as absent
   * @throws ValidationErrorsException if the value contains a NUL byte
   */
  public static void validate(final String value) {
    if (value == null) {
      return;
    }
    if (value.indexOf('\u0000') >= 0) {
      throw new ValidationErrorsException("Search parameters must not contain NUL (0x00) bytes");
    }
  }
}
