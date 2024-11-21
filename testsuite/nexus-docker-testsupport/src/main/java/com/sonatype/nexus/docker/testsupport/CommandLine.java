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
package com.sonatype.nexus.docker.testsupport;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Command Line IT Support classes to conform to.
 */
public interface CommandLine
{
  /**
   * Execute commands on command line.
   *
   * @param commands to execute.
   * @return {@link Optional} of results from a "docker exec" command.
   */
  Optional<List<String>> exec(String commands);

  /**
   * Download a file from the container.
   *
   * @param fromContainerPath the file to download in the container.
   * @param toLocal {@link File} host path to download to
   */
  void download(String fromContainerPath, File toLocal);

  /**
   * Initialization method that should be called right after creation of a Command Line Client but before actual
   * commands will be allowed to execute, this will allow implementers to do any pre-conditional work.
   */
  void init();

  /**
   * Called to exit the command line. Similar as exiting a terminal
   */
  void exit();

  /**
   * Retrieves the TCP Port of the host.
   *
   * @param containerPort the container that is expected to be associated with the host port.
   * @return representing port.
   */
  Integer getHostTcpPort(String containerPort);
}
