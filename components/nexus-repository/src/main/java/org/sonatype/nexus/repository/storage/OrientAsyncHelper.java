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
package org.sonatype.nexus.repository.storage;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLNonBlockingQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class to wrap Orient SQL SELECT queries into {@link Iterable}s.
 *
 * @since 3.0
 */
class OrientAsyncHelper
{
  private static final int BUFFER_SIZE = 128;

  private static final long TIMEOUT_SECONDS = 60L;

  private OrientAsyncHelper() {
    // private
  }

  /**
   * Executes with passed in {@link ODatabaseDocumentTx} connection the passed in {@link String} SELECT SQL command
   * and returns a true asynchronous backed {@link Iterable} of {@link ODocument} instance, that can be later
   * transformed into whatever caller needs. This means that this wrapper supports ANY SQL commands, even those
   * performing aggregation on results, as long as iterator is not accumulated into a real array or list. Still, given
   * the nature of non-blocking query, it's execution happens on a separate thread, hence, it will not see uncommited
   * changes from the caller thread, if any. It uses default buffer size of 128 and timeout of 60 seconds.
   *
   * @param db          The {@link ODatabaseDocumentTx} database instance
   * @param selectQuery The SQL SELECT query in full
   * @param parameters  The SQL SELECT named parameters (optional)
   */
  public static Iterable<ODocument> asyncIterable(final ODatabaseDocumentTx db,
                                                  final String selectQuery,
                                                  @Nullable final Map<String, Object> parameters)
  {
    return asyncIterable(db, selectQuery, parameters, BUFFER_SIZE, TIMEOUT_SECONDS);
  }

  /**
   * Executes with passed in {@link ODatabaseDocumentTx} connection the passed in {@link String} SELECT SQL command
   * and returns a true asynchronous backed {@link Iterable} of {@link ODocument} instance, that can be later
   * transformed into whatever caller needs. This means that this wrapper supports ANY SQL commands, even those
   * performing aggregation on results, as long as iterator is not accumulated into a real array or list. Still, given
   * the nature of non-blocking query, it's execution happens on a separate thread, hence, it will not see uncommited
   * changes from the caller thread, if any. This method allows to tune buffer queue size and timeout.
   *
   * @param db             The {@link ODatabaseDocumentTx} database instance
   * @param selectQuery    The SQL SELECT query in full
   * @param parameters     The SQL SELECT named parameters (optional)
   * @param bufferSize     The queue buffer size
   * @param timeoutSeconds The timeout (in seconds) on queue handoff operations
   */
  public static Iterable<ODocument> asyncIterable(final ODatabaseDocumentTx db,
                                                  final String selectQuery,
                                                  @Nullable final Map<String, Object> parameters,
                                                  final int bufferSize,
                                                  final long timeoutSeconds)
  {
    checkNotNull(db);
    checkNotNull(selectQuery);
    checkArgument(bufferSize > 0);
    checkArgument(timeoutSeconds >= 0);
    final BlockingQueue<ODocument> queue = new ArrayBlockingQueue<>(bufferSize);

    db.command(
        new OSQLNonBlockingQuery<ODocument>(
            selectQuery,
            new QueueFeedingResultListener(timeoutSeconds, queue)
        )
    ).execute(parameters);

    return new QueueConsumingIterable(timeoutSeconds, queue);
  }

  /**
   * A "sentinel" {@link ODocument} instance that marks non blocking result end.
   */
  private static final ODocument SENTINEL = new ODocument();

  /**
   * The queue feeding {@link OCommandResultListener} implementation.
   */
  private static final class QueueFeedingResultListener
      extends ComponentSupport
      implements OCommandResultListener
  {
    private final long timeoutSeconds;

    private final BlockingQueue<ODocument> queue;

    public QueueFeedingResultListener(final long timeoutSeconds,
                                      final BlockingQueue<ODocument> queue)
    {
      this.timeoutSeconds = timeoutSeconds;
      this.queue = queue;
    }

    @Override
    public boolean result(final Object o) {
      final ODocument doc = (ODocument) o;
      try {
        queue.offer(doc, timeoutSeconds, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        log.warn("Interrupted result", e);
        return false;
      }
      return true;
    }

    @Override
    public void end() {
      try {
        queue.offer(SENTINEL, timeoutSeconds, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        log.warn("Interrupted end", e);
      }
    }

    @Override
    public Object getResult() {
      return null; // unused
    }
  }

  /**
   * The {@link Iterable} implementation returned to caller that is consuming the queue fed by SQL query result.
   */
  private static final class QueueConsumingIterable
      extends ComponentSupport
      implements Iterable<ODocument>, Iterator<ODocument>
  {
    private final long timeoutSeconds;

    private final BlockingQueue<ODocument> queue;

    private ODocument next;

    public QueueConsumingIterable(final long timeoutSeconds, final BlockingQueue<ODocument> queue)
    {
      this.timeoutSeconds = timeoutSeconds;
      this.queue = queue;
    }

    @Override
    public Iterator<ODocument> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      if (next == null) {
        try {
          next = queue.poll(timeoutSeconds, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
          log.warn("Interrupted poll", e);
          throw Throwables.propagate(e);
        }
      }
      return next != SENTINEL;
    }

    @Override
    public ODocument next() {
      if (hasNext()) {
        ODocument doc = next;
        next = null;
        return doc;
      }
      throw new NoSuchElementException("Iterator depleted");
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Method not supported");
    }
  }
}
