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
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;

/**
 * Event handler that updates search indexes based on component/asset events.
 *
 * @since 3.next
 */
@Named
@Singleton
public class SearchEventHandler
    extends ComponentSupport
    implements EventAware, EventAware.Asynchronous
{
  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentCreatedEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentUpdatedEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentDeletedEvent event) {
    apply(event, (search, component) -> search.purge(internalComponentId(component)));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final ComponentPurgedEvent event) {
    apply(event, search -> search.purge(event.getComponentIds()));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetCreatedEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetUpdatedEvent event) {
    apply(event, SearchFacet::index);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetDeletedEvent event) {
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

  private void apply(final ComponentPurgedEvent event, final Consumer<SearchFacet> request) {
    event.getRepository().optionalFacet(SearchFacet.class).ifPresent(
        searchFacet -> request.accept(searchFacet));
  }
}
