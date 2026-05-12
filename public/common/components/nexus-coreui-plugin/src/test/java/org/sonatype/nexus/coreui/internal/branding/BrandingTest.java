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

import org.sonatype.nexus.common.app.BaseUrlHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link Branding}.
 */
public class BrandingTest
{
  private Branding underTest;

  @Before
  public void setUp() {
    underTest = new Branding();
    BaseUrlHolder.set("http://localhost:8081", "/");
  }

  @After
  public void tearDown() {
    BaseUrlHolder.unset();
  }

  @Test
  public void testGetState_noConfigReturnsNull() {
    Map<String, Object> state = underTest.getState();
    assertThat(state, is(nullValue()));
  }

  @Test
  public void testGetState_withConfig() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Header</h1>");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.FOOTER_HTML, "<p>Footer</p>");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);
    underTest.set(config);

    Map<String, Object> state = underTest.getState();
    assertThat(state, is(notNullValue()));
    assertThat(state.containsKey("branding"), is(true));

    BrandingXO branding = (BrandingXO) state.get("branding");
    assertThat(branding.isHeaderEnabled(), is(true));
    assertThat(branding.getHeaderHtml(), is("<h1>Header</h1>"));
    assertThat(branding.isFooterEnabled(), is(true));
    assertThat(branding.getFooterHtml(), is("<p>Footer</p>"));
  }

  @Test
  public void testReset() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Header</h1>");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "false");
    properties.put(BrandingCapabilityConfiguration.FOOTER_HTML, "");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);
    underTest.set(config);

    assertThat(underTest.getState(), is(notNullValue()));

    underTest.reset();

    assertThat(underTest.getState(), is(nullValue()));
  }

  @Test
  public void testInterpolate_withBaseUrl() {
    String result = underTest.interpolate("<img src='$baseUrl/images/logo.png'/>");
    assertThat(result, is(notNullValue()));
    // BaseUrlHolder relative path is "/" which gets stripped to ""
    assertThat(result, is("<img src='/images/logo.png'/>"));
  }

  @Test
  public void testInterpolate_withNullReturnsNull() {
    String result = underTest.interpolate(null);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void testInterpolate_noBaseUrlPlaceholder() {
    String html = "<h1>No replacement needed</h1>";
    String result = underTest.interpolate(html);
    assertThat(result, is(html));
  }

  @Test
  public void testGetState_interpolatesHtml() {
    BaseUrlHolder.unset();
    BaseUrlHolder.set("http://localhost:8081/nexus", "/nexus");

    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<img src='$baseUrl/logo.png'/>");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.FOOTER_HTML, "<a href='$baseUrl/help'>Help</a>");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);
    underTest.set(config);

    Map<String, Object> state = underTest.getState();
    BrandingXO branding = (BrandingXO) state.get("branding");
    assertThat(branding.getHeaderHtml(), is("<img src='/nexus/logo.png'/>"));
    assertThat(branding.getFooterHtml(), is("<a href='/nexus/help'>Help</a>"));
  }

  @Test
  public void testGetState_nullHtmlFieldsInterpolatedAsNull() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "false");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);
    underTest.set(config);

    Map<String, Object> state = underTest.getState();
    BrandingXO branding = (BrandingXO) state.get("branding");
    assertThat(branding.getHeaderHtml(), is(nullValue()));
    assertThat(branding.getFooterHtml(), is(nullValue()));
  }

  @Test
  public void testSet_overridesPreviousConfig() {
    Map<String, String> properties1 = new HashMap<>();
    properties1.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties1.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>First</h1>");

    Map<String, String> properties2 = new HashMap<>();
    properties2.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "false");
    properties2.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Second</h1>");

    underTest.set(new BrandingCapabilityConfiguration(properties1));
    underTest.set(new BrandingCapabilityConfiguration(properties2));

    Map<String, Object> state = underTest.getState();
    BrandingXO branding = (BrandingXO) state.get("branding");
    assertThat(branding.isHeaderEnabled(), is(false));
    assertThat(branding.getHeaderHtml(), is("<h1>Second</h1>"));
  }
}
