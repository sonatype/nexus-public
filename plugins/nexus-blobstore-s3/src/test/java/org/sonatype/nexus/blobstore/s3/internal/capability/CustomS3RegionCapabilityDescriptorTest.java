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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomS3RegionCapabilityDescriptorTest {

  private CustomS3RegionCapabilityDescriptor descriptor;

  @Before
  public void setUp() {
    descriptor = new CustomS3RegionCapabilityDescriptor();
  }

  @Test
  public void testType() {
    CapabilityType type = descriptor.type();
    assertEquals("customs3regions", type.toString());
  }

  @Test
  public void testName() {
    String name = descriptor.name();
    assertEquals("Custom S3 Regions", name);
  }

  @Test
  public void testFormFields() {
    List<FormField> formFields = descriptor.formFields();
    assertNotNull(formFields);
    assertEquals(1, formFields.size());
    assertEquals("regions", formFields.get(0).getId());
  }

  @Test
  public void testCreateConfig() {
    Map<String, String> properties = new HashMap<>();
    properties.put("regions", "us-east-1,us-west-2");
    assertNotNull(descriptor.createConfig(properties));
  }
}
