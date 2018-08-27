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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

/**
 * Scaffolding for scoped {@link CooperationFactory} implementations.
 *
 * @since 3.next
 */
public abstract class ScopedCooperationFactorySupport
    extends CooperationFactorySupport
{
  @Override
  protected Cooperation build(final String id, final Config config) {
    return new ScopedCooperation(id, config);
  }

  protected <T> CooperatingFuture<T> createFuture(final String requestKey, final Config config) {
    return new CooperatingFuture<>(requestKey, config);
  }

  protected abstract <T> CooperatingFuture<T> putFuture(final String scopedKey, final CooperatingFuture<T> future);

  protected abstract <T> void removeFuture(final String scopedKey, final CooperatingFuture<T> future);

  protected abstract Stream<CooperatingFuture<?>> streamFutures(final String scope);

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

      CooperatingFuture<T> theirFuture = putFuture(scopedKey, myFuture);
      if (theirFuture == null) {
        try {
          return myFuture.call(request); // we're the lead thread, go-ahead with the I/O request
        }
        finally {
          removeFuture(scopedKey, myFuture);
        }
      }
      else {
        return theirFuture.cooperate(request); // cooperatively wait for lead thread to complete
      }
    }

    @Override
    public Map<String, Integer> getThreadCountPerKey() {
      return streamFutures(scope).collect(toMap(CooperatingFuture::getRequestKey, CooperatingFuture::getThreadCount));
    }
  }
}
