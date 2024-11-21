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
package com.sonatype.nexus.docker.testsupport.conda;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sonatype.nexus.docker.testsupport.ContainerCommandLineITSupport;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * Conda implementation of a Docker Command Line enabled container.
 *
 * @since 3.19
 */
public class CondaCommandLineITSupport
    extends ContainerCommandLineITSupport
{
  private static final String CMD_CONDA = "conda ";

  /**
   * Constructor.
   *
   * @param dockerContainerConfig {@link DockerContainerConfig}
   */
  public CondaCommandLineITSupport(final DockerContainerConfig dockerContainerConfig) {
    super(dockerContainerConfig);
  }

  /**
   * Execute a conda command. I.e. a command that is always prefixed with {@link #CMD_CONDA}
   *
   * @see #exec(String)
   */
  public List<String> condaExec(final String s) {
    return exec(CMD_CONDA + s).orElse(emptyList());
  }

  /**
   * Runs a <code>conda -y install</code>
   *
   * @param packageName name of the conda package to install
   * @return List of {@link String} of output from execution
   */
  public List<String> condaInstall(final String packageName) {
    return condaExec(format("install -y %s", packageName));
  }

  /**
   * Runs a <code>conda list</code>
   *
   * @return List of {@link String} of output from execution
   */
  public List<String> listInstalled() {
    return clearTerminalOutputHeader(condaExec("list"));
  }

  /**
   * Runs a <code>conda activate</code>
   *
   * @return List of {@link String} of output from execution
   */
  public List<String> condaSearchPackages(final String name) {
    return clearTerminalOutputHeader(condaExec("search " + name));
  }

  /**
   * Remove package by name
   *
   * @param name name of the package
   * @return terminal output
   */
  public List<String> removePackage(final String name) {
    return condaExec("remove -y --name " + name);
  }

  /**
   * Clean Conda client cache
   */
  public List<String> clearClientCache() {
    return condaExec("clean -a -y"); // -a = all ; -y - do not ask accept
  }

  /**
   * Remove top header from the terminal output
   */
  private List<String> clearTerminalOutputHeader(final List<String> terminalOutput) {
    Optional<String> header = terminalOutput.stream().filter(row -> row.contains("Name")).findFirst();
    if (!header.isPresent()) {
      return emptyList();
    }
    int headerIndex = terminalOutput.indexOf(header.get());
    return new ArrayList<>(terminalOutput.subList(headerIndex + 1, terminalOutput.size()));
  }
}
