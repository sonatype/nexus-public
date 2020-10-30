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
package org.sonatype.nexus.repository.maven.internal.matcher;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Predicate;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * UT for {@link MavenMatcherSupport}
 *
 * @since 3.0
 */
public class MavenMatcherSupportTest
    extends TestSupport
{
  @Test
  public void equalsWithHashes() {
    Predicate<String> pred = MavenMatcherSupport.withHashes("/some-path.txt"::equals);

    assertThat(pred.apply("/some-path.txt"), equalTo(true));
    assertThat(pred.apply("/some-path.txt.sha1"), equalTo(true));
    assertThat(pred.apply("/some-path.txt.md5"), equalTo(true));
    assertThat(pred.apply("/some-path.txt.sha256"), equalTo(true));
    assertThat(pred.apply("/some-path.txt.sha512"), equalTo(true));

    assertThat(pred.apply("some-path.txt"), equalTo(false));
    assertThat(pred.apply("/some-path.txt.crc"), equalTo(false));
    assertThat(pred.apply("/some-path.tx"), equalTo(false));
    assertThat(pred.apply("/other-path.txt"), equalTo(false));
  }

  @Test
  public void endsWithWithHashes() {
    Predicate<String> pred = MavenMatcherSupport.withHashes((String input) -> input.endsWith("/some-path.txt"));

    assertThat(pred.apply("/some/prefix/some-path.txt"), equalTo(true));
    assertThat(pred.apply("/some-path.txt.sha1"), equalTo(true));
    assertThat(pred.apply("some/prefix/some-path.txt.md5"), equalTo(true));
    assertThat(pred.apply("/some-path.txt.sha256"), equalTo(true));
    assertThat(pred.apply("/some-path.txt.sha512"), equalTo(true));

    assertThat(pred.apply("some-path.txt"), equalTo(false));
    assertThat(pred.apply("/some-path.txt.crc"), equalTo(false));
    assertThat(pred.apply("/some-path.tx"), equalTo(false));
    assertThat(pred.apply("/other-path.txt"), equalTo(false));
  }
}
