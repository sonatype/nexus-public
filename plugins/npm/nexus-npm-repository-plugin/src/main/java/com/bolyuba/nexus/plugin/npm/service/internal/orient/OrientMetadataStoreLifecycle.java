/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal.orient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.events.NexusInitializedEvent;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.Subscribe;
import com.orientechnologies.orient.core.Orient;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EventSubscriber} implementation that drives OrientMetadataStore lifecycle.
 */
@Singleton
@Named
public class OrientMetadataStoreLifecycle
    extends ComponentSupport
    implements EventSubscriber
{
  private final OrientMetadataStore orientMetadataStore;

  @Inject
  public OrientMetadataStoreLifecycle(
      final OrientMetadataStore orientMetadataStore)
  {
    this.orientMetadataStore = checkNotNull(orientMetadataStore);
    Orient.instance().removeShutdownHook();
  }


  @Subscribe
  public void on(final NexusInitializedEvent e) throws Exception {
    orientMetadataStore.start();
  }

  @Subscribe
  public void on(final NexusStoppedEvent e) throws Exception {
    log.debug("Asking npm metadata store to stop...");
    try {
      orientMetadataStore.stop();
    }
    catch (Exception ex) {
      log.warn("Problem stopping the npm metadata store", ex);
    } finally {
       log.debug("Asking Orient to shutdown...");
       Orient.instance().shutdown(); 
    }
  }
}
