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
package org.sonatype.nexus.crypto;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnalyticsObfuscatorTest
{
  private static final String SHA1_TEST = "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";

  private static final String SHA1_MAVEN_HOSTED_PROD = "9b98e1a1b87c0ba0d687bbfbe33f67f3f63e5a91";

  private static final String SHA1_DOCKER_HOSTED = "50e9c7a420f4d5b9ad813c3aa96d98b9a968578b";

  private static final String SHA1_EMPTY = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

  private static final String SHA1_CAFE_NFC = "f424452a9673918c6f09b0cdd35b20be8e6ae7d7";

  private static final String SHA1_MAVEN_HOSTED_PROD_UPPER = "720b05c6d628eedf2db71db43d69bb1bf7ec536f";

  private static final String SHA1_MIXED_ALPHANUMERIC = "7e060b7dee61e716db7294e3801e2d762065370b";

  private static final String SHA1_SPECIAL_CHARS = "fe38286a9fef4327c498ff13f09d05ed6fda1096";

  private final AnalyticsObfuscator obfuscator = new AnalyticsObfuscator();

  @Test
  public void obfuscatesToLowercaseHexSha1() {
    assertThat(obfuscator.obfuscate("test")).isEqualTo(SHA1_TEST);
    assertThat(obfuscator.obfuscate("maven-hosted-prod")).isEqualTo(SHA1_MAVEN_HOSTED_PROD);
    assertThat(obfuscator.obfuscate("docker-hosted")).isEqualTo(SHA1_DOCKER_HOSTED);
  }

  @Test
  public void obfuscatesNullInputAsEmptyString() {
    assertThat(obfuscator.obfuscate(null)).isEqualTo(SHA1_EMPTY);
  }

  @Test
  public void producesStableOutputAcrossInvocations() {
    String first = obfuscator.obfuscate("stable-input");
    String second = obfuscator.obfuscate("stable-input");

    assertThat(first).isEqualTo(second);
  }

  @Test
  public void obfuscatesEmptyString() {
    assertThat(obfuscator.obfuscate("")).isEqualTo(SHA1_EMPTY);
  }

  @Test
  public void obfuscatesNonAsciiUtf8Input() {
    assertThat(obfuscator.obfuscate("café")).isEqualTo(SHA1_CAFE_NFC);
  }

  @Test
  public void obfuscatesUppercaseInput() {
    assertThat(obfuscator.obfuscate("MAVEN-HOSTED-PROD")).isEqualTo(SHA1_MAVEN_HOSTED_PROD_UPPER);
    assertThat(obfuscator.obfuscate("MAVEN-HOSTED-PROD"))
        .isNotEqualTo(obfuscator.obfuscate("maven-hosted-prod"));
  }

  @Test
  public void obfuscatesMixedAlphanumericInput() {
    assertThat(obfuscator.obfuscate("Repo42-Mixed_Case-v2")).isEqualTo(SHA1_MIXED_ALPHANUMERIC);
  }

  @Test
  public void obfuscatesSpecialCharactersInput() {
    assertThat(obfuscator.obfuscate("repo name/with spaces&special!@#$%")).isEqualTo(SHA1_SPECIAL_CHARS);
  }
}
