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
package org.sonatype.nexus.coreui.internal.previewui;

import java.util.Optional;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY;

@ExtendWith(AuthenticationExtension.class)
@WithUser
class PreviewUiSettingsResourceTest
    extends Test5Support
{
  @Mock
  private KeyValueStore keyValueStore;

  @Test
  void readIncludesDisableSwitchFeedback() {
    when(keyValueStore.getBoolean("preview.ui.anonymous.enabled")).thenReturn(Optional.of(false));
    when(keyValueStore.getBoolean("preview.ui.loggedin.enabled")).thenReturn(Optional.of(true));
    when(keyValueStore.getBoolean("preview.ui.default.enabled")).thenReturn(Optional.of(false));
    when(keyValueStore.getBoolean("preview.ui.legacy.disabled")).thenReturn(Optional.of(false));
    when(keyValueStore.getBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY)).thenReturn(Optional.of(true));

    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO result = underTest.read();

    assertThat(result.isDisableSwitchFeedback(), is(true));
  }

  @Test
  void updatePersistsDisableSwitchFeedback() {
    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO settings = new PreviewUiSettingsXO();
    settings.setAnonymousEnabled(false);
    settings.setLoggedInEnabled(true);
    settings.setDefaultToPreviewUi(false);
    settings.setDisableLegacyUi(false);
    settings.setDisableSwitchFeedback(true);

    underTest.update(settings);

    verify(keyValueStore).setBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY, true);
  }

  @Test
  void shouldRejectLockoutWhenDisableLegacyAndBothAccessDisabled() {
    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO settings = new PreviewUiSettingsXO();
    settings.setDisableLegacyUi(true);
    settings.setAnonymousEnabled(false);
    settings.setLoggedInEnabled(false);
    settings.setDefaultToPreviewUi(false);
    settings.setDisableSwitchFeedback(false);

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.update(settings));
    assertThat(exception.getResponse().getStatus(), is(BAD_REQUEST.getStatusCode()));
    assertThat(exception.getResponse().getEntity().toString(),
        org.hamcrest.Matchers.containsString("At least one UI access method must remain enabled"));
  }

  @Test
  void shouldRejectLockoutForCloudScenario() {
    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO settings = new PreviewUiSettingsXO();
    settings.setDisableLegacyUi(true);
    settings.setAnonymousEnabled(false);
    settings.setLoggedInEnabled(false);
    settings.setDefaultToPreviewUi(true);
    settings.setDisableSwitchFeedback(true);

    assertThrows(WebApplicationMessageException.class, () -> underTest.update(settings));
  }

  @Test
  void shouldAllowDisableLegacyWhenAnonymousEnabled() {
    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO settings = new PreviewUiSettingsXO();
    settings.setDisableLegacyUi(true);
    settings.setAnonymousEnabled(true);
    settings.setLoggedInEnabled(false);
    settings.setDefaultToPreviewUi(false);
    settings.setDisableSwitchFeedback(false);

    assertDoesNotThrow(() -> underTest.update(settings));
  }

  @Test
  void shouldAllowDisableLegacyWhenLoggedInEnabled() {
    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO settings = new PreviewUiSettingsXO();
    settings.setDisableLegacyUi(true);
    settings.setAnonymousEnabled(false);
    settings.setLoggedInEnabled(true);
    settings.setDefaultToPreviewUi(false);
    settings.setDisableSwitchFeedback(false);

    assertDoesNotThrow(() -> underTest.update(settings));
  }

  @Test
  void shouldAllowDisableLegacyWhenBothAccessEnabled() {
    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO settings = new PreviewUiSettingsXO();
    settings.setDisableLegacyUi(true);
    settings.setAnonymousEnabled(true);
    settings.setLoggedInEnabled(true);
    settings.setDefaultToPreviewUi(false);
    settings.setDisableSwitchFeedback(false);

    assertDoesNotThrow(() -> underTest.update(settings));
  }

  @Test
  void shouldAllowAllAccessDisabledWhenLegacyNotDisabled() {
    PreviewUiSettingsResource underTest =
        new PreviewUiSettingsResource(keyValueStore, false, false, false, false, false);

    PreviewUiSettingsXO settings = new PreviewUiSettingsXO();
    settings.setDisableLegacyUi(false);
    settings.setAnonymousEnabled(false);
    settings.setLoggedInEnabled(false);
    settings.setDefaultToPreviewUi(false);
    settings.setDisableSwitchFeedback(false);

    assertDoesNotThrow(() -> underTest.update(settings));
  }
}
