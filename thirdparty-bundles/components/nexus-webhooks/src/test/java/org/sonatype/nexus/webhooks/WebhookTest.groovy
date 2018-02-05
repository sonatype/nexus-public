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
package org.sonatype.nexus.webhooks

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventManager

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify

/**
 * Tests for {@link Webhook}.
 */
class WebhookTest
  extends TestSupport
{
  @Mock
  private EventManager eventManager

  /**
   * Test {@link WebhookType}.
   */
  private static class TestWebhookType
    extends WebhookType
  {
    TestWebhookType() {
      super('test')
    }
  }

  /**
   * Test {@link Webhook}.
   */
  private static class TestWebhook
    extends Webhook
  {
    @Override
    WebhookType getType() {
      return new TestWebhookType()
    }

    @Override
    String getName() {
      return 'test'
    }

    // promote to public
    @Override
    Set<WebhookSubscription> getSubscriptions() {
      return super.getSubscriptions()
    }
  }

  private TestWebhook underTest

  @Before
  void setUp() {
    underTest = new TestWebhook(
        eventManager: eventManager
    )
  }

  @Test
  void 'prepends rm: to event id'() {
    assert underTest.id.startsWith("rm:")
  }

  @Test
  void 'listen for events on first subscription'() {
    assert underTest.subscriptions.size() == 0

    def subscription1 = underTest.subscribe(mock(WebhookConfiguration.class))
    assert underTest.subscriptions.size() == 1

    def subscription2 = underTest.subscribe(mock(WebhookConfiguration.class))
    assert underTest.subscriptions.size() == 2

    verify(eventManager, times(1)).register(underTest)
  }

  @Test
  void 'stop listening for events when subscriptions empty'() {
    def subscription1 = underTest.subscribe(mock(WebhookConfiguration.class))
    def subscription2 = underTest.subscribe(mock(WebhookConfiguration.class))

    subscription1.cancel()
    assert underTest.subscriptions.size() == 1

    subscription2.cancel()
    assert underTest.subscriptions.size() == 0

    verify(eventManager, times(1)).unregister(underTest)
  }
}
