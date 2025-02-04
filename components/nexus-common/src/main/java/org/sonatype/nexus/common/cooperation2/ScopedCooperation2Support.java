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
package org.sonatype.nexus.common.cooperation2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.cooperation2.datastore.internal.CooperatingFuture;
import org.sonatype.nexus.common.cooperation2.internal.Cooperation2Builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

/**
 * Support for class using basic local concurrency controls for {@link Cooperation2Factory}
 *
 * @since 3.41
 */
public abstract class ScopedCooperation2Support
    extends ComponentSupport
    implements Cooperation2
{
  private final ConcurrentMap<String, CooperatingFuture<?>> localFutures = new ConcurrentHashMap<>();

  protected final Config config;

  protected final String scope;

  protected ScopedCooperation2Support(final String scope, final Config config) {
    this.config = checkNotNull(config);
    this.scope = checkNotNull(scope);
  }

  @SuppressWarnings("unchecked")
  protected <T> CooperatingFuture<T> beginCooperation(final String scopedKey, final CooperatingFuture<T> future) {
    return (CooperatingFuture<T>) localFutures.putIfAbsent(scopedKey, future);
  }

  protected <T> void endCooperation(final String scopedKey, final CooperatingFuture<T> future) {
    localFutures.remove(scopedKey, future);
  }

  @Override
  public <RET> Builder<RET> on(final IOCall<RET> workFunction) {
    return new ScopedCooperation2Builder<>(workFunction);
  }

  @Override
  public Map<String, Integer> getThreadCountPerKey() {
    return localFutures.values()
        .stream()
        .collect(toMap(CooperatingFuture::getRequestKey, CooperatingFuture::getThreadCount));
  }

  public class ScopedCooperation2Builder<R>
      extends Cooperation2Builder<R>
  {
    protected CooperationKey cooperationKey;

    public ScopedCooperation2Builder(final IOCall<R> workFunction) {
      super(workFunction);
    }

    protected R perform(final Boolean failover) {
      try {
        Optional<R> potentialResult;
        if (failover && (potentialResult = checkFunction.check()).isPresent()) {
          return potentialResult.get();
        }

        return workFunction.call();
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public R cooperate(final String action, final String... nestedScope) throws IOException {
      cooperationKey = CooperationKey.create(scope, action, nestedScope);
      CooperatingFuture<R> myFuture = new CooperatingFuture<>(cooperationKey, config);
      String scopedKey = cooperationKey.getHashedKey();

      try {
        CooperatingFuture<R> theirFuture = beginCooperation(scopedKey, myFuture);
        if (theirFuture == null) {
          try {
            return myFuture.call(this::perform); // we're the lead thread, go-ahead with the I/O request
          }
          finally {
            endCooperation(scopedKey, myFuture);
          }
        }
        else {
          return theirFuture.cooperate(this::perform); // cooperatively wait for lead thread to complete
        }
      }
      catch (UncheckedIOException e) {
        throw e.getCause();
      }
    }
  }
}
