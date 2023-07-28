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
package org.sonatype.nexus.repository;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class CleanupDryRunEvent
{
  public static final String FINISHED_AT_IN_MILLISECONDS = "finishedAt_in_milliseconds";

  public static final String STARTED_AT_IN_MILLISECONDS = "startedAt_in_milliseconds";

  private final Map<String, Object> reference;

  public CleanupDryRunEvent(final Map<String, Object> reference)
  {
    this.reference = checkNotNull(reference);
  }

  public Map<String, Object> getReference() {
    return reference;
  }

  @Override
  public String toString() {
    return getReference().toString();
  }
}
