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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.oss.PaymentRequiredException;
import org.sonatype.nexus.common.oss.circuit.ContentUsageCircuitBreaker;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A format-neutral payment required handler.
 * If {@link ContentUsageCircuitBreaker} is open then {@link PaymentRequiredException} will be thrown.
 */
@Named
@Singleton
public class PaymentRequiredHandler
    extends ComponentSupport
    implements Handler
{
  private final ContentUsageCircuitBreaker circuitBreaker;

  @Inject
  public PaymentRequiredHandler(final ContentUsageCircuitBreaker circuitBreaker) {
    this.circuitBreaker = checkNotNull(circuitBreaker);
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    if (circuitBreaker.isClosed()) {
      return context.proceed();
    }
    if (isPaymentRequired(context)) {
      throw new PaymentRequiredException("Payment Required");
    }
    return context.proceed();
  }

  public boolean isPaymentRequired(@Nonnull final Context context) {
    String action = context.getRequest().getAction();
    if (HttpMethods.PUT.equals(action)) {
      return true;
    }
    return false;
  }
}
