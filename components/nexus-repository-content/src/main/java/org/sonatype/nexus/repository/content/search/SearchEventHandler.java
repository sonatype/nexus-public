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
package org.sonatype.nexus.repository.content.search;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.asset.AssetCreateEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeleteEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdateEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreateEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeleteEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgeEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentUpdateEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;

/**
 * Event handler that updates search indexes based on component/asset events.
 *
 * @since 3.26
 */
@Named
@Singleton
public class SearchEventHandler
    extends ComponentSupport
    implements EventAware, EventAware.Asynchronous
{
  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentCreateEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentUpdateEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentDeleteEvent event) {
    apply(event, (search, component) -> search.purge(internalComponentId(component)));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentPurgeEvent event) {
    apply(event, search -> search.purge(event.getComponentIds()));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetCreateEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetUpdateEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetDeleteEvent event) {
    apply(event, SearchFacet::index);
  }

  private void apply(final ComponentEvent event, final BiConsumer<SearchFacet, Component> request) {
    event.getRepository().optionalFacet(SearchFacet.class).ifPresent(
        searchFacet -> request.accept(searchFacet, event.getComponent()));
  }

  private void apply(final AssetEvent event, final BiConsumer<SearchFacet, Component> request) {
    Optional<Component> component = event.getAsset().component();
    if (component.isPresent()) {
      event.getRepository().optionalFacet(SearchFacet.class).ifPresent(
          searchFacet -> request.accept(searchFacet, component.get()));
    }
  }

  private void apply(final ComponentPurgeEvent event, final Consumer<SearchFacet> request) {
    event.getRepository().optionalFacet(SearchFacet.class).ifPresent(
        searchFacet -> request.accept(searchFacet));
  }
}
