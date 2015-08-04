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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.Config;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteStrategy;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyFailedException;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyResult;
import org.sonatype.nexus.proxy.maven.routing.internal.TextFilePrefixSourceMarshaller.Result;
import org.sonatype.nexus.proxy.storage.remote.httpclient.HttpClientManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Remote prefix file strategy.
 * 
 * @author cstamas
 */
@Named(RemotePrefixFileStrategy.ID)
@Singleton
public class RemotePrefixFileStrategy
    extends AbstractHttpRemoteStrategy
    implements RemoteStrategy
{
  private static final PrefixSource UNSUPPORTED_PREFIXSOURCE = new PrefixSource()
  {
    @Override
    public boolean exists() {
      return false;
    }

    @Override
    public boolean supported() {
      return false;
    }

    @Override
    public List<String> readEntries() throws IOException {
      return Collections.emptyList();
    }

    @Override
    public long getLostModifiedTimestamp() {
      return 0;
    }

  };

  protected static final String ID = "prefix-file";

  private final Config config;

  /**
   * Constructor.
   */
  @Inject
  public RemotePrefixFileStrategy(final Config config, final HttpClientManager httpClientManager) {
    super(100, ID, httpClientManager);
    this.config = checkNotNull(config);
  }

  @Override
  public StrategyResult doDiscover(final MavenProxyRepository mavenProxyRepository) throws StrategyFailedException,
      IOException
  {
    StorageFileItem item;
    String path = config.getRemotePrefixFilePath();
    log.debug("Looking for remote prefix on {} at path {}", mavenProxyRepository, path);
    // we keep exclusive lock on UID during discovery to prevent other threads grabbing this file
    // prematurely. We release the lock only when file is present locally, and is validated.
    // in that moment it's not published yet, but the content is correct and it will be
    // the same that will get published.
    final RepositoryItemUid uid = mavenProxyRepository.createUid(path);
    uid.getLock().lock(Action.update);
    try {
      item = retrieveFromRemoteIfExists(mavenProxyRepository, path);
      if (item != null) {
        log.debug("Remote prefix on {} at path {} found!", mavenProxyRepository, path);
        long prefixFileAgeInDays = (System.currentTimeMillis() - item.getModified()) / 86400000L;
        Result unmarshalled = new TextFilePrefixSourceMarshaller(config).read(item);
        if (!unmarshalled.supported()) {
          return new StrategyResult("Remote disabled automatic routing", UNSUPPORTED_PREFIXSOURCE, false);
        }
        if (unmarshalled.entries().isEmpty()) {
          return new StrategyResult("Remote publishes empty prefix file", UNSUPPORTED_PREFIXSOURCE, false);
        }

        final PrefixSource prefixSource = new FilePrefixSource(mavenProxyRepository, path, config);
        if (prefixFileAgeInDays < 1) {
          return new StrategyResult("Remote publishes prefix file (is less than a day old), using it.", prefixSource,
              true);
        }
        else {
          return new StrategyResult(
              "Remote publishes prefix file (is " + prefixFileAgeInDays + " days old), using it.", prefixSource, true);
        }
      }
    }
    finally {
      uid.getLock().unlock();
    }
    throw new StrategyFailedException("Remote does not publish prefix files on path " + path);
  }

  // ==

  protected StorageFileItem retrieveFromRemoteIfExists(final MavenProxyRepository mavenProxyRepository,
      final String path) throws IOException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(path);
    request.setRequestRemoteOnly(true);
    request.getRequestContext().put(Manager.ROUTING_INITIATED_FILE_OPERATION_FLAG_KEY, Boolean.TRUE);
    // NXCM-5188: Disable checksum policy for prefix file request, it will be processed and checked anyway
    request.getRequestContext().put(ChecksumPolicy.REQUEST_CHECKSUM_POLICY_KEY, ChecksumPolicy.IGNORE);

    // check for remote presence, as fetching with setRequestRemoteOnly has a side effect of
    // DELETING the file from local cache if not present remotely. In this case, prefix
    // file (on default location) obviously originates from scrape, so we should not delete it.
    final boolean presentRemotely = mavenProxyRepository.getRemoteStorage().containsItem(mavenProxyRepository, request);
    if (!presentRemotely) {
      return null;
    }

    mavenProxyRepository.removeFromNotFoundCache(request);
    try {
      @SuppressWarnings("deprecation")
      final StorageItem item = mavenProxyRepository.retrieveItem(true, request);
      if (item instanceof StorageFileItem) {
        return (StorageFileItem) item;
      }
      else {
        return null;
      }
    }
    catch (IllegalOperationException e) {
      // eh?
      return null;
    }
    catch (ItemNotFoundException e) {
      // expected when remote does not publish it
      // not let if rot in NFC as it would block us (if interval is less than NFC keep alive)
      mavenProxyRepository.removeFromNotFoundCache(request);
      return null;
    }
  }
}
