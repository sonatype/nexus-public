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
package org.sonatype.nexus.upgrade.datastore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeContextTest
{
  private final UpgradeContext underTest = new UpgradeContext();

  @Test
  void unsetFlagIsFalse() {
    assertThat(underTest.isFlagSet("some.flag")).isFalse();
  }

  @Test
  void flag_roundTrips() {
    underTest.setFlag("feature.a");
    assertThat(underTest.isFlagSet("feature.a")).isTrue();
    // independent of other keys
    assertThat(underTest.isFlagSet("feature.b")).isFalse();
  }

  @Test
  void missingValueIsEmpty() {
    assertThat(underTest.get("nope")).isEmpty();
  }

  @Test
  void value_roundTrips() {
    underTest.put("count", 42);
    assertThat(underTest.get("count")).contains(42);
    assertThat(underTest.get("count", Integer.class)).contains(42);
  }

  @Test
  void getTyped_returnsEmptyWhenStoredValueIsNotAssignable() {
    underTest.put("count", 42);
    // a value present but not assignable to the requested type is treated as absent (no ClassCastException)
    assertThat(underTest.get("count", String.class)).isEmpty();
    // the untyped getter still sees the underlying value
    assertThat(underTest.get("count")).contains(42);
  }

  @Test
  void flagIsDistinctFromValueLookup() {
    underTest.setFlag("flag");
    // a raised flag is stored as Boolean.TRUE and is also visible via the generic getter
    assertThat(underTest.get("flag")).contains(Boolean.TRUE);
  }
}
