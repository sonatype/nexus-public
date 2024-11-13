
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
package org.sonatype.nexus.blobstore.s3.internal.capability;

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
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;

import com.google.common.collect.ImmutableList;

import static org.sonatype.nexus.capability.Tag.categoryTag;
import static org.sonatype.nexus.capability.Tag.tags;

@Named(CustomS3RegionCapabilityDescriptor.TYPE_ID)
@Singleton
@AvailabilityVersion(from = "2.4")
public class CustomS3RegionCapabilityDescriptor
    extends CapabilityDescriptorSupport<CustomS3RegionCapabilityConfiguration>
    implements Taggable
{
  public static final String TYPE_ID = "customs3regions";

  public static final CapabilityType TYPE_NAME = CapabilityType.capabilityType(TYPE_ID);

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Custom S3 Regions")
    String name();

    @DefaultMessage("Regions")
    String regionsLabel();

    @DefaultMessage("Custom S3 Regions, separated by commas")
    String regionsHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  @SuppressWarnings("rawtypes")
  private final List<FormField> formFields;

  public CustomS3RegionCapabilityDescriptor()
  {
    formFields = ImmutableList.of(
        new StringTextFormField(
            CustomS3RegionCapabilityConfiguration.REGIONS,
            messages.regionsLabel(),
            messages.regionsHelp(),
            FormField.MANDATORY
        )
    );
  }

  @Override
  public CapabilityType type() { return TYPE_NAME; }

  @Override
  public String name() { return messages.name(); }

  @Override
  public List<FormField> formFields() { return formFields; }

  @Override
  public Set<Tag> getTags() { return tags(categoryTag("S3")); }

  @Override
  protected CustomS3RegionCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new CustomS3RegionCapabilityConfiguration(properties);
  }

  @Override
  protected String renderAbout() throws Exception {
    return render(TYPE_ID + "-about.vm");
  }
}
