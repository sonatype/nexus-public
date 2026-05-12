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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.formfields.FormField;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link BrandingCapabilityDescriptor}.
 */
public class BrandingCapabilityDescriptorTest
{
  private BrandingCapabilityDescriptor underTest;

  @Before
  public void setUp() {
    underTest = new BrandingCapabilityDescriptor();
  }

  @Test
  public void testType() {
    CapabilityType type = underTest.type();
    assertThat(type, is(notNullValue()));
    assertThat(type, is(BrandingCapabilityDescriptor.TYPE));
  }

  @Test
  public void testTypeId() {
    assertThat(BrandingCapabilityDescriptor.TYPE_ID, is("rapture.branding"));
  }

  @Test
  public void testName() {
    assertThat(underTest.name(), is("UI: Branding"));
  }

  @Test
  public void testFormFields() {
    List<FormField> formFields = underTest.formFields();
    assertThat(formFields, is(notNullValue()));
    assertThat(formFields, hasSize(4));
  }

  @Test
  public void testFormFieldIds() {
    List<FormField> formFields = underTest.formFields();
    assertThat(formFields.get(0).getId(), is(BrandingCapabilityConfiguration.HEADER_ENABLED));
    assertThat(formFields.get(1).getId(), is(BrandingCapabilityConfiguration.HEADER_HTML));
    assertThat(formFields.get(2).getId(), is(BrandingCapabilityConfiguration.FOOTER_ENABLED));
    assertThat(formFields.get(3).getId(), is(BrandingCapabilityConfiguration.FOOTER_HTML));
  }

  @Test
  public void testGetTags() {
    Set<Tag> tags = underTest.getTags();
    assertThat(tags, is(notNullValue()));
    assertThat(tags.isEmpty(), is(false));
    assertThat(tags, is(Tag.tags(Tag.categoryTag("UI"))));
  }

  @Test
  public void testCreateConfig() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Header</h1>");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "false");
    properties.put(BrandingCapabilityConfiguration.FOOTER_HTML, "<p>Footer</p>");

    BrandingCapabilityConfiguration config = underTest.createConfig(properties);
    assertThat(config, is(notNullValue()));
    assertThat(config.isHeaderEnabled(), is(true));
    assertThat(config.getHeaderHtml(), is("<h1>Header</h1>"));
    assertThat(config.isFooterEnabled(), is(false));
    assertThat(config.getFooterHtml(), is("<p>Footer</p>"));
  }

  @Test
  public void testCreateConfig_emptyProperties() {
    Map<String, String> properties = new HashMap<>();

    BrandingCapabilityConfiguration config = underTest.createConfig(properties);
    assertThat(config, is(notNullValue()));
    assertThat(config.isHeaderEnabled(), is(false));
    assertThat(config.isFooterEnabled(), is(false));
  }

  @Test
  public void testTypeConstant() {
    assertThat(CapabilityType.capabilityType(BrandingCapabilityDescriptor.TYPE_ID),
        is(BrandingCapabilityDescriptor.TYPE));
  }

  @Test
  public void testFormFieldLabels() {
    List<FormField> formFields = underTest.formFields();
    assertThat(formFields.get(0).getLabel(), containsString("header"));
    assertThat(formFields.get(2).getLabel(), containsString("footer"));
  }
}
