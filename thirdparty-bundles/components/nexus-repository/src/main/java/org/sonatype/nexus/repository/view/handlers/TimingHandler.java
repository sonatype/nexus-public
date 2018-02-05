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
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.AttributeKey;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.base.Stopwatch;

/**
 * Simple timing handler.
 *
 * @since 3.0
 */
@Named
@Singleton
public class TimingHandler
  extends ComponentSupport
  implements Handler
{
  public static final String ELAPSED_KEY = AttributeKey.get(TimingHandler.class, "elapsed");

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    Stopwatch watch = Stopwatch.createStarted();

    try {
      return context.proceed();
    }
    finally {
      String elapsed = watch.toString();
      context.getAttributes().set(ELAPSED_KEY, elapsed);
      log.trace("Timing: {}", elapsed);
    }
  }
}
