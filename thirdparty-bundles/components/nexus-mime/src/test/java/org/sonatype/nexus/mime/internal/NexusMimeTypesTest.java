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
package org.sonatype.nexus.mime.internal;

import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.mime.MimeRule;

import com.google.common.base.Joiner;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link NexusMimeTypes}.
 */
public class NexusMimeTypesTest
    extends TestSupport
{
  private NexusMimeTypes underTest = new NexusMimeTypes();

  private Properties addMimeType(final Properties properties, final String extension, final String... types) {
    properties.setProperty(extension, Joiner.on(",").join(types));
    return properties;
  }

  @Test
  public void unconfigured() {
    assertThat(underTest.getMimeRuleForExtension("test"), is(nullValue()));
  }

  @Test
  public void addMimeType() {
    underTest.initMimeTypes(addMimeType(new Properties(), "test", "application/octet-stream"));
    final MimeRule mimeRule = underTest.getMimeRuleForExtension("test");
    assertThat(mimeRule, is(notNullValue()));
    assertThat(mimeRule, hasProperty("override", is(false)));
    assertThat(mimeRule.getMimetypes(), contains("application/octet-stream"));
  }

  @Test
  public void overrideMimeType() {
    Properties properties = new Properties();
    underTest.initMimeTypes(addMimeType(properties, "override.test", "application/octet-stream"));
    final MimeRule mimeRule = underTest.getMimeRuleForExtension("test");
    assertThat(mimeRule, is(notNullValue()));
    assertThat(mimeRule, hasProperty("override", is(true)));
    assertThat(mimeRule.getMimetypes(), contains("application/octet-stream"));
  }

  @Test
  public void mergeOverrideAndAdditional() {
    Properties types = new Properties();

    addMimeType(types, "override.test", "application/octet-stream");
    addMimeType(types, "test", "text/plain");

    underTest.initMimeTypes(types);
    final MimeRule mimeRule = underTest.getMimeRuleForExtension("test");
    assertThat(mimeRule, is(notNullValue()));
    assertThat(mimeRule, hasProperty("override", is(true)));
    assertThat(mimeRule.getMimetypes(), contains("application/octet-stream", "text/plain"));
  }
}
