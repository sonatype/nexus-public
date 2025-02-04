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
package org.sonatype.nexus.common.entity;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getInteger;

/**
 * Helper functions for dealing with {@link Continuation}'s.
 *
 * @since 3.31
 */
public class Continuations
{
  private static final String ITERABLE_NON_NULL = "Iterable must be non-null";

  private static final String FUNCTION_NON_NULL = "Browse function must be non-null";

  public static final String LIMIT_NON_NEGATIVE = "Browse limit must be non-negative";

  private static final String PROPERTY_PREFIX = "nexus.continuation.browse.";

  public static final int BROWSE_LIMIT = getInteger(PROPERTY_PREFIX + "limit", 1_000);

  private Continuations() {
    // static util class
  }

  public static <T> Stream<T> streamOf(final Iterable<T> iterable) {
    checkArgument(iterable != null, ITERABLE_NON_NULL);
    return stream(iterable.spliterator(), false);
  }

  public static <T> Stream<T> streamOf(final BiFunction<Integer, String, Continuation<T>> browseFunction) {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    return streamOf(iterableOf(browseFunction));
  }

  public static <T> Stream<T> streamOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    return streamOf(iterableOf(browseFunction, limit));
  }

  public static <T> Stream<T> streamOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    return streamOf(iterableOf(browseFunction, limit, startToken));
  }

  public static <T> Iterable<T> iterableOf(final BiFunction<Integer, String, Continuation<T>> browseFunction) {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    return () -> iteratorOf(browseFunction);
  }

  public static <T> Iterable<T> iterableOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    return () -> iteratorOf(browseFunction, limit);
  }

  public static <T> Iterable<T> iterableOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    return () -> iteratorOf(browseFunction, limit, startToken);
  }

  public static <T> Iterator<T> iteratorOf(final BiFunction<Integer, String, Continuation<T>> browseFunction) {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    return iteratorOf(browseFunction, BROWSE_LIMIT);
  }

  public static <T> Iterator<T> iteratorOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit)
  {
    return iteratorOf(browseFunction, limit, null);
  }

  public static <T> Iterator<T> iteratorOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    return new Iterator<T>()
    {
      private Continuation<T> continuation = browseFunction.apply(limit, startToken);

      private Iterator<T> iterator = continuation.iterator();

      @Override
      public boolean hasNext() {
        if (continuation.isEmpty()) {
          return false;
        }
        else if (iterator.hasNext()) {
          return true;
        }
        else if (continuation.size() < limit) {
          // Optimization, if the number of returned results is less than the limit we provided, this indicates
          // that there were no more entries at the time of the query.
          return false;
        }
        else {
          return Optional.ofNullable(continuation.nextContinuationToken())
              .map(token -> {
                continuation = browseFunction.apply(limit, token);
                iterator = continuation.iterator();
                return iterator.hasNext();
              })
              .orElse(false);
        }
      }

      @Override
      public T next() {
        return iterator.next();
      }
    };
  }
}
