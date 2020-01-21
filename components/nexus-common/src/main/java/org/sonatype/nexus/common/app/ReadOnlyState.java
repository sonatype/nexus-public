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

import java.util.List;

import static java.lang.String.format;

/**
 * Summary object representing read-only state.
 *
 * @since 3.21
 */
public class ReadOnlyState
{
  private final List<FreezeRequest> state;

  private final boolean detailed;

  public ReadOnlyState(final List<FreezeRequest> state, final boolean detailed) {
    this.state = state;
    this.detailed = detailed;
  }

  public boolean isFrozen() {
    return !state.isEmpty();
  }

  public String getSummaryReason() {
    if (!detailed || state.isEmpty()) {
      return "";
    }

    return "Requested by " + state.stream()
        .filter(r -> !r.token().isPresent())
        .findAny()
        .map(u -> format("an administrator at %s", u.frozenAt().toString("yyyy-MM-dd HH:mm:ss ZZ")))
        .orElse(format("%s running system task(s)", state.size()));
  }

  public boolean isSystemInitiated() {
    return state.stream().anyMatch(r -> r.token().isPresent());
  }
}
