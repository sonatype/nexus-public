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
package org.sonatype.nexus.bundle.launcher.internal;

import java.io.IOException;
import java.lang.reflect.Method;

import org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread;
import org.sonatype.nexus.bootstrap.monitor.KeepAliveThread;
import org.sonatype.nexus.bootstrap.monitor.commands.ExitCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.HaltCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.PingCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.StopApplicationCommand;

import org.tanukisoftware.wrapper.WrapperManager;

import static org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread.LOCALHOST;
import static org.sonatype.nexus.bootstrap.monitor.KeepAliveThread.KEEP_ALIVE_PING_INTERVAL;
import static org.sonatype.nexus.bootstrap.monitor.KeepAliveThread.KEEP_ALIVE_PORT;
import static org.sonatype.nexus.bootstrap.monitor.KeepAliveThread.KEEP_ALIVE_TIMEOUT;

/**
 * The main Nexus class (launcher) used to replace Nexus launchers for versions < 2.2.
 * <p/>
 * The launcher will start (if configured) the threads bellow and then redirect to original JSW configured launcher:
 * <br/>
 * * the command monitor (on port {#link NexusITLauncher#COMMAND_MONITOR_PORT}<br/>
 * * the keep alive thread (on port {#link NexusITLauncher#KEEP_ALIVE_PORT}<br></br>
 *
 * @since 2.2
 */
public class NexusITLauncher
{

  /**
   * Name of property to be looked up in system properties containing the FQCN of original launcher
   */
  public static final String LAUNCHER = NexusITLauncher.class.getName() + ".launcher";

  /**
   * Name of environment variable/system property to be looked up for the port number of command monitor thread.
   * If not present, command monitor will not be started.
   */
  public static final String COMMAND_MONITOR_PORT = NexusITLauncher.class.getName() + ".monitor.port";

  /**
   * 5 seconds in milliseconds.
   */
  public static final String FIVE_SECONDS = "5000";

  /**
   * 1 second in milliseconds.
   */
  public static final String ONE_SECOND = "1000";

  /**
   * Starts the command monitor/keep alive if configured and launches original JSW launcher.
   *
   * @param args startup arguments
   * @return null (continue running)
   * @throws Exception re-thrown
   */
  public Integer start(final String[] args)
      throws Exception
  {
    // find wrapped launcher
    String launcher = System.getProperty(LAUNCHER);
    if (launcher == null) {
      throw new IllegalStateException("Launcher must be specified via system property: " + LAUNCHER);
    }

    Class<?> launcherClass = getClass().getClassLoader().loadClass(launcher);
    Method main = launcherClass.getMethod("main", args.getClass());

    maybeEnableCommandMonitor();
    maybeEnableShutdownIfNotAlive();

    main.invoke(null, new Object[]{args});

    return null; // continue running
  }

  /**
   * Starts the command monitor if {#link NexusITLauncher#COMMAND_MONITOR_PORT} is configured.
   *
   * @throws IOException If command monitor fails to start
   */
  protected void maybeEnableCommandMonitor()
      throws IOException
  {
    String commandMonitorPort = System.getProperty(COMMAND_MONITOR_PORT);
    if (commandMonitorPort == null) {
      commandMonitorPort = System.getenv(COMMAND_MONITOR_PORT);
    }
    if (commandMonitorPort != null) {
      new CommandMonitorThread(
          Integer.parseInt(commandMonitorPort),
          new StopApplicationCommand(new Runnable()
          {
            @Override
            public void run() {
              WrapperManager.stopAndReturn(0);
            }
          }),
          new PingCommand(),
          new ExitCommand(),
          new HaltCommand()
      ).start();
    }
  }

  /**
   * Starts the command monitor if {#link NexusITLauncher#KEEP_ALIVE_PORT} is configured.
   *
   * @throws IOException If command monitor fails to start
   */
  protected void maybeEnableShutdownIfNotAlive()
      throws IOException
  {
    String port = System.getProperty(KEEP_ALIVE_PORT);
    if (port == null) {
      port = System.getenv(KEEP_ALIVE_PORT);
    }
    if (port != null) {
      String pingInterval = System.getProperty(KEEP_ALIVE_PING_INTERVAL);
      if (pingInterval == null) {
        pingInterval = System.getenv(KEEP_ALIVE_PING_INTERVAL);
        if (pingInterval == null) {
          pingInterval = FIVE_SECONDS;
        }
      }
      String timeout = System.getProperty(KEEP_ALIVE_TIMEOUT);
      if (timeout == null) {
        timeout = System.getenv(KEEP_ALIVE_TIMEOUT);
        if (timeout == null) {
          timeout = ONE_SECOND;
        }
      }
      new KeepAliveThread(
          LOCALHOST,
          Integer.parseInt(port),
          Integer.parseInt(pingInterval),
          Integer.parseInt(timeout)
      ).start();
    }
  }

  public static void main(final String[] args)
      throws Exception
  {
    new NexusITLauncher().start(args);
  }

}
