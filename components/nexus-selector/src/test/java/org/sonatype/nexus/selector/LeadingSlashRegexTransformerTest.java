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
package org.sonatype.nexus.selector;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.selector.LeadingSlashRegexTransformer.trimLeadingSlashes;

public class LeadingSlashRegexTransformerTest
    extends TestSupport
{
  @Test
  public void expectedLeadingSlashTransformations() {
    assertThat(trimLeadingSlashes(""), is(""));
    assertThat(trimLeadingSlashes("/"), is(""));
    assertThat(trimLeadingSlashes(".*"), is(".*"));
    assertThat(trimLeadingSlashes("/.*"), is(".*"));
    assertThat(trimLeadingSlashes(".*/"), is(".*(^|/)"));
    assertThat(trimLeadingSlashes("/.*/"), is(".*/"));
    assertThat(trimLeadingSlashes(".?/foo/"), is(".?(^|/)foo/"));
    assertThat(trimLeadingSlashes("/+foo/"), is("(^|/)+foo/"));
    assertThat(trimLeadingSlashes("/com|/org"), is("com|org"));
    assertThat(trimLeadingSlashes("(?!.*/struts/.*).*/apache/.*"), is("(?!.*(^|/)struts/.*).*(^|/)apache/.*"));
    assertThat(trimLeadingSlashes("((([a-z[A-Z]]/)?([0-9]?/)*)|.*/?)/foo"), is("((([a-z[A-Z]]/)?([0-9]?(^|/))*)|.*/?)foo"));
  }
}
