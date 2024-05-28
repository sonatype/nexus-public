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
package org.sonatype.nexus.repository.httpbridge.legacy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.formfields.FormField;

import static org.sonatype.nexus.capability.Tag.categoryTag;
import static org.sonatype.nexus.capability.Tag.tags;

/**
 * {@link LegacyUrlCapability} descriptor.
 *
 * @since 3.7
 */
@Named(LegacyUrlCapabilityDescriptor.TYPE_ID)
@Singleton
public class LegacyUrlCapabilityDescriptor
    extends CapabilityDescriptorSupport<LegacyUrlCapabilityConfiguration>
    implements Taggable
{
  public static final String TYPE_ID = "LegacyUrlCapability";

  public static final CapabilityType TYPE = CapabilityType.capabilityType(TYPE_ID);

  @Override
  public CapabilityType type() {
    return TYPE;
  }

  @Override
  public String name() {
    return "NXRM2 style URLs";
  }

  @Override
  public Set<Tag> getTags() {
    return tags(categoryTag("Core"));
  }

  @Override
  public List<FormField> formFields() {
    return Collections.emptyList();
  }

  @Override
  protected LegacyUrlCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new LegacyUrlCapabilityConfiguration();
  }

  @Override
  public boolean isExposed() {
    return true;
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  protected String renderAbout() throws Exception {
    return render(TYPE_ID + "-about.vm");
  }
}
