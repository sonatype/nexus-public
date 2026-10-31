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
package org.sonatype.nexus.capability.firewall;

import java.util.stream.Stream;

public enum FirewallSupportedFormat
{
  R("r"),
  GO("go"),
  P2("p2"),
  RUBYGEMS("rubygems"),
  NPM("npm"),
  YUM("yum"),
  PYPI("pypi"),
  CARGO("cargo"),
  CONAN("conan"),
  CONDA("conda"),
  NUGET("nuget"),
  MAVEN2("maven2"),
  COMPOSER("composer"),
  COCOAPODS("cocoapods"),
  HUGGINGFACE("huggingface"),
  SWIFT("swift"),
  DOCKER("docker"),
  RAW("raw"),
  PUB("pub");

  private final String value;

  FirewallSupportedFormat(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static String[] getValues() {
    return Stream.of(FirewallSupportedFormat.values())
        .map(FirewallSupportedFormat::getValue)
        .toArray(String[]::new);
  }
}
