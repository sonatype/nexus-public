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
package org.sonatype.nexus.common.app.trash;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrashFormatGateTest
{
  @Test
  public void flagOffIsNeverEnabled() {
    TrashFormatGate gate = new TrashFormatGate(false, "maven2,npm");

    assertFalse(gate.isEnabled());
    assertFalse(gate.isEnabledForFormat("maven2"));
  }

  @Test
  public void flagOnWithEmptyAllowlistDisablesAllFormats() {
    TrashFormatGate gate = new TrashFormatGate(true, "");

    // Fail-closed: an empty allowlist means the feature is inert -- everything hard-deletes.
    assertFalse(gate.isEnabled());
    assertFalse(gate.isEnabledForFormat("maven2"));
    assertFalse(gate.isEnabledForFormat("npm"));
    assertFalse(gate.isEnabledForFormat(null));
  }

  @Test
  public void flagOnWithAllowlistRestrictsToListedFormats() {
    TrashFormatGate gate = new TrashFormatGate(true, "maven2,npm");

    assertTrue(gate.isEnabled());
    assertTrue(gate.isEnabledForFormat("maven2"));
    assertTrue(gate.isEnabledForFormat("npm"));
    assertFalse(gate.isEnabledForFormat("docker"));
    assertFalse(gate.isEnabledForFormat(null));
  }

  @Test
  public void allowlistMatchingIsCaseInsensitiveAndTrimmed() {
    TrashFormatGate gate = new TrashFormatGate(true, "  Maven2 , NPM ");

    assertTrue(gate.isEnabledForFormat("maven2"));
    assertTrue(gate.isEnabledForFormat("MAVEN2"));
    assertTrue(gate.isEnabledForFormat("npm"));
    assertFalse(gate.isEnabledForFormat("nuget"));
  }

  @Test
  public void wildcardEnablesAllFormats() {
    TrashFormatGate gate = new TrashFormatGate(true, "*");

    assertTrue(gate.isEnabled());
    assertTrue(gate.isEnabledForFormat("maven2"));
    assertTrue(gate.isEnabledForFormat("anything-else"));
    // a null format is still not a real format
    assertFalse(gate.isEnabledForFormat(null));
  }

  @Test
  public void wildcardIsIgnoredWhenFlagOff() {
    TrashFormatGate gate = new TrashFormatGate(false, "*");

    assertFalse(gate.isEnabled());
    assertFalse(gate.isEnabledForFormat("maven2"));
  }

  @Test
  public void wildcardMixedWithFormatsStillEnablesAll() {
    TrashFormatGate gate = new TrashFormatGate(true, " maven2 , * ");

    assertTrue(gate.isEnabledForFormat("npm"));
    assertTrue(gate.isEnabledForFormat("maven2"));
  }

  @Test
  public void nullAllowlistIsTreatedAsEmpty() {
    TrashFormatGate gate = new TrashFormatGate(true, null);

    // Fail-closed: a null/unset allowlist enables nothing.
    assertFalse(gate.isEnabled());
    assertFalse(gate.isEnabledForFormat("anything"));
  }
}
