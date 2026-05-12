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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getInteger;

/**
 * Helper functions for dealing with {@link Continuation}'s.
 */
public class Continuations
{
  private static final Logger log = LoggerFactory.getLogger(Continuations.class);

  private static final String ITERABLE_NON_NULL = "Iterable must be non-null";

  private static final String FUNCTION_NON_NULL = "Browse function must be non-null";

  public static final String LIMIT_NON_NEGATIVE = "Browse limit must be non-negative";

  private static final String PROPERTY_PREFIX = "nexus.continuation.browse.";

  public static final int BROWSE_LIMIT = getInteger(PROPERTY_PREFIX + "limit", 1_000);

  private Continuations() {
    // static util class
  }

  /**
   * Empty continuation implementation for use when browse function fails.
   * This allows iteration to complete gracefully when fetching the next page fails.
   */
  private static class EmptyContinuation<T>
      extends AbstractCollection<T>
      implements Continuation<T>
  {
    @Override
    public Iterator<T> iterator() {
      return new Iterator<T>()
      {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public T next() {
          throw new NoSuchElementException();
        }
      };
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public String nextContinuationToken() {
      return null;
    }
  }

  /**
   * Page information passed to the continuation token consumer.
   */
  public static final class PageInfo
  {
    private final String continuationToken;

    private final int pageSize;

    public PageInfo(final String continuationToken, final int pageSize) {
      this.continuationToken = continuationToken;
      this.pageSize = pageSize;
    }

    /**
     * Returns the continuation token for the page that just completed, or null if this is the final page.
     * This token can be used to resume iteration from this point.
     */
    public String getContinuationToken() {
      return continuationToken;
    }

    /**
     * Returns the actual number of items in the completed page.
     */
    public int getPageSize() {
      return pageSize;
    }
  }

  public static <T> Stream<T> streamOf(final Iterable<T> iterable) {
    checkArgument(iterable != null, ITERABLE_NON_NULL);
    return stream(iterable.spliterator(), false);
  }

  public static <T> Stream<T> streamOf(final BiFunction<Integer, String, Continuation<T>> browseFunction) {
    return streamOf(browseFunction, BROWSE_LIMIT, null, null);
  }

  public static <T> Stream<T> streamOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit)
  {
    return streamOf(browseFunction, limit, null, null);
  }

