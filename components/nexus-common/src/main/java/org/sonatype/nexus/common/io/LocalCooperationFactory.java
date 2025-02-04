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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Supplies local {@link Cooperation} points.
 *
 * @since 3.14
 */
@Named("local")
@Singleton
public class LocalCooperationFactory
    extends ScopedCooperationFactorySupport
{
  private final ConcurrentMap<String, CooperatingFuture<?>> localFutures = new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  protected <T> CooperatingFuture<T> beginCooperation(final String scopedKey, final CooperatingFuture<T> future) {
    return (CooperatingFuture<T>) localFutures.putIfAbsent(scopedKey, future);
  }

  @Override
  protected <T> void endCooperation(final String scopedKey, final CooperatingFuture<T> future) {
    localFutures.remove(scopedKey, future);
  }

  @Override
  protected Stream<CooperatingFuture<?>> streamFutures(final String scope) {
    return localFutures.entrySet()
        .stream()
        .filter(entry -> entry.getKey().startsWith(scope))
        .map(Entry::getValue);
  }
}
