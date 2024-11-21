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
package org.sonatype.nexus.thread.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.thread.NexusThreadFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.join;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getInteger;
import static org.sonatype.nexus.security.subject.FakeAlmightySubject.TASK_SUBJECT;
import static org.sonatype.nexus.thread.NexusExecutorService.forFixedSubject;

/**
 * Common class for allowing {@link PipedOutputStream} to be written into a {@link PipedInputStream}. By default the
 * reading and writing of the streams is done in separate threads that are managed by an {@link ExecutorService} with
 * a maximum threads of {@link #DEFAULT_POOL_SIZE}. To change the amount of threads used by default either provide
 * your own {@link ExecutorService} to the {@link #StreamCopier(Consumer, Function, ExecutorService)} constructor or
 * change the property "nexus.streamcopier.poolSize" to a desired integer number.
 *
 * @since 3.8
 */
public class StreamCopier<T>
    extends ComponentSupport
{
  /**
   * Used as a default common thread pool, mainly allowing simple usage of this class
   **/
  private static ExecutorService COMMON_SERVICE = makeExecutorService();

  private final static int DEFAULT_POOL_SIZE = 100;

  private final Consumer<OutputStream> write;

  private final Function<InputStream, T> read;

  private final ExecutorService service;

  private boolean afterReadLeaveStreamsOpen;

  /**
   * Constructor that uses the default {@link #COMMON_SERVICE} as it's {@link ExecutorService}
   */
  public StreamCopier(final Consumer<OutputStream> write, final Function<InputStream, T> read) {
    this(write, read, COMMON_SERVICE);
  }

  /**
   * @param service - {@link ExecutorService} instead of the default common one.
   * @see #StreamCopier(Consumer, Function)
   */
  public StreamCopier(
      final Consumer<OutputStream> write,
      final Function<InputStream, T> read,
      final ExecutorService service)
  {
    this.write = checkNotNull(write);
    this.read = checkNotNull(read);
    this.service = checkNotNull(service);
  }

  /**
   * Reads out the written {@link OutputStream} of the given {@link Consumer}'s into the {@link InputStream}
   * of the given {@link Function}. It waits, if necessary, for at most 60 seconds for the computation
   * to complete, and then retrieves its result, if available.
   *
   * @return T
   */
  public T read() {
    return read(60000);
  }

  /**
   * Similar to {@link #read()}, but allows to setting of a waiting time for the computation
   * to complete, and then retrieves its result, if available.
   *
   * @param timeoutMilliseconds - the maximum time to wait
   * @see #read()
   */
  public T read(final long timeoutMilliseconds) {
    try {

      return service.submit(() -> {
        T result;
        PipedInputStream input = new PipedInputStream();
        PipedOutputStream output = new PipedOutputStream(input);

        try {
          write(output);

          result = read.apply(input);
        }
        catch (Exception e) {
          closeStream(input);

          throw new RuntimeException("Unable to properly read from stream", e);
        }

        return result;
      }).get(timeoutMilliseconds, MILLISECONDS);

    }
    catch (Exception e) {
      throw new RuntimeException("Unable to properly read from stream", e);
    }
  }

  /**
   * Mark that the OutputStream should be left open after we have read from {@link StreamCopier}
   *
   * @return StreamCopier
   */
  public StreamCopier<T> afterReadLeaveStreamsOpen() {
    this.afterReadLeaveStreamsOpen = true;
    return this;
  }

  private void write(final OutputStream stream) {
    if (afterReadLeaveStreamsOpen) {
      service.execute(() -> write.accept(stream));
    }
    else {
      service.execute(() -> write.andThen(this::closeStream).accept(stream));
    }
  }

  private void closeStream(final Closeable stream) {
    try {
      stream.close();
    }
    catch (IOException e) {
      log.error("Failed to close Stream", e);
    }
  }

  private static ExecutorService makeExecutorService() {
    final String name = StreamCopier.class.getSimpleName().toLowerCase();
    final int nThreads = getInteger(join(".", "nexus", name, "poolSize"), DEFAULT_POOL_SIZE);

    ThreadFactory factory = new NexusThreadFactory(name, name);
    ThreadPoolExecutor backing = new ThreadPoolExecutor(0, nThreads, 60L, SECONDS, new SynchronousQueue<>(), factory);

    return forFixedSubject(backing, TASK_SUBJECT);
  }
}
