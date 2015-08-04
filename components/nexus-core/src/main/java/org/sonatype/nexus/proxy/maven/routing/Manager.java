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
package org.sonatype.nexus.proxy.maven.routing;

import java.io.IOException;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;

/**
 * Autorouting Manager component.
 *
 * @author cstamas
 * @since 2.4
 */
public interface Manager
{
  /**
   * Key that is put into {@link ResourceStoreRequest}'s context for prefix file related operations, to mark that the
   * file operation is initiated by autorouting feature. Only the presence (or no presence) of this key is used for
   * flagging, the value mapped under this key is irrelevant.
   */
  String ROUTING_INITIATED_FILE_OPERATION_FLAG_KEY = Manager.class.getName() + ".fileOperation";

  /**
   * Key that is put into {@link ResourceStoreRequest}'s context when {@link ProxyRequestFilter} rejects a request.
   * Only the presence (or no presence) of this key is used for flagging, the value mapped under this key is
   * irrelevant.
   */
  String ROUTING_REQUEST_REJECTED_FLAG_KEY = Manager.class.getName() + ".requestRejected";

  /**
   * Key that when put into {@link ResourceStoreRequest}'s context, the given request becomes a
   * "not a filtering subject". Autorouting's {@link ProxyRequestFilter} will not interfere with that request, it
   * will
   * be not subject for filtering. It should be used sparingly, only in cases when you know that autorouting might
   * interfere with your request, usually because of stale prefix list. Only the presence (or no presence) of this
   * key
   * is used for flagging, the value mapped under this key is irrelevant.
   */
  String ROUTING_REQUEST_NFS_FLAG_KEY = Manager.class.getName() + ".requestNfs";

  /**
   * Startup. This method should not be invoked by any code (maybe except in UTs).
   */
  void startup();

  /**
   * Shutdown. This method should not be invoked by any code (maybe except in UTs).
   */
  void shutdown();

  /**
   * Initializes prefix list of given repository (used on repository addition and on boot when called with all
   * defined
   * repository during boot up).
   */
  void initializePrefixFile(MavenRepository mavenRepository);

  /**
   * Executes an update of prefix list for given repository. In case of {@link MavenProxyRepository} instance, it
   * might not do anything, depending is configuration returned by
   * {@link #getRemoteDiscoveryConfig(MavenProxyRepository)} for it enabled or not. This method invocation will spawn
   * the update in background, and return immediately.
   *
   * @return {@code true} if the update job was actually spawned, or {@code false} if not since one is already
   *         running
   *         for same repository. Still, will the spawned background job actually update or not depends on
   *         aforementioned configuration.
   * @throws IllegalStateException when the passed in repository is unsupported, or for some reason not in state to
   *                               be
   *                               updated (out of service, or in case of proxy, it's proxyMode does not allow remote
   *                               access and such).
   */
  boolean updatePrefixFile(MavenRepository mavenRepository)
      throws IllegalStateException;

  /**
   * Executes an update of prefix list for given repository. In case of {@link MavenProxyRepository} instance, it
   * might not do anything, depending is configuration returned by
   * {@link #getRemoteDiscoveryConfig(MavenProxyRepository)} for it enabled or not. This method invocation will
   * always
   * spawn the update in background, and return immediately. Also, this method will cancel any currently running
   * updates on same repository.
   *
   * @return {@code true} if another already running update was cancelled to execute this forced update.
   * @throws IllegalStateException when the passed in repository is unsupported, or for some reason not in state to
   *                               be
   *                               updated (out of service, or in case of proxy, it's proxyMode does not allow remote
   *                               access and such).
   */
  boolean forceUpdatePrefixFile(MavenRepository mavenRepository)
      throws IllegalStateException;

