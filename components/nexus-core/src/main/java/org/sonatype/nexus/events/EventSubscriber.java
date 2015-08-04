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
package org.sonatype.nexus.events;

import javax.inject.Provider;

/**
 * Marker interface for subscribers wanting to get events from Nexus Event Bus, aka. the new event inspectors.
 * Example of event subscriber:
 * <pre>
 *   @Singleton
 *   @Named
 *   public class MySubscriber
 *     implements EventSubscriber {
 *
 *     @Subscribe
 *     public void onSomeEvent(final SomeEvent evt) {
 *       ... do something
 *     }
 *   }
 * </pre>
 * Event subscriber limitations: they have to be singleton scoped components, otherwise event bus
 * registration and unregistration will fail.
 * <p/>
 * In short, you code as you would do usually with Google Guava EventBus (so using @Subscribe and
 * @AllowConcurrentEvents as usually), and to those annotated methods same constrains applies as for
 * plain event bus subscribers (method should be public, and have one parameter). The "trick" here is
 * that your component should implement the EventSubscriber interface, and in that case it will get
 * auto-registered with Nexus EventBus.
 * <p/>
 * In general, event subscriber should not post events, still, there are cases where it post events
 * implicitly (as when it performs some content manipulation, item related events will be fired). Same
 * constraints to posting events applies as for Guava EventBus.
 * <p/>
 * Also, be aware that event subscribers are looked up early in Nexus (during boot), and components
 * you want to have injected might not be available yet. To circumvent this, inject their {@link Provider}
 * instead.
 * <p/>
 * Event subscribers are automatically managed by event subscriber host {@link EventSubscriberHost}, and
 * they will not receive events fired <em>before</em> Nexus boot process starts. All the events in Core
 * might be handled by subscribers, but only after host is started, event that is executed very early
 * by Nexus Core. All the subsequent events <em>are received</em> by event subscribers.
 * Still, the system might contain some other (read non-core) component as extension and similar, that uses
 * "bare EventBus", and might post any kind of event of it. If you need to interact with such events,
 * use EventBus directly (create an eager singleton and manually handle registration and such), as
 * event subscribers are suited for Nexus events happening during it's lifecycle. This limitation
 * exists only for the <em>boot process</em> (ie. subscriber will start receiving events only after
 * Nexus has booted), while EventBus might dispatch events even before it. Event types that subscriber
 * might listen for is not limited in any way.
 *
 * @since 2.7.0
 */
public interface EventSubscriber
{
}
