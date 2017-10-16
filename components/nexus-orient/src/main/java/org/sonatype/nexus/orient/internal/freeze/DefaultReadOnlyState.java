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
package org.sonatype.nexus.orient.internal.freeze;

import java.util.List;

import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.ReadOnlyState;

import static java.lang.String.format;
import static org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType.SYSTEM;
import static org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType.USER_INITIATED;

/**
 * Default {@link ReadOnlyState} implementation.
 */
class DefaultReadOnlyState
    implements ReadOnlyState
{

  private final List<FreezeRequest> state;

  private final boolean authorized;

  DefaultReadOnlyState(final List<FreezeRequest> state, final boolean authorized) {
    this.state = state;
    this.authorized = authorized;
  }

  @Override
  public boolean isFrozen() {
    return !state.isEmpty();
  }

  @Override
  public String getSummaryReason() {
    if (!authorized || state.isEmpty()) {
      return "";
    }

    return state.stream().filter(r -> USER_INITIATED.equals(r.getInitiatorType())).findAny()
        .map(u -> format("activated by an administrator at %s", u.getTimestamp().toString("yyyy-MM-dd HH:mm:ss ZZ")))
        .orElse(format("activated by %s running system task(s)", state.size()));
  }

  @Override
  public boolean isSystemInitiated() {
    return state.stream().anyMatch(r -> SYSTEM.equals(r.getInitiatorType()));
  }
}