  /**
   * Special version of update of prefix list for given Maven2 proxy repository. This method will execute
   * <b>synchronously</b> and doing "quick" update only (will never scrape, only will try prefix file fetch from
   * remote). Usable in special cases when you know remote should have prefix file published, and you are interested
   * in results immediately (or at least ASAP). Still, consider that this method does remote access (using
   * {@link RemoteRepositoryStorage} of the given repository), hence, might have longer runtime (network latency,
   * remote server load and such).
   *
   * @throws IllegalStateException when the passed in repository is unsupported, or for some reason not in state to
   *                               be
   *                               updated (out of service, or in case of proxy, it's proxyMode does not allow remote
   *                               access and such).
   */
  void forceProxyQuickUpdatePrefixFile(MavenProxyRepository mavenProxyRepository)
      throws IllegalStateException;

  /**
   * Queries is the given {@link MavenRepository} supported by autorouting feature (as not all Maven2 nor all
   * {@link MavenRepository} implementations are supported! We exclude Maven1 layout and Maven2 shadow repositories).
   *
   * @return {@code true} if autorouting feature is supported for given repository instance.
   */
  boolean isMavenRepositorySupported(final MavenRepository mavenRepository);

  /**
   * Returns the autorouting status for given repository.
   *
   * @return the status, never {@code null}.
   */
  RoutingStatus getStatusFor(MavenRepository mavenRepository);

  /**
   * Returns the current (in effect) configuration of the remote discovery for given {@link MavenProxyRepository}
   * repository instance.
   *
   * @return the configuration, never {@code null}.
   */
  DiscoveryConfig getRemoteDiscoveryConfig(MavenProxyRepository mavenProxyRepository);

  /**
   * Sets the current (in effect) configuration of the remote discovery for given {@link MavenProxyRepository}
   * repository instance.
   */
  void setRemoteDiscoveryConfig(MavenProxyRepository mavenProxyRepository, DiscoveryConfig config)
      throws IOException;

  /**
   * Maintains the prefix list of a hosted repository. Offers entries to prefix list, and method updates the prefix
   * list of given hosted repository if needed. If prefix list modified, returns {@code true}.
   *
   * @param mavenHostedRepository the hosted repository to which prefix list we offer entries.
   * @param item                  the entry offered.
   * @return {@code true} if prefix list was changed, {@code false} otherwise.
   * @throws IOException in case of some IO problem.
   */
  boolean offerEntry(final MavenHostedRepository mavenHostedRepository, StorageItem item)
      throws IOException;

  /**
   * Maintains the prefix list of a hosted repository. Revokes entries from prefix list, and method updates the
   * prefix
   * list of given hosted repository if needed. If prefix list modified, returns {@code true}.
   *
   * @param mavenHostedRepository the hosted repository from which prefix list we revoke entries.
   * @param item                  the entry revoked.
   * @return {@code true} if prefix list was changed, {@code false} otherwise.
   * @throws IOException in case of some IO problem.
   */
  boolean revokeEntry(final MavenHostedRepository mavenHostedRepository, StorageItem item)
      throws IOException;

  /**
   * Returns {@link PrefixSource} for given {@link MavenRepository}.For the existence of the prefix list in question
   * (if you want to read it), check {@link PrefixSource#exists()} and {@link PrefixSource#supported()} method! Never
   * returns {@code null}.
   *
   * @return the {@link PrefixSource} for given repository.
   */
  PrefixSource getPrefixSourceFor(MavenRepository mavenRepository);

  /**
   * Publishes the passed in {@link PrefixSource} into the given {@link MavenRepository}.
   */
  void publish(MavenRepository mavenRepository, PrefixSource prefixSource)
      throws IOException;

  /**
   * Unpublishes (removes if published before) the entries, and marks {@link MavenRepository} as "noscrape".
   */
  void unpublish(MavenRepository mavenRepository)
      throws IOException;

  // ==

  /**
   * Checks whether the passed in item event is about prefix list file. In other words, is event originating from a
   * {@link MavenRepository} and has specific path.
   *
   * @return {@code true} if item event is about prefix list file.
   */
  boolean isEventAboutPrefixFile(final RepositoryItemEvent evt);
}
