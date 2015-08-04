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

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

/**
 * Thrown by RemoteRepositoryStorage if the requested item is not found for some reason that is part of internal
 * implementation of that same RemoteRepositoryStorage.
 *
 * @author cstamas
 * @since 2.1
 */
public class RemoteItemNotFoundException
    extends ItemNotFoundException
{
  private static final long serialVersionUID = 8422409141417737154L;

  /**
   * Constructor for group thrown "not found" exception providing information about whole tree being processed and
   * reasons why the grand total result is "not found.
   */
  public RemoteItemNotFoundException(final ResourceStoreRequest request, final ProxyRepository repository,
                                     final String reason, final String remoteUrl)
  {
    super(reasonFor(request, repository,
        "Path %s not found in remote storage of repository %s (reason=%s, remoteUrl=%s).",
        request.getRequestPath(), RepositoryStringUtils.getHumanizedNameString(repository), reason, remoteUrl));
  }

  @Override
  public ItemNotFoundInRepositoryReason getReason() {
    return (ItemNotFoundInRepositoryReason) super.getReason();
  }

}
