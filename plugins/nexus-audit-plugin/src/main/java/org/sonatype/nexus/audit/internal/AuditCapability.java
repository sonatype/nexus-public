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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.audit.internal.AuditCapability.Configuration;
import org.sonatype.nexus.capability.CapabilityConfigurationSupport;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.formfields.FormField;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

@Named(AuditCapability.TYPE_ID)
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
  }

  static final Messages messages = I18N.create(Messages.class);

  private final AuditRecorder auditRecorder;

  @Inject
  public AuditCapability(final AuditRecorder auditRecorder) {
    this.auditRecorder = checkNotNull(auditRecorder);
  }

  @Override
  protected Configuration createConfig(final Map<String, String> properties) {
    return new Configuration(properties);
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
  }

  @Override
  protected void onPassivate(final Configuration config) {
    if (auditRecorder instanceof AuditRecorderImpl) {
      ((AuditRecorderImpl) auditRecorder).setEnabled(false);
    }
  }

  public static class Configuration
      extends CapabilityConfigurationSupport
  {
    public Configuration(final Map<String, String> properties) {
      // . . .
    }
  }

  @Named(AuditCapability.TYPE_ID)
  @Singleton
  public static class Descriptor
      extends CapabilityDescriptorSupport<Configuration>
      implements Taggable
  {
    public Descriptor() {
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
      return emptyList();
    }

    @Override
    protected Configuration createConfig(final Map<String, String> properties) {
      return new Configuration(properties);
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
