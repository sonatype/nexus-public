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
package org.sonatype.nexus.proxy.storage.remote;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.AbstractContextualizedRepositoryStorage;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class is a base abstract class for remote storage.
 *
 * @author cstamas
 */
public abstract class AbstractRemoteRepositoryStorage
    extends AbstractContextualizedRepositoryStorage<RemoteStorageContext>
    implements RemoteRepositoryStorage
{
  private final MimeSupport mimeSupport;

  private final ApplicationStatusSource applicationStatusSource;

  protected AbstractRemoteRepositoryStorage(final ApplicationStatusSource applicationStatusSource,
                                            final MimeSupport mimeSupport)
  {
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.mimeSupport = checkNotNull(mimeSupport);
  }

  protected MimeSupport getMimeSupport() {
    return mimeSupport;
  }

  @Override
  public URL getAbsoluteUrlFromBase(final ProxyRepository repository, final ResourceStoreRequest request)
      throws RemoteStorageException
  {
    return getAbsoluteUrlFromBase(repository.getRemoteUrl(), request.getRequestPath());
  }

  protected URL getAbsoluteUrlFromBase(final String baseUrl, final String path)
      throws RemoteStorageException
  {
    final StringBuilder urlStr = new StringBuilder(baseUrl);

    if (!baseUrl.endsWith(RepositoryItemUid.PATH_SEPARATOR)) {
      urlStr.append(RepositoryItemUid.PATH_SEPARATOR);
    }

    if (!path.startsWith(RepositoryItemUid.PATH_SEPARATOR)) {
      urlStr.append(path);
    }
    else {
      urlStr.append(path.substring(RepositoryItemUid.PATH_SEPARATOR.length()));
    }

    try {
      return new URL(urlStr.toString());
    }
    catch (MalformedURLException e) {
      throw new RemoteStorageException("The repository has broken URL!", e);
    }

  }

  protected RemoteStorageContext getRemoteStorageContext(ProxyRepository repository)
      throws RemoteStorageException
  {
    try {
      return super.getStorageContext(repository, repository.getRemoteStorageContext());
    }
    catch (RemoteStorageException e) {
      throw e;
    }
    catch (IOException e) {
      throw new RemoteStorageException("Could not update context of " + repository, e);
    }
  }

  @Override
  protected void updateStorageContext(final Repository repository, final RemoteStorageContext context,
                                      final ContextOperation contextOperation)
      throws IOException
  {
    if (ContextOperation.INITIALIZE == contextOperation) {
      log.info("Initializing remote transport for proxy repository {}...",
          RepositoryStringUtils.getHumanizedNameString(repository));

    }
    else if (ContextOperation.UPDATE == contextOperation) {
      log.info("Updating remote transport for proxy repository {}...",
          RepositoryStringUtils.getHumanizedNameString(repository));
    }
    updateContext((ProxyRepository) repository, context);
  }

  @Override
  public boolean containsItem(ProxyRepository repository, ResourceStoreRequest request)
      throws RemoteStorageException
  {
    return containsItem(0, repository, request);
  }

  public String getVersion() {
    final SystemStatus status = applicationStatusSource.getSystemStatus();

    return status.getVersion();
  }

  // helper methods

  /**
   * Remote storage specific, when the remote connection settings are actually applied.
   *
   * @param repository to update context for
   * @param context    remote repository context
   * @throws RemoteStorageException If context could not be updated
   */
  protected abstract void updateContext(ProxyRepository repository, RemoteStorageContext context)
      throws RemoteStorageException;

}
