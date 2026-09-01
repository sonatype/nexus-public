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
package org.sonatype.nexus.audit.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.i18n.I18N;
import org.sonatype.nexus.common.i18n.MessageBundle;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.audit.internal.AuditCapability.Configuration;
import org.sonatype.nexus.capability.CapabilityConfigurationSupport;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

@Component(AuditCapability.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuditCapability
    extends CapabilitySupport<Configuration>
{
  public static final String TYPE_ID = "audit";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Audit")
    String name();

    @DefaultMessage("Audit")
    String category();

    @DefaultMessage("Disabled")
    String disabled();

    @DefaultMessage("Enabled")
    String enabled();

    @DefaultMessage("Retention (days)")
    String retentionDaysLabel();

    @DefaultMessage("Number of days audit events are retained before pruning. Default 90; minimum 1; maximum %s.")
    String retentionDaysHelp(int max);
  }

  static final Messages messages = I18N.create(Messages.class);

  private final AuditRecorder auditRecorder;

  private final AuditRetentionSettings retentionSettings;

  @Autowired
  public AuditCapability(final AuditRecorder auditRecorder, final AuditRetentionSettings retentionSettings) {
    this.auditRecorder = checkNotNull(auditRecorder);
    this.retentionSettings = checkNotNull(retentionSettings);
  }

  @Override
  protected Configuration createConfig(final Map<String, String> properties) {
    return new Configuration(properties, retentionSettings.getMaxRetentionDays());
  }

  @Override
  @Nullable
  protected String renderDescription() {
    if (context().isActive()) {
      return messages.enabled();
    }
    return messages.disabled();
  }

  @Override
  public Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate();
  }

  @Override
  protected void onActivate(final Configuration config) {
    if (auditRecorder instanceof AuditRecorderImpl) {
      ((AuditRecorderImpl) auditRecorder).setEnabled(true);
    }
    retentionSettings.setRetentionDays(config.getRetentionDays());
  }

  @Override
  protected void onUpdate(final Configuration config) {
    retentionSettings.setRetentionDays(config.getRetentionDays());
  }

  @Override
  protected void onPassivate(final Configuration config) {
    if (auditRecorder instanceof AuditRecorderImpl) {
      ((AuditRecorderImpl) auditRecorder).setEnabled(false);
    }
    // Intentionally does not reset retentionSettings: the cleanup task runs independently of the
    // capability lifecycle so existing rows continue to be pruned with the last-known retention.
  }

  public static class Configuration
      extends CapabilityConfigurationSupport
  {
    public static final String RETENTION_DAYS = "retentionDays";

    private final int retentionDays;

    public Configuration(final Map<String, String> properties, final int maxRetentionDays) {
      checkNotNull(properties);
      String raw = properties.get(RETENTION_DAYS);
      int parsed;
      try {
        parsed = parseInteger(raw, AuditRetentionSettings.DEFAULT_RETENTION_DAYS);
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "retentionDays must be an integer between 1 and " + maxRetentionDays + ", got '" + raw + "'",
            e);
      }
      if (parsed < 1 || parsed > maxRetentionDays) {
        throw new IllegalArgumentException(
            "retentionDays must be between 1 and " + maxRetentionDays + ", got " + parsed);
      }
      this.retentionDays = parsed;
    }

    public int getRetentionDays() {
      return retentionDays;
    }

    @Override
    public String toString() {
      return "Configuration{retentionDays=" + retentionDays + '}';
    }
  }

  @AvailabilityVersion(from = "1.0")
  @Component
  @Qualifier(AuditCapability.TYPE_ID)
  public static class Descriptor
      extends CapabilityDescriptorSupport<Configuration>
      implements Taggable
  {
    private final AuditRetentionSettings retentionSettings;

    @Autowired
    public Descriptor(final AuditRetentionSettings retentionSettings) {
      this.retentionSettings = checkNotNull(retentionSettings);
      setExposed(true);
      setHidden(false);
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
      int max = retentionSettings.getMaxRetentionDays();
      return List.of(
          new NumberTextFormField(
              Configuration.RETENTION_DAYS,
              messages.retentionDaysLabel(),
              messages.retentionDaysHelp(max),
              FormField.MANDATORY)
                  .withInitialValue(AuditRetentionSettings.DEFAULT_RETENTION_DAYS)
                  .withMinimumValue(1)
                  .withMaximumValue(max));
    }

    @Override
    protected Configuration createConfig(final Map<String, String> properties) {
      return new Configuration(properties, retentionSettings.getMaxRetentionDays());
    }

    @Override
    protected String renderAbout() {
      return render(TYPE_ID + "-about.vm");
    }

    @Override
    public Set<Tag> getTags() {
      return singleton(Tag.categoryTag(messages.category()));
    }
  }
}
