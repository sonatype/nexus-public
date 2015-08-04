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

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Testing RegexpMimeRulesSource, a handy class for it's wanted behavior. While everyone is free to reimplement the
 * MimeRulesSource as they want, this class is just a general utility since probably it fits almost always.
 */
public class RegexpMimeRulesSourceTest
    extends TestSupport
{
  @Test
  public void testRegectMimeRulesSourceTest() {
    final RegexpMimeRulesSource mimeRulesSource = new RegexpMimeRulesSource();

    mimeRulesSource.addRule(".*\\.foo\\z", "foo/bar");
    mimeRulesSource.addRule(".*\\.pom\\z", "application/x-pom");
    mimeRulesSource.addRule("(.*/maven-metadata.xml\\z)|(maven-metadata.xml\\z)", "application/x-maven-metadata");

    // "more specific" one
    mimeRulesSource.addRule("\\A/atom-service/.*\\.xml\\z", "application/atom+xml");
    // and now the "general one"
    mimeRulesSource.addRule(".*\\.xml\\z", "application/xml");

    assertThat(mimeRulesSource.getRuleForPath("/some/repo/path/content.foo"), equalTo("foo/bar"));
    assertThat(mimeRulesSource.getRuleForPath("/some/repo/path/content.foo.bar"), nullValue());
    assertThat(mimeRulesSource.getRuleForPath("/log4j/log4j/1.2.12/log4j-1.2.12.pom"),
        equalTo("application/x-pom"));
    assertThat(mimeRulesSource.getRuleForPath("maven-metadata.xml"), equalTo("application/x-maven-metadata"));
    assertThat(mimeRulesSource.getRuleForPath("/maven-metadata.xml"), equalTo("application/x-maven-metadata"));
    assertThat(mimeRulesSource.getRuleForPath("/org/sonatype/nexus/maven-metadata.xml"),
        equalTo("application/x-maven-metadata"));
    assertThat(mimeRulesSource.getRuleForPath("/org/sonatype/nexus/maven-metadata.xml.bar"), nullValue());
    assertThat(mimeRulesSource.getRuleForPath("/org/sonatype/nexus/maven-metadata.xml/tricky/path.pom"),
        equalTo("application/x-pom"));

    assertThat(mimeRulesSource.getRuleForPath("/org/sonatype/nexus/maven-metadata1.xml"),
        equalTo("application/xml"));
    assertThat(mimeRulesSource.getRuleForPath("/atom-service//org/sonatype/nexus/maven-metadata1.xml"),
        equalTo("application/atom+xml"));

  }

}