  public static <T> Stream<T> streamOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken)
  {
    return streamOf(browseFunction, limit, startToken, null);
  }

  /**
   * Returns a stream that fetches pages using the browse function and invokes a callback
   * after each page is fully consumed and after fetching the next page.
   *
   * @param browseFunction function that fetches a page given a limit and continuation token
   * @param limit maximum number of items per page
   * @param startToken starting continuation token (null to start from beginning)
   * @param continuationTokenConsumer callback invoked with the continuation token and page size
   *          from the completed page (to be saved for resumption). Called after each page is fully
   *          consumed and after fetching the next page. May be null if no callback is needed.
   *          The token is null when the final page is complete. The continuation token provided
   *          to the callback represents the page that just completed and can be used to resume
   *          iteration from this point. Tokens from browseFunction are validated to prevent
   *          abnormally long tokens from causing issues.
   */
  public static <T> Stream<T> streamOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken,
      final Consumer<PageInfo> continuationTokenConsumer)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    return streamOf(iterableOf(browseFunction, limit, startToken, continuationTokenConsumer));
  }

  public static <T> Iterable<T> iterableOf(final BiFunction<Integer, String, Continuation<T>> browseFunction) {
    return iterableOf(browseFunction, BROWSE_LIMIT);
  }

  public static <T> Iterable<T> iterableOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit)
  {
    return iterableOf(browseFunction, limit, null, null);
  }

  public static <T> Iterable<T> iterableOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken)
  {
    return iterableOf(browseFunction, limit, startToken, null);
  }

  /**
   * Returns an iterable that fetches pages using the browse function and invokes a callback
   * after each page is fully consumed and after fetching the next page.
   *
   * @param browseFunction function that fetches a page given a limit and continuation token
   * @param limit maximum number of items per page
   * @param startToken starting continuation token (null to start from beginning)
   * @param continuationTokenConsumer callback invoked with the continuation token and page size
   *          from the completed page (to be saved for resumption). Called after each page is fully
   *          consumed and after fetching the next page. May be null if no callback is needed.
   *          The token is null when the final page is complete. The continuation token provided
   *          to the callback represents the page that just completed and can be used to resume
   *          iteration from this point. Tokens from browseFunction are validated to prevent
   *          abnormally long tokens from causing issues.
   */
  public static <T> Iterable<T> iterableOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken,
      final Consumer<PageInfo> continuationTokenConsumer)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    return () -> iteratorOf(browseFunction, limit, startToken, continuationTokenConsumer);
  }

  public static <T> Iterator<T> iteratorOf(final BiFunction<Integer, String, Continuation<T>> browseFunction) {
    return iteratorOf(browseFunction, BROWSE_LIMIT, null, null);
  }

  public static <T> Iterator<T> iteratorOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit)
  {
    return iteratorOf(browseFunction, limit, null, null);
  }

  public static <T> Iterator<T> iteratorOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken)
  {
    return iteratorOf(browseFunction, limit, startToken, null);
  }

  /**
   * Returns an iterator that fetches pages using the browse function and invokes a callback
   * after each page is fully consumed and after fetching the next page.
   *
   * @param browseFunction function that fetches a page given a limit and continuation token
   * @param limit maximum number of items per page
   * @param startToken starting continuation token (null to start from beginning)
   * @param continuationTokenConsumer callback invoked with the continuation token and page size
   *          from the completed page (to be saved for resumption). Called after each page is fully
   *          consumed and after fetching the next page. May be null if no callback is needed.
   *          The token is null when the final page is complete. The continuation token provided
   *          to the callback represents the page that just completed and can be used to resume
   *          iteration from this point. Tokens from browseFunction are validated to prevent
   *          abnormally long tokens from causing issues.
   */
  public static <T> Iterator<T> iteratorOf(
      final BiFunction<Integer, String, Continuation<T>> browseFunction,
      final int limit,
      final String startToken,
      final Consumer<PageInfo> continuationTokenConsumer)
  {
    checkArgument(browseFunction != null, FUNCTION_NON_NULL);
    checkArgument(limit >= 0, LIMIT_NON_NEGATIVE);
    checkArgument(startToken == null || startToken.length() <= 1024, "Continuation token too long");

    return new Iterator<>()
    {
      private Continuation<T> continuation = browseFunction.apply(limit, startToken);

      private Iterator<T> iterator = continuation.iterator();

      private boolean finalPageCallbackInvoked = false;

      @Override
      public boolean hasNext() {
        if (continuation.isEmpty()) {
          // Single empty continuation - invoke the complete callback
          // The callback will only fire once due to finalPageCallbackInvoked flag
          maybeTriggerConsumerForComplete();
          return false;
        }
        else if (iterator.hasNext()) {
          return true;
        }
        else if (continuation.size() < limit) {
          // Optimization, if the number of returned results is less than the limit we provided, this indicates
          // that there were no more entries at the time of the query.
          // Trigger the complete callback to indicate the final page is complete
          maybeTriggerConsumerForComplete();
          return false;
        }
        else {
          return Optional.ofNullable(continuation.nextContinuationToken())
              .map(token -> {
                // Validate token from browseFunction for length and dangerous characters
                if (token.length() > 1024) {
                  log.warn("Continuation token too long ({} chars), stopping iteration", token.length());
                  maybeTriggerConsumerForComplete();
                  return false;
                }
                // Store the current continuation info before fetching the next page
                int completedPageSize = continuation.size();
                try {
                  continuation = browseFunction.apply(limit, token);
                }
                catch (Exception e) {
                  log.warn(
                      "Failed to fetch next page with continuation token '{}', stopping iteration",
                      token, e);
                  continuation = new EmptyContinuation<>();
                }
                iterator = continuation.iterator();
                // Consume the continuation token after the page has been fetched and iterator updated.
                // The token represents the page that just completed.
                maybeTriggerConsumerForPage(token, completedPageSize);
                // If the next continuation is empty, trigger the complete callback
                if (continuation.isEmpty()) {
                  maybeTriggerConsumerForComplete();
                  return false;
                }
                return iterator.hasNext();
              })
              .orElse(false);
        }
      }

      @Override
      public T next() {
        return iterator.next();
      }

      /**
       * Invokes the continuation token consumer with the completed page's token and size.
       * This is called after a page is fully consumed and after fetching the next page.
       *
       * @param token the continuation token for the completed page
       * @param pageSize the actual number of items in the completed page
       */
      private void maybeTriggerConsumerForPage(final String token, final int pageSize) {
        if (continuationTokenConsumer != null) {
          continuationTokenConsumer.accept(new PageInfo(token, pageSize));
        }
      }

      /**
       * Invokes the continuation token consumer with null token to indicate completion.
       * This is called when there are no more pages to process, or when an error occurs.
       * The callback is only invoked once due to the finalPageCallbackInvoked flag.
       */
      private void maybeTriggerConsumerForComplete() {
        if (continuationTokenConsumer != null && !finalPageCallbackInvoked) {
          finalPageCallbackInvoked = true;
          continuationTokenConsumer.accept(new PageInfo(null, continuation.size()));
        }
      }
    };
  }
}
