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
package org.sonatype.nexus.common.io;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.io.Cooperation.IOCheck;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

/**
 * Scaffolding for scoped {@link CooperationFactory} implementations.
 *
 * @since 3.14
 */
public abstract class ScopedCooperationFactorySupport
    extends CooperationFactorySupport
{
  @Override
  protected Cooperation build(final String id, final Config config) {
    return new ScopedCooperation(id, config);
  }

  /**
   * Creates a new {@link CooperatingFuture} for the given configuration.
   */
  protected <T> CooperatingFuture<T> createFuture(final String requestKey, final Config config) {
    return new CooperatingFuture<>(requestKey, config);
  }

  /**
   * Begins cooperation for the scoped key using the given future.
   *
   * @return {@code null} if the key was not already in use; otherwise the currently associated future
   */
  protected abstract <T> CooperatingFuture<T> beginCooperation(String scopedKey, CooperatingFuture<T> future);

  /**
   * Ends cooperation for the scoped key and its associated future.
   *
   * Each thread that successfully calls {@link #beginCooperation} will eventually call this with the same values.
   */
  protected abstract <T> void endCooperation(String scopedKey, CooperatingFuture<T> future);

  /**
   * Streams all futures that are currently cooperating.
   */
  protected abstract Stream<CooperatingFuture<?>> streamFutures(String scope);

  /**
   * Join cache results without retrying; assumes that any caches have no lag.
   */
  @Nullable
  protected <T> T join(final IOCheck<T> request) throws IOException {
    return request.check();
  }

  /**
   * {@link Cooperation} that's saved under a scoped partition of the {@link #futures()} map.
   */
  private class ScopedCooperation
      implements Cooperation
  {
    private final String scope;

    private final Config config;

    /**
     * @param id unique identifier for this cooperation point
     */
    public ScopedCooperation(final String id, final Config config) {
      this.scope = checkNotNull(id) + ':';
      this.config = checkNotNull(config);
    }

    @Override
    public <T> T cooperate(final String requestKey, final IOCall<T> request) throws IOException {
      CooperatingFuture<T> myFuture = createFuture(requestKey, config);
      String scopedKey = scope + requestKey;

      CooperatingFuture<T> theirFuture = beginCooperation(scopedKey, myFuture);
      if (theirFuture == null) {
        try {
          return myFuture.call(request); // we're the lead thread, go-ahead with the I/O request
        }
        finally {
          endCooperation(scopedKey, myFuture);
        }
      }
      else {
        return theirFuture.cooperate(request); // cooperatively wait for lead thread to complete
      }
    }

    @Override
    public <T> T join(final IOCheck<T> request) throws IOException {
      return ScopedCooperationFactorySupport.this.join(request);
    }

    @Override
    public Map<String, Integer> getThreadCountPerKey() {
      return streamFutures(scope).collect(toMap(CooperatingFuture::getRequestKey, CooperatingFuture::getThreadCount));
    }
  }
}
