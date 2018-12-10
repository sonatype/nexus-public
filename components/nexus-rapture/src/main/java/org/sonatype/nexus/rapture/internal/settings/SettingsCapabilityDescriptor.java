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
package org.sonatype.nexus.rapture.internal.settings;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.StringTextFormField;

import com.google.common.collect.Lists;

/**
 * {@link SettingsCapability} descriptor.
 *
 * @since 3.0
 */
@Named(SettingsCapabilityDescriptor.TYPE_ID)
@Singleton
public class SettingsCapabilityDescriptor
    extends CapabilityDescriptorSupport<SettingsCapabilityConfiguration>
    implements Taggable
{
  public static final String TYPE_ID = "rapture.settings";

  public static final CapabilityType TYPE = CapabilityType.capabilityType(TYPE_ID);

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("UI: Settings")
    String name();

    @DefaultMessage("Debug allowed")
    String debugAllowedLabel();

    @DefaultMessage("Allow developer debugging")
    String debugAllowedHelp();

    @DefaultMessage("Authenticated user status interval")
    String statusIntervalAuthenticatedLabel();

    @DefaultMessage("Interval between status requests for authenticated users (seconds)")
    String statusIntervalAuthenticatedHelp();

    @DefaultMessage("Anonymous user status interval")
    String statusIntervalAnonymousLabel();

    @DefaultMessage("Interval between status requests for anonymous user (seconds)")
    String statusIntervalAnonymousHelp();

    @DefaultMessage("Session timeout")
    String sessionTimeoutLabel();

    @DefaultMessage(
        "Period of inactivity before session times out (minutes). A value of 0 will mean that a session never expires."
    )
    String sessionTimeoutHelp();

    @DefaultMessage("Standard request timeout")
    String requestTimeoutLabel();

    @DefaultMessage("Period of time to keep the connection alive for requests expected to take a normal period of time (seconds)")
    String requestTimeoutHelp();

    @DefaultMessage("Extended request timeout")
    String longRequestTimeoutLabel();

    @DefaultMessage("Period of time to keep the connection alive for requests expected to take an extended period of time (seconds)")
    String longRequestTimeoutHelp();

    @DefaultMessage("Title")
    String titleLabel();

    @DefaultMessage("Browser page title")
    String titleHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  public SettingsCapabilityDescriptor() {
    formFields = Lists.<FormField>newArrayList(
        new StringTextFormField(
            SettingsCapabilityConfiguration.TITLE,
            messages.titleLabel(),
            messages.titleHelp(),
            FormField.MANDATORY
        ).withInitialValue(RaptureSettings.DEFAULT_TITLE),
        new CheckboxFormField(
            SettingsCapabilityConfiguration.DEBUG_ALLOWED,
            messages.debugAllowedLabel(),
            messages.debugAllowedHelp(),
            FormField.OPTIONAL
        ).withInitialValue(RaptureSettings.DEFAULT_DEBUG_ALLOWED),
        new NumberTextFormField(
            SettingsCapabilityConfiguration.STATUS_INTERVAL_AUTHENTICATED,
            messages.statusIntervalAuthenticatedLabel(),
            messages.statusIntervalAuthenticatedHelp(),
            FormField.MANDATORY
        ).withInitialValue(RaptureSettings.DEFAULT_STATUS_INTERVAL_AUTHENTICATED),
        new NumberTextFormField(
            SettingsCapabilityConfiguration.STATUS_INTERVAL_ANONYMOUS,
            messages.statusIntervalAnonymousLabel(),
            messages.statusIntervalAnonymousHelp(),
            FormField.MANDATORY
        ).withInitialValue(RaptureSettings.DEFAULT_STATUS_INTERVAL_ANONYMOUS),
        new NumberTextFormField(
            SettingsCapabilityConfiguration.SESSION_TIMEOUT,
            messages.sessionTimeoutLabel(),
            messages.sessionTimeoutHelp(),
            FormField.MANDATORY
        ).withInitialValue(RaptureSettings.DEFAULT_SESSION_TIMEOUT),
        new NumberTextFormField(
            SettingsCapabilityConfiguration.REQUEST_TIMEOUT,
            messages.requestTimeoutLabel(),
            messages.requestTimeoutHelp(),
            FormField.MANDATORY
        ).withInitialValue(RaptureSettings.DEFAULT_REQUEST_TIMEOUT)
            .withMinimumValue(RaptureSettings.MIN_REQUEST_TIMEOUT),
        new NumberTextFormField(
            SettingsCapabilityConfiguration.LONG_REQUEST_TIMEOUT,
            messages.longRequestTimeoutLabel(),
            messages.longRequestTimeoutHelp(),
            FormField.MANDATORY
        ).withInitialValue(RaptureSettings.DEFAULT_LONG_REQUEST_TIMEOUT)
            .withMinimumValue(RaptureSettings.DEFAULT_LONG_REQUEST_TIMEOUT)
    );
  }

  @Override
  public CapabilityType type() {
    return TYPE;
  }

  @Override
  public String name() {
    return messages.name();
  }

  @Override
  public List<FormField> formFields() {
    return formFields;
  }

  @Override
  protected SettingsCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new SettingsCapabilityConfiguration(properties);
  }

  @Override
  protected String renderAbout() throws Exception {
    return render(TYPE_ID + "-about.vm");
  }

  @Override
  public Set<Tag> getTags() {
    return Tag.tags(Tag.categoryTag("UI"));
  }

}
