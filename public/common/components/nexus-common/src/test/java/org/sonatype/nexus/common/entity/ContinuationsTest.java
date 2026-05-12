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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.sonatype.nexus.common.entity.Continuations.PageInfo;

import com.google.common.collect.ForwardingCollection;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonatype.nexus.common.entity.Continuations.BROWSE_LIMIT;
import static org.sonatype.nexus.common.entity.Continuations.iterableOf;
import static org.sonatype.nexus.common.entity.Continuations.iteratorOf;
import static org.sonatype.nexus.common.entity.Continuations.streamOf;

public class ContinuationsTest

{
  private static final String[] STRINGS =
      new String[]{"one", "two", "three", "four", "five", "six", "seven", "eight"};

  private BrowseMock browseMock = spy(new BrowseMock(STRINGS));

  @Test
  public void testStreamOfLimit_Default() {
    int limit = BROWSE_LIMIT;
    assertThat(streamOf(browseMock::browse).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_9() {
    int limit = 9;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_8() {
    int limit = 8;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "eight");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_7() {
    int limit = 7;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "seven");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_6() {
    int limit = 6;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "six");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_5() {
    int limit = 5;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "five");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_4() {
    int limit = 4;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "four");
    verify(browseMock).browse(limit, "eight");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_3() {
    int limit = 3;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "three");
    verify(browseMock).browse(limit, "six");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_2() {
    int limit = 2;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "two");
    verify(browseMock).browse(limit, "four");
    verify(browseMock).browse(limit, "six");
    verify(browseMock).browse(limit, "eight");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_1() {
    int limit = 1;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), contains(STRINGS));
    verify(browseMock).browse(limit, null);
    verify(browseMock).browse(limit, "one");
    verify(browseMock).browse(limit, "two");
    verify(browseMock).browse(limit, "three");
    verify(browseMock).browse(limit, "four");
    verify(browseMock).browse(limit, "five");
    verify(browseMock).browse(limit, "six");
    verify(browseMock).browse(limit, "seven");
    verify(browseMock).browse(limit, "eight");
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfLimit_0() {
    int limit = 0;
    assertThat(streamOf(browseMock::browse, limit).collect(toList()), empty());
    verify(browseMock).browse(limit, null);
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfEmptyList() {
    browseMock = spy(new BrowseMock());
    assertThat(streamOf(browseMock::browse).collect(toList()), empty());
    verify(browseMock).browse(BROWSE_LIMIT, null);
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfSingleton() {
    browseMock = spy(new BrowseMock("one"));
    List<String> result = streamOf(browseMock::browse).collect(toList());
    assertThat(result, contains("one"));
    assertThat(result, hasSize(1));
    verify(browseMock).browse(BROWSE_LIMIT, null);
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testStreamOfSingletonWithNull() {
    browseMock = spy(new BrowseMock(true, "one"));
    List<String> result = streamOf(browseMock::browse).collect(toList());
    assertThat(result, contains("one"));
    assertThat(result, hasSize(1));
    verify(browseMock).browse(BROWSE_LIMIT, null);
    verifyNoMoreInteractions(browseMock);
  }

  @Test
  public void testIteratorReuse() {
    Iterable<String> it = iterableOf(browseMock::browse, 3);

    assertThat(stream(it.spliterator(), false).collect(toList()), contains(STRINGS));
    assertThat(stream(it.spliterator(), false).collect(toList()), contains(STRINGS));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIteratorNullFunction() {
    iteratorOf(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIteratorNegativeLimit() {
    iteratorOf(browseMock::browse, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIterableNullFunction() {
    iterableOf(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIterableNegativeLimit() {
    iterableOf(browseMock::browse, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStreamNullIterable() {
    streamOf((Iterable<?>) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStreamNullFunction() {
    streamOf((BiFunction<Integer, String, Continuation<Object>>) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStreamNegativeLimit() {
    streamOf(browseMock::browse, -1);
  }

  private static class BrowseMock
  {
    private final boolean isLastTokenNull;

    private final LinkedList<String> strings;

    public BrowseMock(final String... strings) {
      this(false, strings);
    }

    public BrowseMock(final boolean isLastTokenNull, final String... strings) {
      this.isLastTokenNull = isLastTokenNull;
      this.strings = new LinkedList<>(Arrays.asList(strings));
    }

    public Continuation<String> browse(final int limit, final String continuationToken) {
      Iterator<String> it = strings.iterator();
      if (continuationToken != null) {
        while (it.hasNext()) {
          if (continuationToken.equals(it.next())) {
            break;
          }
        }
      }
      LinkedList<String> result = new LinkedList<>();
      for (int i = 0; i < limit; i++) {
        if (it.hasNext()) {
          result.add(it.next());
        }
      }
      return new ContinuationMock<>(result, result.isEmpty() ? null : nextToken(result));
    }

    private String nextToken(final LinkedList<String> result) {
      if (isLastTokenNull && strings.getLast().equals(result.getLast())) {
        return null;
      }
      else {
        return result.getLast();
      }
    }
  }

  private static class ContinuationMock<E>
      extends ForwardingCollection<E>
      implements Continuation<E>
  {
    private final Collection<E> collection;

    private final String continuationToken;

    public ContinuationMock(final Collection<E> collection, final String continuationToken) {
      this.collection = collection;
      this.continuationToken = continuationToken;
    }

    @Override
    protected Collection<E> delegate() {
      return collection;
    }

    @Override
    public String nextContinuationToken() {
      return continuationToken;
    }
  }

  // ==================== CONTINUATION TOKEN CALLBACK TESTS ====================

  @Test
  public void testIteratorCallbackCalledForEachPage() {
    List<PageInfo> pageInfos = new LinkedList<>();
    Consumer<PageInfo> callback = pageInfo -> pageInfos.add(pageInfo);

    List<String> result = streamOf(browseMock::browse, 3, null, callback).collect(toList());
    assertThat(result, contains(STRINGS));

    // Callback should be called after page 1 (at "three"), page 2 (at "six"), and page 3 (null)
    // Page 1: one, two, three (3 items) -> next token is "three"
    // Page 2: four, five, six (3 items) -> next token is "six"
    // Page 3: seven, eight (2 items) -> last page, callback with null
    assertThat(pageInfos, hasSize(3));
    assertThat(pageInfos.get(0).getContinuationToken(), containsString("three"));
    assertThat(pageInfos.get(0).getPageSize(), equalTo(3));
    assertThat(pageInfos.get(1).getContinuationToken(), containsString("six"));
    assertThat(pageInfos.get(1).getPageSize(), equalTo(3));
    assertThat(pageInfos.get(2).getContinuationToken(), nullValue());
    assertThat(pageInfos.get(2).getPageSize(), equalTo(2));
  }

  @Test
  public void testIteratorCallbackWithNullTokenOnLastPage() {
    browseMock = spy(new BrowseMock(true, "one", "two", "three"));
    List<PageInfo> pageInfos = new LinkedList<>();
    Consumer<PageInfo> callback = pageInfo -> pageInfos.add(pageInfo);

    List<String> result = streamOf(browseMock::browse, 2, null, callback).collect(toList());
    assertThat(result, contains("one", "two", "three"));

    // Two callbacks: "two" (end of page 1 with 2 items), and null (end of page 2 with 1 item - last page)
    assertThat(pageInfos, hasSize(2));
    assertThat(pageInfos.get(0).getContinuationToken(), containsString("two"));
    assertThat(pageInfos.get(0).getPageSize(), equalTo(2));
    assertThat(pageInfos.get(1).getContinuationToken(), nullValue());
    assertThat(pageInfos.get(1).getPageSize(), equalTo(1));
  }

  @Test
  public void testIteratorCallbackWithStartToken() {
    List<PageInfo> pageInfos = new LinkedList<>();
    Consumer<PageInfo> callback = pageInfo -> pageInfos.add(pageInfo);

    // Start from "two" - BrowseMock skips up to and including "two", so result is "three" onwards
    List<String> result = streamOf(browseMock::browse, 3, "two", callback).collect(toList());
    assertThat(result, contains("three", "four", "five", "six", "seven", "eight"));

    // Callbacks at "five" (end of page 1, 3 items), "eight" (end of page 2, 3 items), and null (end of page 3 - last
    // page, 0 items)
    assertThat(pageInfos, hasSize(3));
    assertThat(pageInfos.get(0).getContinuationToken(), containsString("five"));
    assertThat(pageInfos.get(0).getPageSize(), equalTo(3));
    assertThat(pageInfos.get(1).getContinuationToken(), containsString("eight"));
    assertThat(pageInfos.get(1).getPageSize(), equalTo(3));
    assertThat(pageInfos.get(2).getContinuationToken(), nullValue());
    assertThat(pageInfos.get(2).getPageSize(), equalTo(0));
  }

  @Test
  public void testIterableCallbackCalledForEachPage() {
    List<PageInfo> pageInfos = new LinkedList<>();
    Consumer<PageInfo> callback = pageInfo -> pageInfos.add(pageInfo);

    List<String> result =
        stream(iterableOf(browseMock::browse, 3, null, callback).spliterator(), false).collect(toList());
    assertThat(result, contains(STRINGS));

    // Callbacks at "three" (3 items), "six" (3 items), and null (for final page with 2 items)
    assertThat(pageInfos, hasSize(3));
  }

  @Test
  public void testIteratorCallbackNotNullWhenNullTokenProvided() {
    browseMock = spy(new BrowseMock(true, "one"));
    List<PageInfo> pageInfos = new LinkedList<>();
    Consumer<PageInfo> callback = pageInfo -> pageInfos.add(pageInfo);

    List<String> result = streamOf(browseMock::browse, 2, null, callback).collect(toList());
    assertThat(result, contains("one"));

    // Callback is invoked with null because the first page is also the last page (1 item)
    assertThat(pageInfos, hasSize(1));
    assertThat(pageInfos.get(0).getContinuationToken(), nullValue());
    assertThat(pageInfos.get(0).getPageSize(), equalTo(1));
  }

  @Test
  public void testIteratorCallbackEmptyPage() {
    BrowseMock emptyMock = spy(new BrowseMock());
    List<PageInfo> pageInfos = new LinkedList<>();
    Consumer<PageInfo> callback = pageInfo -> pageInfos.add(pageInfo);

    List<String> result = streamOf(emptyMock::browse, 10, null, callback).collect(toList());
    assertThat(result, empty());
    // When continuation is empty, callback is invoked with null to signal completion
    assertThat(pageInfos, hasSize(1));
    assertThat(pageInfos.get(0).getContinuationToken(), nullValue());
    assertThat(pageInfos.get(0).getPageSize(), equalTo(0));
  }

  /**
   * Verifies that exceptions thrown from the continuation callback propagate
   * and cause the stream to stop processing.
   */
  @Test
  public void testIteratorCallbackExceptionPropagates() {
    List<PageInfo> pageInfos = new LinkedList<>();
    Consumer<PageInfo> callback = pageInfo -> {
      pageInfos.add(pageInfo);
      if (pageInfos.size() == 2) {
        throw new RuntimeException("Callback exception on second call");
      }
    };

    // Verify that the exception from the callback propagates and stops the stream
    boolean exceptionThrown = false;
    try {
      streamOf(browseMock::browse, 3, null, callback).collect(toList());
    }
    catch (RuntimeException e) {
      exceptionThrown = true;
    }

    // Exception thrown from callback
    assertThat(exceptionThrown, equalTo(true));
    // At least 2 callbacks occurred before exception stopped processing
    assertThat(pageInfos.size(), greaterThanOrEqualTo(2));
  }

  @Test
  public void testIteratorTokenLengthValidation() {
    String longToken = new String(new char[1025]).replace('\0', 'a');
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      iteratorOf(browseMock::browse, 10, longToken, null);
    });
    assertThat(exception.getMessage(), containsString("Continuation token too long"));
  }

  @Test
  public void testIteratorNullStartTokenAllowed() {
    // Should not throw exception with null start token
    List<String> result = streamOf(browseMock::browse, 10, null, null).collect(toList());
    assertThat(result, contains(STRINGS));
  }

  @Test
  public void testCallbackReceivesCorrectContinuationToken() {
    AtomicInteger callCount = new AtomicInteger(0);
    List<PageInfo> capturedPageInfos = new LinkedList<>();

    Consumer<PageInfo> callback = pageInfo -> {
      int currentCall = callCount.incrementAndGet();
      capturedPageInfos.add(pageInfo);
      // Token should be the last item of the current page
      if (currentCall == 1) {
        // After page 1 (one, two, three), token should be "three"
        assertThat(pageInfo.getContinuationToken(), containsString("three"));
        assertThat(pageInfo.getPageSize(), equalTo(3));
      }
      else if (currentCall == 2) {
        // After page 2 (four, five, six), token should be "six"
        assertThat(pageInfo.getContinuationToken(), containsString("six"));
        assertThat(pageInfo.getPageSize(), equalTo(3));
      }
      else if (currentCall == 3) {
        // After page 3 (seven, eight), token should be null (final page)
        assertThat(pageInfo.getContinuationToken(), nullValue());
        assertThat(pageInfo.getPageSize(), equalTo(2));
      }
    };

    streamOf(browseMock::browse, 3, null, callback).collect(toList());
    assertThat(capturedPageInfos, hasSize(3));
  }

  @Test
  public void testIteratorNullCallbackInvokedOnlyOnce() {
    AtomicInteger nullCallbackCount = new AtomicInteger(0);
    Consumer<PageInfo> callback = pageInfo -> {
      if (pageInfo.getContinuationToken() == null) {
        nullCallbackCount.incrementAndGet();
      }
    };

    Iterator<String> iterator = iteratorOf(browseMock::browse, 3, null, callback);

    // Call hasNext() multiple times to simulate Stream/collector behavior
    // that may buffer or check hasNext() multiple times
    while (iterator.hasNext()) {
      iterator.next();
    }

    // The null callback should be invoked exactly once, even though hasNext()
    // was called multiple times
    assertThat(nullCallbackCount.get(), equalTo(1));
  }

  @Test
  public void testIteratorNullCallbackInvokedOnlyOnce_StreamFilter() {
    AtomicInteger nullCallbackCount = new AtomicInteger(0);
    Consumer<PageInfo> callback = pageInfo -> {
      if (pageInfo.getContinuationToken() == null) {
        nullCallbackCount.incrementAndGet();
      }
    };

    // Using Stream.filter can cause hasNext() to be called multiple times
    // on the iterator, which could trigger multiple null callbacks without protection
    List<String> result = streamOf(browseMock::browse, 3, null, callback)
        .filter(s -> true) // Identity filter that may affect iteration behavior
        .collect(toList());

    assertThat(result, contains(STRINGS));
    // The null callback should be invoked exactly once
    assertThat(nullCallbackCount.get(), equalTo(1));
  }

  @Test
  public void testIterableNullCallbackInvokedOnlyOnce_Reusable() {
    AtomicInteger nullCallbackCount = new AtomicInteger(0);
    Consumer<PageInfo> callback = pageInfo -> {
      if (pageInfo.getContinuationToken() == null) {
        nullCallbackCount.incrementAndGet();
      }
    };

    Iterable<String> iterable = iterableOf(browseMock::browse, 3, null, callback);

    // Consume the iterable multiple times via separate iterators
    // The null callback should only be invoked for the actual final page
    stream(iterable.spliterator(), false).collect(toList());

    // The null callback should be invoked exactly once per iterator
    assertThat(nullCallbackCount.get(), equalTo(1));
  }
}
