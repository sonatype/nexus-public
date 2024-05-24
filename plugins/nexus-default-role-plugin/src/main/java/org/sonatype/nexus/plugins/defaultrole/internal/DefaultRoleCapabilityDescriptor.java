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
package org.sonatype.nexus.plugins.defaultrole.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.formfields.FormField;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;
import static org.sonatype.nexus.plugins.defaultrole.internal.DefaultRoleCapabilityConfiguration.P_ROLE;

/**
 * Descriptor of the {@link DefaultRoleCapability} for UI configuration
 *
 * @since 3.22
 */
@AvailabilityVersion(from = "1.0")
@Named(DefaultRoleCapabilityDescriptor.TYPE_ID)
@Singleton
public class DefaultRoleCapabilityDescriptor
    extends CapabilityDescriptorSupport<DefaultRoleCapabilityConfiguration>
{
  public static final String TYPE_ID = "defaultrole";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Default Role")
    String name();

    @DefaultMessage("Role")
    String roleLabel();

    @DefaultMessage("The role which is automatically granted to authenticated users")
    String roleHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final ComboboxFormField role;

  public DefaultRoleCapabilityDescriptor() {
    setExposed(true);
    setHidden(false);

    this.role = new ComboboxFormField<String>(
        P_ROLE,
        messages.roleLabel(),
        messages.roleHelp(),
        FormField.MANDATORY
    ).withStoreApi("coreui_Role.read");
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
    return singletonList(role);
  }

  @Override
  protected String renderAbout() {
    return render(TYPE_ID + "-about.vm");
  }
}
