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
package org.sonatype.nexus.coreui.internal.branding;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

/**
 * Tests for {@link BrandingCapabilityConfiguration}.
 */
public class BrandingCapabilityConfigurationTest
    extends TestSupport
{
  @Test
  public void testConstructor_allPropertiesSet() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Header</h1>");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.FOOTER_HTML, "<p>Footer</p>");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.isHeaderEnabled(), is(true));
    assertThat(config.getHeaderHtml(), is("<h1>Header</h1>"));
    assertThat(config.isFooterEnabled(), is(true));
    assertThat(config.getFooterHtml(), is("<p>Footer</p>"));
  }

  @Test
  public void testConstructor_emptyProperties() {
    Map<String, String> properties = new HashMap<>();

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.isHeaderEnabled(), is(false));
    assertThat(config.getHeaderHtml(), is(nullValue()));
    assertThat(config.isFooterEnabled(), is(false));
    assertThat(config.getFooterHtml(), is(nullValue()));
  }

  @Test
  public void testConstructor_nullProperties() {
    assertThrows(NullPointerException.class, () -> new BrandingCapabilityConfiguration(null));
  }

  @Test
  public void testParseBoolean_trueValue() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "false");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.isHeaderEnabled(), is(true));
    assertThat(config.isFooterEnabled(), is(false));
  }

  @Test
  public void testParseBoolean_emptyStringFallsBackToDefault() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.isHeaderEnabled(), is(false));
    assertThat(config.isFooterEnabled(), is(false));
  }

  @Test
  public void testParseBoolean_nullFallsBackToDefault() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, null);

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.isHeaderEnabled(), is(false));
  }

  @Test
  public void testParseString_nonEmptyValue() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Test</h1>");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.getHeaderHtml(), is("<h1>Test</h1>"));
  }

  @Test
  public void testParseString_emptyValueReturnsNull() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.getHeaderHtml(), is(nullValue()));
  }

  @Test
  public void testParseString_nullValueReturnsNull() {
    Map<String, String> properties = new HashMap<>();

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    assertThat(config.getHeaderHtml(), is(nullValue()));
    assertThat(config.getFooterHtml(), is(nullValue()));
  }

  @Test
  public void testToString() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "false");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);

    String result = config.toString();
    assertThat(result, containsString("BrandingCapabilityConfiguration"));
    assertThat(result, containsString("headerEnabled=true"));
    assertThat(result, containsString("footerEnabled=false"));
  }

  @Test
  public void testConstants() {
    assertThat(BrandingCapabilityConfiguration.HEADER_ENABLED, is("headerEnabled"));
    assertThat(BrandingCapabilityConfiguration.HEADER_HTML, is("headerHtml"));
    assertThat(BrandingCapabilityConfiguration.FOOTER_ENABLED, is("footerEnabled"));
    assertThat(BrandingCapabilityConfiguration.FOOTER_HTML, is("footerHtml"));
  }
}
