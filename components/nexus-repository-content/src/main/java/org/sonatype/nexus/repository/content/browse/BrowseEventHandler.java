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
package org.sonatype.nexus.repository.content.browse;

import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetCreateEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event handler that updates search indexes based on component/asset events.
 *
 * @since 3.next
 */
@Named
@Singleton
public class BrowseEventHandler
    extends ComponentSupport
    implements EventAware, EventAware.Asynchronous
{
  private final ContentFacetFinder contentFacetFinder;

  @Inject
  public BrowseEventHandler(final ContentFacetFinder contentFacetFinder) {
    this.contentFacetFinder = checkNotNull(contentFacetFinder);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetCreateEvent event) {
    apply(event, BrowseFacet::addPathToAsset);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AssetUploadEvent event) {
    apply(event, BrowseFacet::addPathToAsset);
  }

  private void apply(final AssetEvent event, final BiConsumer<BrowseFacet, Asset> request) {
    contentFacetFinder.findRepository(event.getAsset()).ifPresent(
        repository -> repository.optionalFacet(BrowseFacet.class).ifPresent(
            browseFacet -> request.accept(browseFacet, event.getAsset())));
  }
}
