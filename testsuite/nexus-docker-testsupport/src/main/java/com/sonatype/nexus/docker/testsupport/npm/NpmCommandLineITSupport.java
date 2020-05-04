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
package com.sonatype.nexus.docker.testsupport.npm;

import java.util.List;
import java.util.Optional;

import com.sonatype.nexus.docker.testsupport.ContainerCommandLineITSupport;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import static com.sonatype.nexus.docker.testsupport.npm.NpmFactory.createNodeConfig;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * Npm implementation of a Docker Command Line enabled container.
 *
 * @since 3.6.1
 */
public class NpmCommandLineITSupport
    extends ContainerCommandLineITSupport
{
  private static final String CMD_NPM = "npm ";

  private static final String LOGIN_SH = "/login.sh";

  /**
   * Constructor that uses the default configuration of {@link NpmCommandLineConfig}
   *
   * @see #NpmCommandLineITSupport(NpmCommandLineConfig)
   */
  public NpmCommandLineITSupport() {
    this(NpmCommandLineConfig.builder().build());
  }

  /**
   * Constructor that takes the {@link NpmCommandLineConfig} and uses the {@link DockerContainerConfig} created
   * by the factory method {@link NpmFactory#createNodeConfig(NpmCommandLineConfig)}
   *
   * @param config {@link NpmCommandLineConfig}
   */
  public NpmCommandLineITSupport(NpmCommandLineConfig config) {
    super(createNodeConfig(config));
  }

  /**
   * Constructor that uses the default configuration of {@link NpmCommandLineConfig}
   * and runs in a specified image
   * @param image specify the image to use, e.g. node:8.6. Unspecified assumes the latest
   */
  public NpmCommandLineITSupport(final String image) {
    super(image);
  }

  /**
   * Constructor.
   *
   * @param dockerContainerConfig {@link DockerContainerConfig}
   */
  public NpmCommandLineITSupport(DockerContainerConfig dockerContainerConfig) {
    super(dockerContainerConfig);
  }

  public Optional<List<String>> execNpm(final String s) {
    return exec(CMD_NPM + s);
  }

  public Optional<List<String>> execNpmConfig(final String key, final String value) {
    String command = format("%sconfig set %s %s", CMD_NPM, key, value);
    return exec(command);
  }

  public List<String> listInstalled() {
    return execNpm("ls").orElse(emptyList());
  }

  public List<String> install(final String packageName) {
    return execNpm("install " + packageName).orElse(emptyList());
  }

  public List<String> audit() {
    return execNpm("audit").orElse(emptyList());
  }

  public List<String> audit(String dir) {
    return execNpm("audit --prefix " + dir).orElse(emptyList());
  }

  public List<String> auditFix() {
    return execNpm("audit fix").orElse(emptyList());
  }

  public List<String> auditFix(String dir) {
    return execNpm("audit fix --prefix " + dir).orElse(emptyList());
  }

  public List<String> publish(final String location) {
    return execNpm("publish " + location).orElse(emptyList());
  }

  public List<String> deprecate(final String packageName, final String message) {
    String command = format("deprecate %s \"%s\"", packageName, message);
    return execNpm(command).orElse(emptyList());
  }

  public Optional<List<String>> login(final String directory) {
    exec("chmod 755 " + directory + LOGIN_SH);
    return exec(directory + LOGIN_SH);
  }

  public void updateLoginScript(final String directory,
                                final String repositoryUrl,
                                final String username,
                                final String password)
  {
    String loginTemplate = directory + "/login-template.sh";
    String login = directory + LOGIN_SH;

    String sedCommand = format("sed 's/${repositoryUrl}/%s/g' %s" +
        "| sed 's/${username}/%s/g'" +
        "| sed 's/${password}/%s/g'" +
        " > %s", repositoryUrl, loginTemplate, username, password, login);

    exec(sedCommand);
  }

  public void updatePackageJson(final String directory,
                                final String name,
                                final String version,
                                final String maintainers,
                                final String publishConfig)
  {
    String packageJsonTemplate = directory + "/package-template.json";
    String packageJson = directory + "/package.json";

    String sedCommand = format("sed 's/${maintainers}/%s/g' %s" +
        "| sed 's/${publishConfig}/%s/g'" +
        "| sed 's/${name}/%s/g'" +
        "| sed 's/${version}/%s/g'" +
        " > %s", maintainers, packageJsonTemplate, publishConfig, name, version, packageJson);

    exec(sedCommand);
  }

  public void updatePackageJson(
      final String directory,
      final String npmComponentName,
      final String npmComponentVersion)
  {
    String packageJsonTemplate = directory + "/package-template.json";
    String packageJson = directory + "/package.json";

    updatePackageJson(packageJsonTemplate, packageJson, npmComponentName, npmComponentVersion);
  }

  public void updatePackageLockJson(
      final String directory,
      final String npmComponentName,
      final String npmComponentVersion)
  {
    String packageJsonTemplate = directory + "/package-lock-template.json";
    String packageJson = directory + "/package-lock.json";

    updatePackageJson(packageJsonTemplate, packageJson, npmComponentName, npmComponentVersion);
  }

  private void updatePackageJson(
      final String inputFileName,
      final String outputFileName,
      final String npmComponentName,
      final String npmComponentVersion)
  {
    String sedCommand = format("sed 's/${componentName}/%s/g' %s | sed 's/${componentVersion}/%s/g' > %s",
        npmComponentName, inputFileName, npmComponentVersion, outputFileName);

    exec(sedCommand);
  }
}
