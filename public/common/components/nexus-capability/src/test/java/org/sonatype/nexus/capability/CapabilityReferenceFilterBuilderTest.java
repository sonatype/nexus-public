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

import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.sonatype.nexus.capability.CapabilityIdentity.capabilityIdentity;
import static org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.capabilities;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * Tests {@link CapabilityReferenceFilterBuilder.CapabilityReferenceFilter} used as a {@link Predicate}.
 */
@ExtendWith(MockitoExtension.class)
class CapabilityReferenceFilterBuilderTest
{
  private static final CapabilityType TYPE = capabilityType("filter-test");

  @Mock
  private CapabilityReference reference;

  @Mock
  private CapabilityContext context;

  @Mock
  private CapabilityDescriptor descriptor;

  @BeforeEach
  void setUp() {
    lenient().when(reference.context()).thenReturn(context);
    lenient().when(context.descriptor()).thenReturn(descriptor);
    lenient().when(descriptor.isExposed()).thenReturn(true);
    lenient().when(context.type()).thenReturn(TYPE);
    lenient().when(context.id()).thenReturn(capabilityIdentity("id-1"));
    lenient().when(context.isEnabled()).thenReturn(true);
    lenient().when(context.isActive()).thenReturn(false);
    lenient().when(context.properties()).thenReturn(Map.of("key", "value"));
  }

  @Test
  void filterIsAJdkPredicate() {
    Predicate<CapabilityReference> predicate = capabilities().withType(TYPE);

    assertTrue(predicate.test(reference));
  }

  @Test
  void nullReferenceNeverMatches() {
    assertFalse(capabilities().test(null));
  }

  @Test
  void matchesOnType() {
    assertTrue(capabilities().withType(TYPE).test(reference));
    assertFalse(capabilities().withType(capabilityType("other")).test(reference));
  }

  @Test
  void matchesOnEnabledAndActive() {
    assertTrue(capabilities().enabled().passive().test(reference));
    assertFalse(capabilities().disabled().test(reference));
    assertFalse(capabilities().active().test(reference));
  }

  @Test
  void ignoresTheGivenCapabilityId() {
    assertFalse(capabilities().ignore(capabilityIdentity("id-1")).test(reference));
    assertTrue(capabilities().ignore(capabilityIdentity("id-2")).test(reference));
  }

  @Test
  void matchesOnProperties() {
    assertTrue(capabilities().withProperty("key", "value").test(reference));
    assertTrue(capabilities().withBoundedProperty("key").test(reference));
    assertFalse(capabilities().withProperty("key", "other").test(reference));
    assertFalse(capabilities().withBoundedProperty("missing").test(reference));
  }

  @Test
  void notExposedCapabilitiesAreOnlyMatchedWhenIncluded() {
    lenient().when(descriptor.isExposed()).thenReturn(false);

    assertFalse(capabilities().test(reference));
    assertTrue(capabilities().includeNotExposed().test(reference));
  }
}
