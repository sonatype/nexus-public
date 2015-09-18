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
package org.sonatype.nexus.mime;

import org.sonatype.goodies.testsupport.TestSupport;

import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link RegexpMimeRulesSource}.
 */
public class RegexpMimeRulesSourceTest
    extends TestSupport
{
  @Test
  public void testRegectMimeRulesSourceTest() {
    final RegexpMimeRulesSource underTest = new RegexpMimeRulesSource();

    underTest.addRule(".*\\.foo\\z", "foo/bar");
    underTest.addRule(".*\\.pom\\z", "application/x-pom");
    underTest.addRule("(.*/maven-metadata.xml\\z)|(maven-metadata.xml\\z)", "application/x-maven-metadata");

    // "more specific" one
    underTest.addRule("\\A/atom-service/.*\\.xml\\z", "application/atom+xml");

    // and now the "general one"
    underTest.addRule(".*\\.xml\\z", "application/xml");

    assertValues(underTest, "/some/repo/path/content.foo", equalTo("foo/bar"));
    assertValues(underTest, "/some/repo/path/content.foo.bar", nullValue());
    assertValues(underTest, "/log4j/log4j/1.2.12/log4j-1.2.12.pom", equalTo("application/x-pom"));
    assertValues(underTest, "maven-metadata.xml", equalTo("application/x-maven-metadata"));
    assertValues(underTest, "/maven-metadata.xml", equalTo("application/x-maven-metadata"));
    assertValues(underTest, "/org/sonatype/nexus/maven-metadata.xml", equalTo("application/x-maven-metadata"));
    assertValues(underTest, "/org/sonatype/nexus/maven-metadata.xml.bar", nullValue());
    assertValues(underTest, "/org/sonatype/nexus/maven-metadata.xml/tricky/path.pom", equalTo("application/x-pom"));

    assertValues(underTest, "/org/sonatype/nexus/maven-metadata1.xml", equalTo("application/xml"));
    assertValues(underTest, "/atom-service//org/sonatype/nexus/maven-metadata1.xml", equalTo("application/atom+xml"));
  }

  private void assertValues(final MimeRulesSource mimeRulesSource, final String path, final Matcher<? super String> matcher) {
    MimeRule nexusMimeType = mimeRulesSource.getRuleForName(path);
    if (matcher.getClass().equals(nullValue().getClass())) {
      assertThat(nexusMimeType, nullValue());
      return;
    }
    assertThat(nexusMimeType, notNullValue());
    assertThat(nexusMimeType.getMimetypes(), hasSize(1));
    assertThat(nexusMimeType.getMimetypes().get(0), matcher);
  }
}
