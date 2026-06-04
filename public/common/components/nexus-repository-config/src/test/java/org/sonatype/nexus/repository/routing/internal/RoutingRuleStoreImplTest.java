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
package org.sonatype.nexus.repository.routing.internal;

import java.util.List;
import java.util.UUID;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRuleInvalidatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RoutingRuleStoreImplTest
{
  @Mock
  private DataSessionSupplier sessionSupplier;

  @Mock
  private EventManager eventManager;

  private RoutingRuleStoreImpl underTest;

  @Before
  public void setup() {
    // Subclass to bypass @Transactional DB operations — we only test event posting here
    underTest = new RoutingRuleStoreImpl(sessionSupplier, eventManager)
    {
      @Override
      protected void doUpdate(final RoutingRuleData rule) {
      }

      @Override
      protected void doDelete(final String name) {
      }
    };
  }

  @Test
  public void testUpdate_postsRoutingRuleInvalidatedEvent() {
    EntityId ruleId = new EntityUUID(UUID.randomUUID());
    RoutingRuleData rule = validRule(ruleId);

    underTest.update(rule);

    RoutingRuleInvalidatedEvent event = captureInvalidatedEvent();
    assertNotNull(event);
    assertThat(event.getRoutingRuleId(), is(ruleId.getValue()));
  }

  @Test
  public void testDelete_postsRoutingRuleInvalidatedEvent() {
    EntityId ruleId = new EntityUUID(UUID.randomUUID());
    RoutingRuleData rule = validRule(ruleId);

    underTest.delete(rule);

    RoutingRuleInvalidatedEvent event = captureInvalidatedEvent();
    assertNotNull(event);
    assertThat(event.getRoutingRuleId(), is(ruleId.getValue()));
  }

  // Regression guard: event must be a concrete class, not an anonymous inner class,
  // so Jackson can serialize it for the distributed event pipeline (NEXUS-50997)
  @Test
  public void testUpdate_invalidatedEventIsConcreteClass() {
    underTest.update(validRule(new EntityUUID(UUID.randomUUID())));

    RoutingRuleInvalidatedEvent event = captureInvalidatedEvent();
    assertThat(event.getClass(), is(RoutingRuleInvalidatedEvent.class));
  }

  @Test
  public void testDelete_invalidatedEventIsConcreteClass() {
    underTest.delete(validRule(new EntityUUID(UUID.randomUUID())));

    RoutingRuleInvalidatedEvent event = captureInvalidatedEvent();
    assertThat(event.getClass(), is(RoutingRuleInvalidatedEvent.class));
  }

  private RoutingRuleInvalidatedEvent captureInvalidatedEvent() {
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, atLeastOnce()).post(captor.capture());
    return captor.getAllValues()
        .stream()
        .filter(e -> e instanceof RoutingRuleInvalidatedEvent)
        .map(e -> (RoutingRuleInvalidatedEvent) e)
        .findFirst()
        .orElse(null);
  }

  private static RoutingRuleData validRule(final EntityId id) {
    RoutingRuleData rule = new RoutingRuleData();
    rule.setId(id);
    rule.name("test-rule");
    rule.description("test description");
    rule.mode(RoutingMode.BLOCK);
    rule.matchers(List.of("/.*"));
    return rule;
  }
}
