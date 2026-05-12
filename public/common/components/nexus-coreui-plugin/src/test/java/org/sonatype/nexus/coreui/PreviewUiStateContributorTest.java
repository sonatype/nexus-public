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
package org.sonatype.nexus.coreui;

import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.kv.KeyValueStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY;

class PreviewUiStateContributorTest
    extends Test5Support
{
  @Mock
  private KeyValueStore keyValueStore;

  @BeforeEach
  public void setUp() {
    when(keyValueStore.getBoolean(anyString())).thenReturn(Optional.empty());
  }

  @Test
  void getStateIncludesDisableSwitchFeedback() {
    when(keyValueStore.getBoolean("preview.ui.anonymous.enabled")).thenReturn(Optional.of(false));
    when(keyValueStore.getBoolean("preview.ui.loggedin.enabled")).thenReturn(Optional.of(true));
    when(keyValueStore.getBoolean("preview.ui.default.enabled")).thenReturn(Optional.of(false));
    when(keyValueStore.getBoolean("preview.ui.legacy.disabled")).thenReturn(Optional.of(false));
    when(keyValueStore.getBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY)).thenReturn(Optional.of(true));

    PreviewUiStateContributor underTest = contributor(false);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get("disableSwitchFeedback"), is(true));
  }

  @Test
  void exposesPreviewAuditEnabledTrue() {
    PreviewUiStateContributor underTest = contributor(true);

    assertThat(underTest.getState().get("previewAuditEnabled"), is(true));
  }

  @Test
  void exposesPreviewAuditEnabledFalse() {
    PreviewUiStateContributor underTest = contributor(false);

    assertThat(underTest.getState().get("previewAuditEnabled"), is(false));
  }

  @Test
  void previewAuditEnabledIsNotReadFromKeyValueStore() {
    // The audit flag is system-property only by design (NEXUS-52060) — KV store lookups for it
    // should never happen, so an admin cannot accidentally enable audit persistence via the
    // preview-ui settings page. Stub is lenient because the production code never issues this
    // call — that absence is the invariant under test.
    lenient().when(keyValueStore.getBoolean("preview.ui.audit.enabled")).thenReturn(Optional.of(true));

    PreviewUiStateContributor underTest = contributor(false);

    assertThat(underTest.getState().get("previewAuditEnabled"), is(false));
  }

  private PreviewUiStateContributor contributor(final boolean auditEnabled) {
    return new PreviewUiStateContributor(keyValueStore, false, false, false, false, false, auditEnabled);
  }
}
