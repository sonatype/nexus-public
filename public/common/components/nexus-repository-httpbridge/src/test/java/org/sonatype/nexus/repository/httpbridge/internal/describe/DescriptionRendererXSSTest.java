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
package org.sonatype.nexus.repository.httpbridge.internal.describe;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersionSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.internal.template.TemplateHelperImpl;
import org.sonatype.nexus.internal.template.VelocityEngineProvider;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests for XSS protection in {@link DescriptionRendererImpl} and describeHtml.vm template.
 */
public class DescriptionRendererXSSTest
    extends TestSupport
{
  private DescriptionRenderer descriptionRenderer;

  @Before
  public void setUp() {
    BaseUrlHolder.set("http://localhost:8081", "/");

    final TemplateHelper templateHelper = new TemplateHelperImpl(
        mock(ApplicationVersionSupport.class),
        new VelocityEngineProvider(20).getObject());

    descriptionRenderer = new DescriptionRendererImpl(templateHelper);
  }

  @After
  public void tearDown() {
    BaseUrlHolder.unset();
  }

  @Test
  public void testTableEntryKeys_escapesTagsToPreventXSS() {
    final Description description = new Description(ImmutableMap.of(
        "path", "/test",
        "nexusUrl", "http://localhost:8081"));

    final Map<String, Object> maliciousParams = new HashMap<>();
    // script tag injection
    maliciousParams.put("<script>alert('xss')</script>", "value");
    // image tag injection
    maliciousParams.put("<img src onerror=alert('rxss-poc')>", "value");
    maliciousParams.put("normalKey", "normalValue");
    description.addTable("Parameters", maliciousParams);

    final String result = descriptionRenderer.renderHtml(description);

    // Then: Script tags should be escaped
    assertNotNull("HTML should not be null", result);
    // normal key/values should render as is
    assertThat(result, containsString("<td>normalKey</td>"));
    assertThat(result, containsString("<td>normalValue</td>"));

    // script tags should get escaped
    assertThat("HTML should NOT contain unescaped script tag",
        result, not(containsString("<script>alert")));
    assertThat("HTML should NOT contain unescaped img tag",
        result, not(containsString("<img src onerror")));

    // image tags should get escaped
    assertThat("HTML should contain escaped script tag",
        result, containsString("&lt;script&gt;alert('xss')&lt;/script&gt;"));
    assertThat("HTML should contain escaped img tag",
        result, containsString("&lt;img src onerror=alert('rxss-poc')&gt;"));
  }

  @Test
  public void testTopicNames_escapesTagsToPreventXSS() {
    // Given: A description with XSS attempt in topic name
    final Description description = new Description(ImmutableMap.of(
        "path", "/test",
        "nexusUrl", "http://localhost:8081"));

    description.topic("<script>alert('topic-xss')</script>");

    // When: Rendering HTML
    final String result = descriptionRenderer.renderHtml(description);

    // Then: Script tags in topic name should be escaped
    assertNotNull("HTML should not be null", result);
    assertThat("HTML should contain escaped script tags in h2",
        result, containsString("&lt;script&gt;"));
    assertThat("HTML should NOT contain unescaped script in h2",
        result, not(containsString("<h2><script>alert")));
  }

  @Test
  public void testTableNames_withScriptTag_areEscaped() {
    // Given: A description with XSS attempt in table name
    final Description description = new Description(ImmutableMap.of(
        "path", "/test",
        "nexusUrl", "http://localhost:8081"));

    description.addTable("<script>alert('table-xss')</script>", ImmutableMap.of("key", "value"));

    final String result = descriptionRenderer.renderHtml(description);

    // Then: Script tags in table name should be escaped
    assertNotNull("HTML should not be null", result);
    assertThat("HTML should contain escaped script tags in h3",
        result, containsString("&lt;script&gt;"));
    assertThat("HTML should NOT contain unescaped script in h3",
        result, not(containsString("<h3><script>alert")));
  }

  @Test
  public void testTableEntryValues_areAlsoEscaped() {
    // Given: A description with XSS attempt in both keys and values
    final Description description = new Description(ImmutableMap.of(
        "path", "/test",
        "nexusUrl", "http://localhost:8081"));

    final Map<String, Object> params = new HashMap<>();
    params.put("normalKey", "<script>alert('xss-in-value')</script>");

    description.addTable("Parameters", params);

    // When: Rendering HTML
    final String result = descriptionRenderer.renderHtml(description);

    // Then: Values should also be escaped (this was already working, but verify)
    assertNotNull("HTML should not be null", result);
    assertThat("HTML should escape script tags in values",
        result, containsString("&lt;script&gt;"));
    assertThat("HTML should NOT contain unescaped script in value",
        result, not(containsString("<script>alert")));
  }

  @Test
  public void testSpecialCharacters_areEscaped() {
    // Given: A description with special HTML characters
    final Description description = new Description(ImmutableMap.of(
        "path", "/test",
        "nexusUrl", "http://localhost:8081"));

    Map<String, Object> params = new HashMap<>();
    params.put("key&with<special>chars\"'", "value");

    description.addTable("Parameters", params);

    String result = descriptionRenderer.renderHtml(description);

    // Then: Special characters should be escaped
    assertNotNull("HTML should not be null", result);
    assertThat("HTML should escape ampersand", result, containsString("&amp;"));
    assertThat("HTML should escape less-than", result, containsString("&lt;"));
    assertThat("HTML should escape greater-than", result, containsString("&gt;"));
    assertThat("HTML should escape quotes", result, containsString("&quot;"));
  }
}
