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
package com.sonatype.nexus.docker.testsupport.pypi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.nexus.docker.testsupport.ContainerCommandLineITSupport;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * PyPi implementation of a Docker Command Line enabled container.
 *
 * @since 3.13
 */
public class PyPiCommandLineITSupport
    extends ContainerCommandLineITSupport
{
  private static final String CMD_PIP = "pip ";

  private static final String PIP_PACKAGE_NAME = "pip";

  /**
   * Constructor.
   *
   * @param dockerContainerConfig {@link DockerContainerConfig}
   */
  public PyPiCommandLineITSupport(DockerContainerConfig dockerContainerConfig) {
    super(dockerContainerConfig);
  }

  /**
   * Execute a pip command. I.e. a command that is always prefixed with {@link #CMD_PIP}
   *
   * @see #exec(String)
   */
  public Optional<List<String>> pipExec(final String s) {
    return exec(CMD_PIP + s);
  }

  /**
   * Runs a <code>pip install twine</code> after installing the required dependencies that are needed (see
   * https://github.com/pyca/cryptography/issues/4789#issuecomment-555509655), i.e. we require to install
   * dependency: libressl-dev.
   *
   * @return List of {@link String} of output from execution of twine install
   */
  public List<String> pipInstallTwine() {
    exec("apk add build-base python3-dev libffi-dev libressl-dev")
        .orElseThrow(() -> new AssertionError(("Unable to install required dependencies")));

    return pipInstall("twine");
  }

  /**
   * Runs a <code>pip install</code>
   *
   * @param packageName name of the pip package to install
   * @return List of {@link String} of output from execution
   */
  public List<String> pipInstall(final String packageName) {
    return pipExec(format("install %s", packageName)).orElse(emptyList());
  }

  /**
   * Runs a <code>pip install pip==version</code>
   *
   * @param packageVersion version of the pip to install
   * @return boolean true if installed on client, false otherwise.
   */
  public boolean pipInstallPip(final String packageVersion) {
    pipInstall(format("%s==%s", PIP_PACKAGE_NAME, packageVersion));

    Map<String, String> showValues = pipShow(PIP_PACKAGE_NAME);
    String name = showValues.get("name");
    String version = showValues.get("version");

    return name.equals(PIP_PACKAGE_NAME) && version.startsWith(packageVersion.split("\\.\\*")[0]);
  }

  /**
   * Runs a <code>pip search</code>
   *
   * @param packageName name of the pip package to search
   * @return List of {@link String} output from execution
   */
  public List<String> pipSearch(final String packageName) {
    return pipExec(format("search %s", packageName)).orElse(emptyList());
  }

  /**
   * Runs a <code>pip show</code>
   *
   * @param packageName of the pip package installed
   * @return Map of {@link String}s of output from execution
   */
  public Map<String, String> pipShow(final String packageName) {
    List<String> output = pipExec(format("show %s", packageName))
        .orElseThrow(() -> new AssertionError(format("No package with name %s to show", packageName)));

    Map<String, String> keyValues = newHashMap();

    output.forEach(s -> {
      String[] split = s.split(":");
      keyValues.put(split[0].trim().toLowerCase(), split.length > 1 ? split[1].trim() : null);
    });

    return keyValues;
  }

  /**
   * Runs a <code>twine upload -r [profile] [path-to-file]</code>
   *
   * @param profile to use from within a .piprc
   * @param cliPath path to file on command line
   */
  public void twineUpload(final String profile, final String cliPath) {
    exec(format("twine upload -r %s %s", profile, cliPath))
        .orElseThrow(() -> new AssertionError(format("No way that we uploaded %s", cliPath)));
  }

  public void twineUploadWheelAndGpgSignatureFiles(
      final String profile,
      final String wheelFilePath,
      final String gpgSignatureFilePath)
  {
    exec(format("twine upload -r %s %s %s", profile, wheelFilePath, gpgSignatureFilePath))
        .orElseThrow(() -> new AssertionError(
            format("Failed to upload %s and %s", wheelFilePath, gpgSignatureFilePath)));
  }

  /**
   * Runs a <code>easy_install</code>
   *
   * @param packageName name of the easy_install package to install
   * @return List of {@link String} of output from execution
   */
  public List<String> easyInstall(final String packageName) {
    return exec(format("easy_install %s", packageName)).orElse(emptyList());
  }
}
