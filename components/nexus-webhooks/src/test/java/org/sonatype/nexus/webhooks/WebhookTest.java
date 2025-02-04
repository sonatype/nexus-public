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
package org.sonatype.nexus.webhooks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class WebhookTest
    extends TestSupport
{
  @Mock
  private EventManager eventManager;

  private static class TestWebhookType
      extends WebhookType
  {
    TestWebhookType() {
      super("test");
    }
  }

  private static class TestWebhook
      extends Webhook
  {
    @Override
    public WebhookType getType() {
      return new TestWebhookType();
    }

    @Override
    public String getName() {
      return "test";
    }

    @Override
    public Set<WebhookSubscription> getSubscriptions() {
      return super.getSubscriptions();
    }
  }

  private TestWebhook underTest;

  @Before
  public void setUp() {
    underTest = new TestWebhook();
    underTest.setEventManager(eventManager);
  }

  @Test
  public void prependsRmToEventId() {
    assertThat(underTest.getId().startsWith("rm:"), is(true));
  }

  @Test
  public void listenForEventsOnFirstSubscription() {
    assertThat(underTest.getSubscriptions(), empty());

    underTest.subscribe(mock(WebhookConfiguration.class));
    assertThat(underTest.getSubscriptions().size(), is(1));

    underTest.subscribe(mock(WebhookConfiguration.class));
    assertThat(underTest.getSubscriptions().size(), is(2));

    verify(eventManager).register(underTest);
  }

  @Test
  public void stopListeningForEventsWhenSubscriptionsEmpty() {
    WebhookSubscription subscription1 = underTest.subscribe(mock(WebhookConfiguration.class));
    WebhookSubscription subscription2 = underTest.subscribe(mock(WebhookConfiguration.class));

    subscription1.cancel();
    assertThat(underTest.getSubscriptions().size(), is(1));

    subscription2.cancel();
    assertThat(underTest.getSubscriptions(), empty());

    verify(eventManager).unregister(underTest);
  }
}
