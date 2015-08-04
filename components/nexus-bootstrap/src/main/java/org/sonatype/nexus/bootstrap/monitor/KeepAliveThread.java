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
package org.sonatype.nexus.bootstrap.monitor;

import java.io.IOException;
import java.net.ConnectException;

import org.sonatype.nexus.bootstrap.ShutdownHelper;
import org.sonatype.nexus.bootstrap.monitor.commands.PingCommand;

/**
 * Thread which pings a specified host:port at a configured interval and executes a task if
 * the remote monitor is unreachable (ie. {@link ConnectException}).
 *
 * @since 2.2
 */
public class KeepAliveThread
    extends Thread
{
  // NOTE: Avoiding any logging our sysout usage by this class, this could lockup logging when its detected remote unreachable

  public static final String KEEP_ALIVE_PORT = KeepAliveThread.class.getName() + ".port";

  public static final String KEEP_ALIVE_PING_INTERVAL = KeepAliveThread.class.getName() + ".pingInterval";

  public static final String KEEP_ALIVE_TIMEOUT = KeepAliveThread.class.getName() + ".timeout";

  private final CommandMonitorTalker talker;

  private final int interval;

  private final int timeout;

  private final Runnable task;

  private volatile boolean running;

  /**
   * Execute custom {@link Runtime} when remote is unreachable.
   *
   * @param host     host to be pinged
   * @param port     port on host to be pinged
   * @param interval interval between pings
   * @param timeout  ping timeout
   * @param task     task to execute when remote is unreachable, this task should not log or write to syslog if
   *                 possible to avoid locking up on shutdown
   */
  // TestAccessible for most uses the task should be to HALT
  public KeepAliveThread(final String host,
                         final int port,
                         final int interval,
                         final int timeout,
                         final Runnable task)
      throws IOException
  {
    setDaemon(true);
    setName(getClass().getName());

    this.talker = new CommandMonitorTalker(host, port);
    this.interval = interval;
    this.timeout = timeout;
    this.task = task;
    this.running = true;
  }

  /**
   * Halt the JVM when remote is unreachable.
   *
   * @param host     host to be pinged
   * @param port     port on host to be pinged
   * @param interval interval between pings
   * @param timeout  ping timeout
   */
  public KeepAliveThread(final String host,
                         final int port,
                         final int interval,
                         final int timeout)
      throws IOException
  {
    this(host, port, interval, timeout, new Runnable()
    {
      @Override
      public void run() {
        ShutdownHelper.halt(666);
      }
    });
  }

  /**
   * Continue pinging on configured port until there is a connection (refused) exception, case when a shutdown will be
   * performed.
   */
  @Override
  public void run() {
    while (running) {
      try {
        try {
          ping();
          sleep(interval);
        }
        catch (final InterruptedException e) {
          // re-ping if we were interrupted for any reason
          ping();
        }
      }
      catch (ConnectException e) {
        stopRunning();
        executeTask();
      }
    }
  }

  /**
   * Pings the configured host/port.
   *
   * @throws ConnectException If ping fails
   */
  private void ping() throws ConnectException {
    try {
      talker.send(PingCommand.NAME, timeout);
    }
    catch (ConnectException e) {
      throw e;
    }
    catch (Exception e) {
      // ignore
    }
  }

  // @TestAccessible
  void executeTask() {
    task.run();
  }

  /**
   * Stops this thread from running (without running the shutdown code).
   */
  public void stopRunning() {
    running = false;
  }
}
