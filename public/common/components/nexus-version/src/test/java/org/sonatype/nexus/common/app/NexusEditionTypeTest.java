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
package org.sonatype.nexus.common.app;

import java.util.Optional;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link NexusEditionType}.
 */
public class NexusEditionTypeTest
{
  @Test
  public void fromShortName_resolvesEachKnownEdition() {
    assertThat(NexusEditionType.fromShortName("CORE"), is(Optional.of(NexusEditionType.CORE)));
    assertThat(NexusEditionType.fromShortName("COMMUNITY"), is(Optional.of(NexusEditionType.COMMUNITY)));
    assertThat(NexusEditionType.fromShortName("PRO"), is(Optional.of(NexusEditionType.PRO)));
    assertThat(NexusEditionType.fromShortName("PROPLUS"), is(Optional.of(NexusEditionType.PRO_PLUS)));
    assertThat(NexusEditionType.fromShortName("ENTERPRISE"), is(Optional.of(NexusEditionType.ENTERPRISE)));
  }

  @Test
  public void fromShortName_isCaseInsensitive() {
    assertThat(NexusEditionType.fromShortName("pro"), is(Optional.of(NexusEditionType.PRO)));
    assertThat(NexusEditionType.fromShortName("Pro"), is(Optional.of(NexusEditionType.PRO)));
    assertThat(NexusEditionType.fromShortName("proplus"), is(Optional.of(NexusEditionType.PRO_PLUS)));
    assertThat(NexusEditionType.fromShortName("enterprise"), is(Optional.of(NexusEditionType.ENTERPRISE)));
  }

  @Test
  public void fromShortName_trimsSurroundingWhitespace() {
    assertThat(NexusEditionType.fromShortName("  PRO  "), is(Optional.of(NexusEditionType.PRO)));
  }

  @Test
  public void fromShortName_returnsEmptyForNullOrUnknown() {
    assertThat(NexusEditionType.fromShortName(null), is(Optional.empty()));
    assertThat(NexusEditionType.fromShortName(""), is(Optional.empty()));
    assertThat(NexusEditionType.fromShortName("TRIAL"), is(Optional.empty()));
    assertThat(NexusEditionType.fromShortName("OSS"), is(Optional.empty()));
  }

  @Test
  public void isProFamily_trueForProProPlusAndEnterprise() {
    assertThat(NexusEditionType.PRO.isProFamily(), is(true));
    assertThat(NexusEditionType.PRO_PLUS.isProFamily(), is(true));
    assertThat(NexusEditionType.ENTERPRISE.isProFamily(), is(true));
  }

  @Test
  public void isProFamily_falseForCoreAndCommunity() {
    assertThat(NexusEditionType.CORE.isProFamily(), is(false));
    assertThat(NexusEditionType.COMMUNITY.isProFamily(), is(false));
  }

  @Test
  public void toFrontendShortName_collapsesProFamilyToPro() {
    assertThat(NexusEditionType.toFrontendShortName("PRO"), is("PRO"));
    assertThat(NexusEditionType.toFrontendShortName("PROPLUS"), is("PRO"));
    assertThat(NexusEditionType.toFrontendShortName("ENTERPRISE"), is("PRO"));
  }

  @Test
  public void toFrontendShortName_leavesNonProEditionsUnchanged() {
    assertThat(NexusEditionType.toFrontendShortName("COMMUNITY"), is("COMMUNITY"));
    assertThat(NexusEditionType.toFrontendShortName("CORE"), is("CORE"));
  }

  @Test
  public void toFrontendShortName_leavesUnrecognisedShortNameUnchanged() {
    assertThat(NexusEditionType.toFrontendShortName("FOO"), is("FOO"));
    assertThat(NexusEditionType.toFrontendShortName(null), is((String) null));
  }
}
