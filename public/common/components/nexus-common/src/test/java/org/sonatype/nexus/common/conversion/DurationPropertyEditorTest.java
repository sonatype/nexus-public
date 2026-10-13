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
package org.sonatype.nexus.common.conversion;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link DurationPropertyEditor}.
 */
class DurationPropertyEditorTest
{
  @Test
  void parseSuffixed() {
    assertThat(parse("5m"), is(Duration.ofMinutes(5)));
    assertThat(parse("300s"), is(Duration.ofSeconds(300)));
    assertThat(parse("2 hours"), is(Duration.ofHours(2)));
  }

  @Test
  void parseIso8601() {
    assertThat(parse("PT5M"), is(Duration.ofMinutes(5)));
    assertThat(parse("pt5m"), is(Duration.ofMinutes(5)));
    assertThat(parse("PT1H30M"), is(Duration.ofMinutes(90)));
    assertThat(parse("P2D"), is(Duration.ofDays(2)));
  }

  @Test
  void parseUnparseable() {
    assertThrows(RuntimeException.class, () -> parse("nonsense"));
  }

  private static Duration parse(final String value) {
    DurationPropertyEditor underTest = new DurationPropertyEditor();
    underTest.setValue(value);
    return (Duration) underTest.getValue();
  }
}
