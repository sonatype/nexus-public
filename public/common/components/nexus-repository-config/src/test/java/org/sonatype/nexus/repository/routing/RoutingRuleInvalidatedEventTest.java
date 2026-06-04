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
package org.sonatype.nexus.repository.routing;

import java.util.UUID;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.event.EventWithSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RoutingRuleInvalidatedEventTest
{
  // Core fix: event must extend EventWithSource to be picked up by DistributedEventPublisher
  @Test
  public void testExtendsEventWithSource() {
    assertTrue(EventWithSource.class.isAssignableFrom(RoutingRuleInvalidatedEvent.class));
  }

  // Required by Jackson for deserialization on remote nodes
  @Test
  public void testNoArgConstructorExists() throws Exception {
    RoutingRuleInvalidatedEvent event = RoutingRuleInvalidatedEvent.class.getDeclaredConstructor().newInstance();
    assertNotNull(event);
  }

  @Test
  public void testRoutingRuleIdPreserved() {
    UUID uuid = UUID.randomUUID();
    EntityId id = new EntityUUID(uuid);

    RoutingRuleInvalidatedEvent event = new RoutingRuleInvalidatedEvent(id);

    assertThat(event.getRoutingRuleId(), is(uuid.toString()));
  }

  @Test
  public void testSetRoutingRuleId() {
    RoutingRuleInvalidatedEvent event = new RoutingRuleInvalidatedEvent();
    String id = UUID.randomUUID().toString();

    event.setRoutingRuleId(id);

    assertThat(event.getRoutingRuleId(), is(id));
  }

  // Verifies the event survives Jackson round-trip used by the distributed event pipeline
  @Test
  public void testJacksonSerializable() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    UUID uuid = UUID.randomUUID();
    EntityId id = new EntityUUID(uuid);

    RoutingRuleInvalidatedEvent original = new RoutingRuleInvalidatedEvent(id);
    String json = mapper.writeValueAsString(original);
    RoutingRuleInvalidatedEvent restored = mapper.readValue(json, RoutingRuleInvalidatedEvent.class);

    assertThat(restored.getRoutingRuleId(), is(uuid.toString()));
  }
}
