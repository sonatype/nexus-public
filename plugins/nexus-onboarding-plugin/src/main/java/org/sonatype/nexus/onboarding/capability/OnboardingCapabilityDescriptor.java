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
package org.sonatype.nexus.onboarding.capability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;

import static java.util.Collections.singletonList;


@AvailabilityVersion(from = "1.0")
@Named(OnboardingCapability.TYPE_ID)
@Singleton
public class OnboardingCapabilityDescriptor
    extends CapabilityDescriptorSupport<OnboardingCapabilityConfiguration>
    implements Taggable
{
  static final Messages messages = I18N.create(Messages.class);

  public OnboardingCapabilityDescriptor() {
    setExposed(false);
    setHidden(true);
  }

  @Override
  public CapabilityType type() {
    return OnboardingCapability.TYPE;
  }

  @Override
  public String name() {
    return messages.name();
  }

  @Override
  public List<FormField> formFields() {
    return new ArrayList<>();
  }

  @Override
  public Set<Tag> getTags() {
    return new HashSet<>(singletonList(Tag.categoryTag(messages.category())));
  }

  interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Onboarding wizard")
    String name();

    @DefaultMessage("Onboarding")
    String category();

    @DefaultMessage("Enabled")
    String enabled();

    @DefaultMessage("Disabled")
    String disabled();
  }
}
