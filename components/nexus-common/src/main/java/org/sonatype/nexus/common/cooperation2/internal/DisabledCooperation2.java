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
package org.sonatype.nexus.common.cooperation2.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.CooperationKey;
import org.sonatype.nexus.common.cooperation2.IOCall;

import com.google.common.base.Stopwatch;

/**
 * An implementation of {@link Cooperation2} which doesn't use any concurrency control and each thread proceeds
 * individually. This is used when co-operation is disabled.
 *
 * @since 3.41
 */
public class DisabledCooperation2
    extends ComponentSupport
    implements Cooperation2
{
  private final String scope;

  public DisabledCooperation2(final String scope) {
    this.scope = scope;
  }

  @Override
  public <RET> Builder<RET> on(final IOCall<RET> workFunction) {
    return new DisabledCooperation2Builder<>(workFunction);
  }

  @Override
  public Map<String, Integer> getThreadCountPerKey() {
    return Collections.emptyMap();
  }

  private class DisabledCooperation2Builder<R>
      extends Cooperation2Builder<R>
  {
    DisabledCooperation2Builder(final IOCall<R> workFunction) {
      super(workFunction);
    }

    @Override
    public R cooperate(final String action, final String... nestedScope) throws IOException {
      CooperationKey cooperationKey = CooperationKey.create(scope, action, nestedScope);
      log.debug("Starting work for {}", cooperationKey);
      Stopwatch timer = Stopwatch.createStarted();

      try {
        return workFunction.call();
      }
      finally {
        log.debug("Completed work for {} in {}", cooperationKey, timer.elapsed());
      }
    }
  }
}
