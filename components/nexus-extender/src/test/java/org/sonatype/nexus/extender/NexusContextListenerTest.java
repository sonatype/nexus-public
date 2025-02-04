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
package org.sonatype.nexus.extender;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.BundleContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class NexusContextListenerTest
{
  final boolean isFeatureFlagEnabled;

  private final String installMode;

  private final String flag;

  private final Boolean flagValue;

  private final String edition;

  private final NexusContextListener underTest;

  public NexusContextListenerTest(
      final String installMode,
      final String flag,
      final Boolean flagValue,
      final String edition,
      final boolean isFeatureFlagEnabled)
  {
    this.installMode = installMode;
    this.flag = flag;
    this.flagValue = flagValue;
    this.edition = edition;
    this.isFeatureFlagEnabled = isFeatureFlagEnabled;

    NexusBundleExtender bundleExtender = mock(NexusBundleExtender.class);
    when(bundleExtender.getBundleContext()).thenReturn(mock(BundleContext.class));
    underTest = new NexusContextListener(bundleExtender);
  }

  @Parameters(name = "{index}: installMode: {0}, flag: {1}, flagValue: {2}, edition: {3}, isFeatureFlagEnabled: {4}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"oss,pro,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "COMMUNITY", true},
        {"oss,pro,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", true},
        {"oss,pro,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "PRO", true},
        {"oss,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "PRO", false},
        {"oss,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", true},
        {"pro,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", false},
        {"pro,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "PRO", true},
        {"pro,community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "COMMUNITY", true},
        {"community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", false},
        {"community:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "COMMUNITY", true},
        {"featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "COMMUNITY", true},

        {"oss:featureFlag:foo.enabled", "foo.enabled", true, "OSS", true},
        {"oss:featureFlag:foo.enabled", "foo.enabled", true, "PRO", false},
        {"oss:featureFlag:foo.enabled", "foo.enabled", false, "OSS", false},
        {"oss:featureFlag:foo.enabled", "foo.enabled", false, "PRO", false},
        {"oss:featureFlag:foo.enabled", "foo.enabled", null, "OSS", false},
        {"oss:featureFlag:foo.enabled", "foo.enabled", null, "PRO", false},
        {"oss:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", true},
        {"oss:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "PRO", false},
        {"oss:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", false, "OSS", false},
        {"oss:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", false, "PRO", false},
        {"oss:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", null, "OSS", true},
        {"oss:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", null, "PRO", false},
        // pro-only feature flag
        {"pro:featureFlag:foo.enabled", "foo.enabled", true, "OSS", false},
        {"pro:featureFlag:foo.enabled", "foo.enabled", true, "PRO", true},
        {"pro:featureFlag:foo.enabled", "foo.enabled", false, "OSS", false},
        {"pro:featureFlag:foo.enabled", "foo.enabled", false, "PRO", false},
        {"pro:featureFlag:foo.enabled", "foo.enabled", null, "OSS", false},
        {"pro:featureFlag:foo.enabled", "foo.enabled", null, "PRO", false},
        {"pro:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", false},
        {"pro:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "PRO", true},
        {"pro:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", false, "OSS", false},
        {"pro:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", false, "PRO", false},
        {"pro:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", null, "OSS", false},
        {"pro:featureFlag:enabledByDefault:foo.enabled", "foo.enabled", null, "PRO", true},
        // common feature flag
        {"featureFlag:foo.enabled", "foo.enabled", true, "OSS", true},
        {"featureFlag:foo.enabled", "foo.enabled", true, "PRO", true},
        {"featureFlag:foo.enabled", "foo.enabled", false, "OSS", false},
        {"featureFlag:foo.enabled", "foo.enabled", false, "PRO", false},
        {"featureFlag:foo.enabled", "foo.enabled", null, "OSS", false},
        {"featureFlag:foo.enabled", "foo.enabled", null, "PRO", false},
        {"featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", true},
        {"featureFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "PRO", true},
        {"featureFlag:enabledByDefault:foo.enabled", "foo.enabled", false, "OSS", false},
        {"featureFlag:enabledByDefault:foo.enabled", "foo.enabled", false, "PRO", false},
        {"featureFlag:enabledByDefault:foo.enabled", "foo.enabled", null, "OSS", true},
        {"featureFlag:enabledByDefault:foo.enabled", "foo.enabled", null, "PRO", true},
        // malformed feature flag strings
        {"featureFlag:", "foo.enabled", null, "OSS", false},
        {"featureFlag:", "foo.enabled", null, "PRO", false},
        {"featureFlag:enabledByDefault:", "foo.enabled", null, "OSS", false},
        {"foo:featureFlag:enabledByDefault:", "foo.enabled", null, "OSS", false},
        {"fooFlag:enabledByDefault:foo.enabled", "foo.enabled", null, "OSS", false},
        {"featureFlag:", "foo.enabled", true, "OSS", false},
        {"featureFlag:", "foo.enabled", true, "PRO", false},
        {"featureFlag:enabledByDefault:", "foo.enabled", true, "OSS", false},
        {"foo:featureFlag:enabledByDefault:", "foo.enabled", true, "OSS", false},
        {"fooFlag:enabledByDefault:foo.enabled", "foo.enabled", true, "OSS", false},
        {"featureFlag:", "foo.enabled", false, "OSS", false},
        {"featureFlag:", "foo.enabled", false, "PRO", false},
        {"featureFlag:enabledByDefault:", "foo.enabled", false, "OSS", false},
        {"foo:featureFlag:enabledByDefault:", "foo.enabled", false, "OSS", false},
        {"fooFlag:enabledByDefault:foo.enabled", "foo.enabled", false, "OSS", false},
        {"", "foo.enabled", null, "OSS", false}
    });
  }

  @Before
  public void setup() {
    System.clearProperty(flag);
    if (flagValue != null) {
      System.setProperty(flag, String.valueOf(flagValue));
    }
  }

  @Test
  public void isFeatureFlagEnabledTest() {
    boolean enabled = underTest.isFeatureFlagEnabled(edition, installMode);
    assertThat(enabled, is(isFeatureFlagEnabled));
  }
}
