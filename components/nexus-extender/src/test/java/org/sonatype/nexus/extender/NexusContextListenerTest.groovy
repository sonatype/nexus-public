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
package org.sonatype.nexus.extender

import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent

import org.apache.karaf.features.Feature
import org.apache.karaf.features.FeaturesService
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import org.osgi.framework.startlevel.FrameworkStartLevel
import spock.lang.Specification
import spock.lang.Unroll

import static org.sonatype.nexus.extender.NexusContextListener.NEXUS_EXTENDER_START_LEVEL

/**
 * Tests for {@link NexusContextListener}
 */
class NexusContextListenerTest
    extends Specification
{
  NexusBundleExtender bundleExtender = Mock(NexusBundleExtender)

  BundleContext bundleContext = Mock(BundleContext)

  NexusContextListener underTest

  def setup() {
    bundleExtender.getBundleContext() >> bundleContext

    underTest = new NexusContextListener(bundleExtender)
  }

  /**
   * Test setting the feature flags of orient dependent stores based on the value of {@code nexus.orient.enabled}
   */
  @Unroll
  def 'orientEnabled: #orientEnabled, configEnabled: #configEnabled, contentEnabled: #contentEnabled, expectedConfig: #expectedConfig, expectedContent: #expectedContent'() {
    given:
      ServletContextEvent event = Mock(ServletContextEvent)
      ServletContext context = Mock(ServletContext)
      Map<String, Object> nexusProperties = [:]
      ServiceReference featureServiceRef = Mock(ServiceReference)
      FeaturesService featureService = Mock(FeaturesService)
      Bundle bundle0 = Mock(Bundle)
      Feature feature = Mock(Feature)
      FrameworkStartLevel startLevel = Mock(FrameworkStartLevel)

      event.getServletContext() >> context
      context.getAttribute("nexus.properties") >> nexusProperties
      bundleContext.getServiceReference(FeaturesService.class) >> featureServiceRef
      bundleContext.getService(featureServiceRef) >> featureService
      bundleContext.getBundle(0) >> bundle0
      featureService.getFeature("null") >> feature
      bundle0.adapt(FrameworkStartLevel.class) >> startLevel
      startLevel.getStartLevel() >> NEXUS_EXTENDER_START_LEVEL

    when:
      maybePut(nexusProperties, "nexus.orient.enabled", orientEnabled)
      maybePut(nexusProperties, "nexus.orient.store.config", configEnabled)
      maybePut(nexusProperties, "nexus.orient.store.content", contentEnabled)
      underTest.contextInitialized(event)

    then:
      underTest.nexusProperties["nexus.orient.store.config"] == expectedConfig
      System.getProperty("nexus.orient.store.config") == expectedConfig as String

      underTest.nexusProperties["nexus.orient.store.content"] == expectedContent
      System.getProperty("nexus.orient.store.content") == expectedContent as String

    where:
      orientEnabled | configEnabled | contentEnabled | expectedConfig | expectedContent
      null          | null          | null           | true           | true
      null          | null          | true           | true           | true
      null          | null          | false          | true           | false
      null          | true          | null           | true           | true
      null          | true          | true           | true           | true
      null          | true          | false          | true           | false
      null          | false         | null           | false          | true
      null          | false         | true           | false          | true
      null          | false         | false          | false          | false
      true          | null          | null           | true           | true
      true          | null          | true           | true           | true
      true          | null          | false          | true           | false
      true          | true          | null           | true           | true
      true          | true          | true           | true           | true
      true          | true          | false          | true           | false
      true          | false         | null           | false          | true
      true          | false         | true           | false          | true
      true          | false         | false          | false          | false
      false         | null          | null           | false          | false
      false         | null          | true           | false          | false
      false         | null          | false          | false          | false
      false         | true          | null           | false          | false
      false         | true          | true           | false          | false
      false         | true          | false          | false          | false
      false         | false         | null           | false          | false
      false         | false         | true           | false          | false
      false         | false         | false          | false          | false
  }

  /**
   * Tests the parsing of the Karaf {@code installMode} string to determine if the feature
   * should be enabled/installed.
   */
  @Unroll
  def 'installMode: #installMode, flag: #flag, flagValue: #flagValue, edition: #edition, isFeatureFlagEnabled: #featureEnabled'() {
    given:
      if (flagValue != null) {
        System.setProperty(flag, flagValue)
      }
      else {
        System.clearProperty(flag)
      }

    when:
      def enabled = underTest.isFeatureFlagEnabled(edition, installMode)

    then:
      enabled == featureEnabled

    where:
      installMode                                    | flag          | flagValue | edition  || featureEnabled
      // oss-only feature flag
      'oss:featureFlag:foo.enabled'                  | 'foo.enabled' | 'true'    | 'OSS'    || true
      'oss:featureFlag:foo.enabled'                  | 'foo.enabled' | 'true'    | 'PRO'    || false
      'oss:featureFlag:foo.enabled'                  | 'foo.enabled' | 'false'   | 'OSS'    || false
      'oss:featureFlag:foo.enabled'                  | 'foo.enabled' | 'false'   | 'PRO'    || false
      'oss:featureFlag:foo.enabled'                  | 'foo.enabled' | null      | 'OSS'    || false
      'oss:featureFlag:foo.enabled'                  | 'foo.enabled' | null      | 'PRO'    || false
      'oss:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'true'    | 'OSS'    || true
      'oss:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'true'    | 'PRO'    || false
      'oss:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'false'   | 'OSS'    || false
      'oss:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'false'   | 'PRO'    || false
      'oss:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | null      | 'OSS'    || true
      'oss:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | null      | 'PRO'    || false
      // pro-only feature flag
      'pro:featureFlag:foo.enabled'                  | 'foo.enabled' | 'true'    | 'OSS'    || false
      'pro:featureFlag:foo.enabled'                  | 'foo.enabled' | 'true'    | 'PRO'    || true
      'pro:featureFlag:foo.enabled'                  | 'foo.enabled' | 'false'   | 'OSS'    || false
      'pro:featureFlag:foo.enabled'                  | 'foo.enabled' | 'false'   | 'PRO'    || false
      'pro:featureFlag:foo.enabled'                  | 'foo.enabled' | null      | 'OSS'    || false
      'pro:featureFlag:foo.enabled'                  | 'foo.enabled' | null      | 'PRO'    || false
      'pro:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'true'    | 'OSS'    || false
      'pro:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'true'    | 'PRO'    || true
      'pro:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'false'   | 'OSS'    || false
      'pro:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | 'false'   | 'PRO'    || false
      'pro:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | null      | 'OSS'    || false
      'pro:featureFlag:enabledByDefault:foo.enabled' | 'foo.enabled' | null      | 'PRO'    || true
      // common feature flag
      'featureFlag:foo.enabled'                      | 'foo.enabled' | 'true'    | 'OSS'    || true
      'featureFlag:foo.enabled'                      | 'foo.enabled' | 'true'    | 'PRO'    || true
      'featureFlag:foo.enabled'                      | 'foo.enabled' | 'false'   | 'OSS'    || false
      'featureFlag:foo.enabled'                      | 'foo.enabled' | 'false'   | 'PRO'    || false
      'featureFlag:foo.enabled'                      | 'foo.enabled' | null      | 'OSS'    || false
      'featureFlag:foo.enabled'                      | 'foo.enabled' | null      | 'PRO'    || false
      'featureFlag:enabledByDefault:foo.enabled'     | 'foo.enabled' | 'true'    | 'OSS'    || true
      'featureFlag:enabledByDefault:foo.enabled'     | 'foo.enabled' | 'true'    | 'PRO'    || true
      'featureFlag:enabledByDefault:foo.enabled'     | 'foo.enabled' | 'false'   | 'OSS'    || false
      'featureFlag:enabledByDefault:foo.enabled'     | 'foo.enabled' | 'false'   | 'PRO'    || false
      'featureFlag:enabledByDefault:foo.enabled'     | 'foo.enabled' | null      | 'OSS'    || true
      'featureFlag:enabledByDefault:foo.enabled'     | 'foo.enabled' | null      | 'PRO'    || true
      // malformed feature flag strings
      'featureFlag:'                                 | 'foo.enabled' | null      | 'OSS' || false
      'featureFlag:'                                 | 'foo.enabled' | null      | 'PRO' || false
      'featureFlag:enabledByDefault:'                | 'foo.enabled' | null      | 'OSS' || false
      'foo:featureFlag:enabledByDefault:'            | 'foo.enabled' | null      | 'OSS' || false
      'fooFlag:enabledByDefault:foo.enabled'         | 'foo.enabled' | null      | 'OSS' || false
      'featureFlag:'                                 | 'foo.enabled' | 'true'    | 'OSS' || false
      'featureFlag:'                                 | 'foo.enabled' | 'true'    | 'PRO' || false
      'featureFlag:enabledByDefault:'                | 'foo.enabled' | 'true'    | 'OSS' || false
      'foo:featureFlag:enabledByDefault:'            | 'foo.enabled' | 'true'    | 'OSS' || false
      'fooFlag:enabledByDefault:foo.enabled'         | 'foo.enabled' | 'true'    | 'OSS' || false
      'featureFlag:'                                 | 'foo.enabled' | 'false'   | 'OSS' || false
      'featureFlag:'                                 | 'foo.enabled' | 'false'   | 'PRO' || false
      'featureFlag:enabledByDefault:'                | 'foo.enabled' | 'false'   | 'OSS' || false
      'foo:featureFlag:enabledByDefault:'            | 'foo.enabled' | 'false'   | 'OSS' || false
      'fooFlag:enabledByDefault:foo.enabled'         | 'foo.enabled' | 'false'   | 'OSS' || false
      ''                                             | 'foo.enabled' | null      | 'OSS' || false
  }

  private static void maybePut(map, key, value) {
    if (value != null) {
      map.put(key, value)
    }
  }
}
