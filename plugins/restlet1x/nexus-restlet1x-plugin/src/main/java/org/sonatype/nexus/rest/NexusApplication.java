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
package org.sonatype.nexus.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.plexus.rest.PlexusRestletApplicationBridge;
import org.sonatype.plexus.rest.RetargetableRestlet;
import org.sonatype.plexus.rest.resource.ManagedPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.web.ProtectedPathManager;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import com.thoughtworks.xstream.XStream;
import org.apache.shiro.util.AntPathMatcher;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.service.StatusService;
import org.restlet.util.Template;
import org.slf4j.LoggerFactory;

/**
 * Legacy restlet application support.
 */
@Named("nexus")
@Singleton
public class NexusApplication
    extends PlexusRestletApplicationBridge
{
  private final EventBus eventBus;

  private final ProtectedPathManager protectedPathManager;

  private final ManagedPlexusResource statusPlexusResource;

  private final StatusService statusService;

  private final boolean useStrictChecking;

  @Inject
  public NexusApplication(final EventBus eventBus,
                          final ProtectedPathManager protectedPathManager,
                          final @Named("StatusPlexusResource") ManagedPlexusResource statusPlexusResource,
                          final StatusService statusService)
  {
    this.eventBus = eventBus;
    this.protectedPathManager = protectedPathManager;
    this.statusPlexusResource = statusPlexusResource;
    this.statusService = statusService;
    useStrictChecking = System.getProperty("nexus.restlet.strict-uri-matching", "true").equals("true");
    LoggerFactory.getLogger(getClass().getName() + "_UriMatching").info("Strict URI matching: {}", useStrictChecking);
  }

  // HACK: Too many places were using new NexusApplication() ... fuck it
  @VisibleForTesting
  public NexusApplication() {
    this(
        null,
        null,
        null,
        null
    );
  }

  @Subscribe
  public void onEvent(final NexusStartedEvent evt) {
    recreateRoot(true);
    afterCreateRoot((RetargetableRestlet) getRoot());
  }

  @Subscribe
  public void onEvent(final NexusStoppedEvent evt) {
    recreateRoot(false);
  }

  /**
   * Adding this as config change listener.
   */
  @Override
  protected void doConfigure() {
    // NEXUS-2883: turning off Range support for now
    getRangeService().setEnabled(false);

    // adding ourselves as listener
    eventBus.register(this);
  }

  @Override
  public XStream doConfigureXstream(XStream xstream) {
    return org.sonatype.nexus.rest.model.XStreamConfigurator.configureXStream(xstream);
  }

  @Override
  protected Router initializeRouter(Router root, boolean isStarted) {
    if (useStrictChecking) {
      root.setDefaultMatchQuery(false);
      root.setDefaultMatchingMode(Template.MODE_EQUALS);
    }
    return root;
  }

  @Override
  protected void doCreateRoot(Router root, boolean isStarted) {
    if (!isStarted) {
      return;
    }

    setStatusService(statusService);

    attach(getApplicationRouter(), statusPlexusResource);

    // protecting service resources with "wall" permission
    this.protectedPathManager.addProtectedResource("/service/local/**",
        // HACK: Disable CSRFGuard support for now, its too problematic
        //"noSessionCreation,authcBasic,csrfToken,perms[nexus:permToCatchAllUnprotecteds]"
        "noSessionCreation,authcBasic,perms[nexus:permToCatchAllUnprotecteds]"
    );
  }

  @Override
  protected void attach(Router router, boolean strict, String uriPattern, Restlet target) {
    super.attach(router, useStrictChecking && strict, uriPattern, target);
  }

  private final AntPathMatcher shiroAntPathMatcher = new AntPathMatcher();

  @Override
  protected void handlePlexusResourceSecurity(PlexusResource resource) {
    PathProtectionDescriptor descriptor = resource.getResourceProtection();
    if (descriptor == null) {
      return;
    }

    // sanity check: path protection descriptor path and resource URI must align
    if (!shiroAntPathMatcher.match(descriptor.getPathPattern(), resource.getResourceUri())) {
      throw new IllegalStateException(String.format(
          "Plexus resource %s would attach to URI=%s but protect path=%s that does not matches URI!",
          resource.getClass().getName(), resource.getResourceUri(),
          descriptor.getPathPattern()));
    }

    String filterExpression = descriptor.getFilterExpression();
    if (filterExpression != null && !filterExpression.contains("authcNxBasic")) {
      // don't create session unless the user logs in from the UI
      filterExpression = "noSessionCreation," + filterExpression;
    }

    // HACK: Disable CSRFGuard support for now, its too problematic
    //if (filterExpression != null
    //    && (filterExpression.contains("authcBasic") || filterExpression.contains("authcNxBasic"))) {
    //  filterExpression += ",csrfToken";
    //}

    this.protectedPathManager.addProtectedResource("/service/local" + descriptor.getPathPattern(), filterExpression);
  }

  @Override
  protected void attach(Router router, PlexusResource resource) {
    handlePlexusResourceSecurity(resource);
    attach(router, resource.requireStrictChecking(), resource.getResourceUri(),
        new NexusPlexusResourceFinder(getContext(), resource));
  }
}
