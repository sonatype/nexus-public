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
package org.sonatype.nexus.repository.view.handlers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.collect.AttributeKey;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.base.Stopwatch;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple timing handler.
 *
 * @since 3.0
 */
@Component
public class TimingHandler
    implements Handler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String ELAPSED_KEY = AttributeKey.get(TimingHandler.class, "elapsed");

  private final Handler meteringHandler;

  @Autowired
  public TimingHandler(@Qualifier("nexus.analytics.meteringHandler") @Nullable final Handler meteringHandler) {
    this.meteringHandler = meteringHandler;
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    Stopwatch watch = Stopwatch.createStarted();

    try {
      if (meteringHandler != null) {
        context.insertHandler(meteringHandler);
      }
      return context.proceed();
    }
    finally {
      String elapsed = watch.toString();
      context.getAttributes().set(ELAPSED_KEY, elapsed);
      log.trace("Timing: {}", elapsed);
    }
  }
}
