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
package org.sonatype.nexus.plugins.ruby;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.Subscribe;
import org.jruby.embed.ScriptingContainer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A provider for RubygemsGateway that creates it lazily (ie. if NX instance does not have rubygems
 * repository, the gateway should not be created either). The termination of the gateway is alos handled here,
 *
 * @since 2.11
 */
@Singleton
@Named
public class NexusRubygemsGatewayProvider
    extends ComponentSupport
    implements Provider<RubygemsGateway>, EventSubscriber
{
  private final Provider<ScriptingContainer> scriptingContainerProvider;

  private RubygemsGateway rubygemsGateway;

  @Inject
  public NexusRubygemsGatewayProvider(final Provider<ScriptingContainer> scriptingContainerProvider) {
    this.scriptingContainerProvider = checkNotNull(scriptingContainerProvider);
  }

  @Override
  public synchronized RubygemsGateway get() {
    if (rubygemsGateway == null) {
      // this takes about 20 seconds and happens lazily, so log it visibly
      log.info("Creating JRuby RubygemsGateway");
      rubygemsGateway = new DefaultRubygemsGateway(scriptingContainerProvider.get());
    }
    return rubygemsGateway;
  }

  @Subscribe
  public void on(final NexusStoppedEvent event) {
    if (rubygemsGateway != null) {
      log.debug("Stopping JRuby RubygemsGateway");
      rubygemsGateway.terminate();
    }
  }
}