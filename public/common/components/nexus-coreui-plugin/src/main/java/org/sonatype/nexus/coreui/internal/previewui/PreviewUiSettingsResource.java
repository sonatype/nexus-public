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

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_ANONYMOUS_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_DEFAULT_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_LEGACY_DISABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_LOGGEDIN_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SETTINGS_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_NAMED_VALUE;

/**
 * REST resource for Preview UI settings.
 * Settings are stored in the database via the KeyValueStore with system property fallbacks.
 */
@Component
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@ConditionalOnProperty(name = PREVIEW_UI_SETTINGS_ENABLED, havingValue = "true", matchIfMissing = true)
@Path(PreviewUiSettingsResource.RESOURCE_PATH)
public class PreviewUiSettingsResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String RESOURCE_PATH = "internal/ui/preview-ui-settings";

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

  @Autowired
  public PreviewUiSettingsResource(
      final KeyValueStore keyValueStore,
      @Value(PREVIEW_UI_ANONYMOUS_ENABLED_NAMED_VALUE) final boolean defaultAnonymousEnabled,
      @Value(PREVIEW_UI_LOGGEDIN_ENABLED_NAMED_VALUE) final boolean defaultLoggedInEnabled,
      @Value(PREVIEW_UI_DEFAULT_ENABLED_NAMED_VALUE) final boolean defaultDefaultToPreview,
      @Value(PREVIEW_UI_LEGACY_DISABLED_NAMED_VALUE) final boolean defaultLegacyDisabled,
      @Value(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_NAMED_VALUE) final boolean defaultSwitchFeedbackDisabled)
  {
    this.keyValueStore = checkNotNull(keyValueStore);
    this.defaultAnonymousEnabled = defaultAnonymousEnabled;
    this.defaultLoggedInEnabled = defaultLoggedInEnabled;
    this.defaultDefaultToPreview = defaultDefaultToPreview;
    this.defaultLegacyDisabled = defaultLegacyDisabled;
    this.defaultSwitchFeedbackDisabled = defaultSwitchFeedbackDisabled;
  }

  @GET
  @RequiresPermissions("nexus:settings:read")
  public PreviewUiSettingsXO read() {
    PreviewUiSettingsXO xo = new PreviewUiSettingsXO();
    xo.setAnonymousEnabled(keyValueStore.getBoolean(KEY_ANONYMOUS_ENABLED).orElse(defaultAnonymousEnabled));
    xo.setLoggedInEnabled(keyValueStore.getBoolean(KEY_LOGGEDIN_ENABLED).orElse(defaultLoggedInEnabled));
    xo.setDefaultToPreviewUi(keyValueStore.getBoolean(KEY_DEFAULT_TO_PREVIEW).orElse(defaultDefaultToPreview));
    xo.setDisableLegacyUi(keyValueStore.getBoolean(KEY_DISABLE_LEGACY).orElse(defaultLegacyDisabled));
    xo.setDisableSwitchFeedback(
        keyValueStore.getBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY).orElse(defaultSwitchFeedbackDisabled));
    return xo;
  }

  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  public PreviewUiSettingsXO update(@NotNull @Valid final PreviewUiSettingsXO settings) {
    validateNoUiLockout(settings);
    keyValueStore.setBoolean(KEY_ANONYMOUS_ENABLED, settings.isAnonymousEnabled());
    keyValueStore.setBoolean(KEY_LOGGEDIN_ENABLED, settings.isLoggedInEnabled());
    keyValueStore.setBoolean(KEY_DEFAULT_TO_PREVIEW, settings.isDefaultToPreviewUi());
    keyValueStore.setBoolean(KEY_DISABLE_LEGACY, settings.isDisableLegacyUi());
    keyValueStore.setBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY, settings.isDisableSwitchFeedback());
    log.info(
        "Preview UI settings updated: anonymousEnabled={}, loggedInEnabled={}, defaultToPreviewUi={}, disableLegacyUi={}, disableSwitchFeedback={}",
        settings.isAnonymousEnabled(), settings.isLoggedInEnabled(), settings.isDefaultToPreviewUi(),
        settings.isDisableLegacyUi(), settings.isDisableSwitchFeedback());
    return read();
  }

  private void validateNoUiLockout(final PreviewUiSettingsXO settings) {
    if (settings.isDisableLegacyUi() && !settings.isAnonymousEnabled() && !settings.isLoggedInEnabled()) {
      throw new WebApplicationMessageException(BAD_REQUEST,
          "Cannot disable Classic UI when both Anonymous and Logged-in access to Nexus One UI are disabled. " +
              "At least one UI access method must remain enabled to prevent lockout.",
          APPLICATION_JSON);
    }
  }
}
