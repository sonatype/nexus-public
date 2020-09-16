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
package org.sonatype.repository.helm.internal.util;

import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.repository.helm.HelmAttributes;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ProvenanceParserTest
    extends TestSupport
{
  private ProvenanceParser provenanceParser;

  @Before
  public void setUp() {
    this.provenanceParser = new ProvenanceParser();
  }

  @Test
  public void parseProv() throws Exception {
    InputStream is = getClass().getResourceAsStream("mysql-1.4.0.tgz.prov");
    HelmAttributes attributes = provenanceParser.parse(is);

    assertThat(attributes.getName(), is(equalTo("mysql")));
    assertThat(attributes.getDescription(), is(equalTo("Fast, reliable, scalable, and easy to use open-source relational database system.")));
    assertThat(attributes.getVersion(), is(equalTo("1.4.0")));
    assertThat(attributes.getIcon(), is(equalTo("https://www.mysql.com/common/logos/logo-mysql-170x115.png")));
    assertThat(attributes.getAppVersion(), is(equalTo("5.7.27")));
  }
}
