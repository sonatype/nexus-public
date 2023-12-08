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
package org.sonatype.nexus.repository.content.fluent;

import java.util.Collection;

import org.sonatype.nexus.common.entity.Continuation;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingCollection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;

/**
 * Fluent {@link Continuation}s.
 *
 * @since 3.24
 */
public class FluentContinuation<E, T>
    extends ForwardingCollection<E>
    implements Continuation<E>
{
  private final Continuation<T> continuation;

  private final Collection<E> fluentCollection;

  public FluentContinuation(final Continuation<T> continuation, final Function<T, E> toFluent) {
    this.continuation = checkNotNull(continuation);
    this.fluentCollection = transform(continuation, toFluent);
  }

  @Override
  protected Collection<E> delegate() {
    return fluentCollection;
  }

  @Override
  public String nextContinuationToken() {
    return continuation.nextContinuationToken();
  }
}
