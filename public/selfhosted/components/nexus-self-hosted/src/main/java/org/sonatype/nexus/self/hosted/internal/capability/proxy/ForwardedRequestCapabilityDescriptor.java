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
package org.sonatype.nexus.self.hosted.internal.capability.proxy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static java.util.Collections.singleton;

/**
 * Descriptor for ForwardedRequestCapability.
 */
@AvailabilityVersion(from = ForwardedRequestCapabilityUpgradeStep_2_131.VERSION)
@Component
@Qualifier(ForwardedRequestCapability.TYPE_ID)
public class ForwardedRequestCapabilityDescriptor
    extends CapabilityDescriptorSupport<ForwardedRequestCapabilityConfiguration>
    implements Taggable
{
  private final List<FormField> formFields;

  @Autowired
  public ForwardedRequestCapabilityDescriptor() {
    formFields = List.of(
        new CheckboxFormField(
            "enabled",
            "Enable processing of forwarded headers",
            "When enabled, X-Forwarded-For, X-Forwarded-Proto and related headers will be processed. "
                + "When disabled, the original connection information is preserved.",
            FormField.OPTIONAL).withInitialValue(true));
    setExposed(true);
    setHidden(false);
  }

  @Override
  public CapabilityType type() {
    return ForwardedRequestCapability.TYPE;
  }

  @Override
  public String name() {
    return ForwardedRequestCapability.messages.name();
  }

  @Override
  public List<FormField> formFields() {
    return formFields;
  }

  @Override
  protected ForwardedRequestCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new ForwardedRequestCapabilityConfiguration(properties);
  }

  @Override
  protected String renderAbout() {
    return render(ForwardedRequestCapability.TYPE_ID + "-about.vm");
  }

  @Override
  public Set<Tag> getTags() {
    return singleton(Tag.categoryTag(ForwardedRequestCapability.messages.category()));
  }
}
