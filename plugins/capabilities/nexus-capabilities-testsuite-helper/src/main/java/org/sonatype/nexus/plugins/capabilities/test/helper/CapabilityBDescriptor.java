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
package org.sonatype.nexus.plugins.capabilities.test.helper;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.support.CapabilityDescriptorSupport;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;

import com.google.common.collect.Lists;

import static org.sonatype.nexus.plugins.capabilities.CapabilityType.capabilityType;

@Named(CapabilityBDescriptor.TYPE_ID)
@Singleton
public class CapabilityBDescriptor
    extends TestCapabilityDescriptor
{

  static final String TYPE_ID = "[b]";

  static final CapabilityType TYPE = capabilityType(TYPE_ID);

  static final String PROPERTY_B1 = "b1";

  private final List<FormField> formFields;

  protected CapabilityBDescriptor() {
    formFields = Lists.<FormField>newArrayList(
        new StringTextFormField(PROPERTY_B1, "Property B1", "Some help text", FormField.MANDATORY)
    );
  }

  @Override
  public CapabilityType type() {
    return TYPE;
  }

  @Override
  public String name() {
    return "Capability B";
  }

  @Override
  public List<FormField> formFields() {
    return formFields;
  }

}
