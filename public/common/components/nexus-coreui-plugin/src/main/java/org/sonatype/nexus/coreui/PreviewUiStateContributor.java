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

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_ANONYMOUS_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_AUDIT_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_DEFAULT_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_LEGACY_DISABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_LOGGEDIN_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_NAMED_VALUE;

/**
 * State contributor for Preview UI feature flags.
 *
 * Exposes the following state values to the frontend:
 * - anonymousEnabled: Whether anonymous users can access Preview UI
 * - loggedInEnabled: Whether logged-in users can access Preview UI
 * - defaultToPreviewUi: Whether to default to Preview UI for all users
 * - disableLegacyUi: Whether to completely disable the Heritage UI
 * - disableSwitchFeedback: Whether to skip the feedback prompt on Classic UI switch
 * - previewAuditEnabled: Whether Preview UI audit DB persistence and audit log page are enabled
 *
 * Settings are read from the database via KeyValueStore with system property fallbacks.
 * Database values take precedence; if not set, system properties (nexus.properties) are used.
 * previewAuditEnabled is system-property only (no KV store entry).
 */
@Component
@Singleton
public class PreviewUiStateContributor
    implements StateContributor
{
  private static final String KEY_ANONYMOUS_ENABLED = "preview.ui.anonymous.enabled";

  private static final String KEY_LOGGEDIN_ENABLED = "preview.ui.loggedin.enabled";

  private static final String KEY_DEFAULT_TO_PREVIEW = "preview.ui.default.enabled";

  private static final String KEY_DISABLE_LEGACY = "preview.ui.legacy.disabled";

  private final KeyValueStore keyValueStore;

  private final boolean defaultAnonymousEnabled;

  private final boolean defaultLoggedInEnabled;

  private final boolean defaultDefaultToPreview;

  private final boolean defaultLegacyDisabled;

  private final boolean defaultSwitchFeedbackDisabled;

  private final boolean auditEnabled;

  @Inject
  public PreviewUiStateContributor(
      final KeyValueStore keyValueStore,
      @Value(PREVIEW_UI_ANONYMOUS_ENABLED_NAMED_VALUE) final boolean defaultAnonymousEnabled,
      @Value(PREVIEW_UI_LOGGEDIN_ENABLED_NAMED_VALUE) final boolean defaultLoggedInEnabled,
      @Value(PREVIEW_UI_DEFAULT_ENABLED_NAMED_VALUE) final boolean defaultDefaultToPreview,
      @Value(PREVIEW_UI_LEGACY_DISABLED_NAMED_VALUE) final boolean defaultLegacyDisabled,
      @Value(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_NAMED_VALUE) final boolean defaultSwitchFeedbackDisabled,
      @Value(PREVIEW_UI_AUDIT_ENABLED_NAMED_VALUE) final boolean auditEnabled)
  {
    this.keyValueStore = checkNotNull(keyValueStore);
    this.defaultAnonymousEnabled = defaultAnonymousEnabled;
    this.defaultLoggedInEnabled = defaultLoggedInEnabled;
    this.defaultDefaultToPreview = defaultDefaultToPreview;
    this.defaultLegacyDisabled = defaultLegacyDisabled;
    this.defaultSwitchFeedbackDisabled = defaultSwitchFeedbackDisabled;
    this.auditEnabled = auditEnabled;
  }

  @Override
  @Nullable
  public Map<String, Object> getState() {
    return ImmutableMap.of(
        "anonymousEnabled", keyValueStore.getBoolean(KEY_ANONYMOUS_ENABLED).orElse(defaultAnonymousEnabled),
        "loggedInEnabled", keyValueStore.getBoolean(KEY_LOGGEDIN_ENABLED).orElse(defaultLoggedInEnabled),
        "defaultToPreviewUi", keyValueStore.getBoolean(KEY_DEFAULT_TO_PREVIEW).orElse(defaultDefaultToPreview),
        "disableLegacyUi", keyValueStore.getBoolean(KEY_DISABLE_LEGACY).orElse(defaultLegacyDisabled),
        "disableSwitchFeedback",
        keyValueStore.getBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY).orElse(defaultSwitchFeedbackDisabled),
        "previewAuditEnabled", auditEnabled);
  }
}
