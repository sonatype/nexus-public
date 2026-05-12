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
package org.sonatype.nexus.capability;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CapabilityDTOTest
{
  @Mock
  private CapabilityReference reference;

  @Mock
  private CapabilityContext context;

  @Mock
  private CapabilityDescriptor descriptor;

  @Mock
  private Capability capability;

  @Mock
  private CapabilityIdentity identity;

  @Mock
  private CapabilityType type;

  @Before
  public void setup() {
    when(reference.context()).thenReturn(context);
    when(reference.capability()).thenReturn(capability);
    when(context.descriptor()).thenReturn(descriptor);
    when(context.id()).thenReturn(identity);
    when(context.type()).thenReturn(type);
    when(identity.toString()).thenReturn("test-id");
    when(type.toString()).thenReturn("test-type");
    when(descriptor.name()).thenReturn("Test Capability");
    when(context.properties()).thenReturn(Collections.emptyMap());
  }

  @Test
  public void testNewFieldsPopulatedForActiveCapability() {
    when(context.isEnabled()).thenReturn(true);
    when(context.isActive()).thenReturn(true);
    when(context.hasFailure()).thenReturn(false);
    when(context.stateDescription()).thenReturn("Running normally");
    when(capability.description()).thenReturn("A test capability description");

    CapabilityDTO dto = new CapabilityDTO(reference);

    assertEquals("test-id", dto.getId());
    assertEquals("test-type", dto.getType());
    assertEquals("Test Capability", dto.getTypeName());
    assertTrue(dto.isEnabled());
    assertTrue(dto.isActive());
    assertFalse(dto.isError());
    assertEquals("active", dto.getState());
    assertEquals("Running normally", dto.getStateDescription());
    assertEquals("A test capability description", dto.getDescription());
  }

  @Test
  public void testStateComputedAsDisabled() {
    when(context.isEnabled()).thenReturn(false);
    when(context.isActive()).thenReturn(false);
    when(context.hasFailure()).thenReturn(false);

    CapabilityDTO dto = new CapabilityDTO(reference);

    assertEquals("disabled", dto.getState());
    assertFalse(dto.isEnabled());
    assertFalse(dto.isActive());
    assertFalse(dto.isError());
  }

  @Test
  public void testStateComputedAsError() {
    when(context.isEnabled()).thenReturn(true);
    when(context.isActive()).thenReturn(false);
    when(context.hasFailure()).thenReturn(true);

    CapabilityDTO dto = new CapabilityDTO(reference);

    assertEquals("error", dto.getState());
    assertTrue(dto.isEnabled());
    assertFalse(dto.isActive());
    assertTrue(dto.isError());
  }

  @Test
  public void testStateComputedAsPassive() {
    when(context.isEnabled()).thenReturn(true);
    when(context.isActive()).thenReturn(false);
    when(context.hasFailure()).thenReturn(false);

    CapabilityDTO dto = new CapabilityDTO(reference);

    assertEquals("passive", dto.getState());
    assertTrue(dto.isEnabled());
    assertFalse(dto.isActive());
    assertFalse(dto.isError());
  }

  @Test
  public void testDescriptionNullWhenCapabilityReturnsNull() {
    when(context.isEnabled()).thenReturn(true);
    when(context.isActive()).thenReturn(true);
    when(context.hasFailure()).thenReturn(false);
    when(capability.description()).thenReturn(null);

    CapabilityDTO dto = new CapabilityDTO(reference);

    assertNull(dto.getDescription());
  }

  @Test
  public void testTagsPopulatedFromTaggableDescriptor() {
    // Create a mock that implements both CapabilityDescriptor and Taggable
    TaggableCapabilityDescriptor taggableDescriptor =
        org.mockito.Mockito.mock(TaggableCapabilityDescriptor.class);
    when(taggableDescriptor.name()).thenReturn("Taggable Test");
    when(taggableDescriptor.getTags()).thenReturn(Set.of(new Tag("key1", "value1")));
    when(context.descriptor()).thenReturn(taggableDescriptor);
    when(context.isEnabled()).thenReturn(true);
    when(context.isActive()).thenReturn(true);
    when(context.hasFailure()).thenReturn(false);

    CapabilityDTO dto = new CapabilityDTO(reference);

    assertNotNull(dto.getTags());
    assertEquals("value1", dto.getTags().get("key1"));
  }

  @Test
  public void testTagsPopulatedFromTaggableCapability() {
    // Create a mock capability that implements Taggable
    TaggableCapability taggableCapability = org.mockito.Mockito.mock(TaggableCapability.class);
    when(taggableCapability.getTags()).thenReturn(Set.of(new Tag("capKey", "capValue")));
    when(reference.capability()).thenReturn(taggableCapability);
    when(context.isEnabled()).thenReturn(true);
    when(context.isActive()).thenReturn(true);
    when(context.hasFailure()).thenReturn(false);

    CapabilityDTO dto = new CapabilityDTO(reference);

    assertNotNull(dto.getTags());
    assertEquals("capValue", dto.getTags().get("capKey"));
  }

  @Test
  public void testDefaultConstructorForDeserialization() {
    CapabilityDTO dto = new CapabilityDTO();

    // All fields should be null/default
    assertNull(dto.getId());
    assertNull(dto.getType());
    assertNull(dto.getTypeName());
    assertNull(dto.getState());
    assertNull(dto.getStateDescription());
    assertNull(dto.getDescription());
    assertFalse(dto.isEnabled());
    assertFalse(dto.isActive());
    assertFalse(dto.isError());
  }

  @Test
  public void testSettersWork() {
    CapabilityDTO dto = new CapabilityDTO();

    dto.setId("my-id");
    dto.setType("my-type");
    dto.setTypeName("My Type Name");
    dto.setEnabled(true);
    dto.setActive(true);
    dto.setError(false);
    dto.setState("active");
    dto.setStateDescription("All good");
    dto.setDescription("My description");
    dto.setTags(Map.of("tagKey", "tagValue"));

    assertEquals("my-id", dto.getId());
    assertEquals("my-type", dto.getType());
    assertEquals("My Type Name", dto.getTypeName());
    assertTrue(dto.isEnabled());
    assertTrue(dto.isActive());
    assertFalse(dto.isError());
    assertEquals("active", dto.getState());
    assertEquals("All good", dto.getStateDescription());
    assertEquals("My description", dto.getDescription());
    assertEquals("tagValue", dto.getTags().get("tagKey"));
  }

  /**
   * Interface for creating mock that implements both CapabilityDescriptor and Taggable.
   */
  private interface TaggableCapabilityDescriptor
      extends CapabilityDescriptor, Taggable
  {
  }

  /**
   * Interface for creating mock that implements both Capability and Taggable.
   */
  private interface TaggableCapability
      extends Capability, Taggable
  {
  }
}
