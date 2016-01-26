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
package org.sonatype.nexus.plugins.ruby.proxy;

import java.io.IOException;

import org.sonatype.nexus.plugins.ruby.NexusStorage;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.ruby.BundlerApiFile;
import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.layout.ProxyStorage;

import com.google.common.base.Throwables;

/**
 * Rubygems proxy storage.
 *
 * @since 2.11
 */
public class ProxyNexusStorage
    extends NexusStorage
    implements ProxyStorage
{
  private final ProxyRubyRepository repository;

  public ProxyNexusStorage(ProxyRubyRepository repository) {
    super(repository);
    this.repository = repository;
  }

  @Override
  public void retrieve(BundlerApiFile file) {
    try {
      log.debug("retrieve :: {}", file);
      file.set(repository.retrieveDirectItem(new ResourceStoreRequest(file.remotePath(), false, false)));
    }
    catch (IOException | IllegalOperationException | ItemNotFoundException e) {
      file.setException(e);
    }
  }

  @Override
  public boolean isExpired(DependencyFile file) {
    boolean expired = true;
    try {
      ResourceStoreRequest request = new ResourceStoreRequest(file.storagePath(), true, false);
      if (repository.getLocalStorage().containsItem(repository, request)) {
        StorageItem item = repository.getLocalStorage().retrieveItem(repository, request);
        expired = repository.isOld(item);
      }
    }
    catch (ItemNotFoundException e) {
      // ignore
    }
    catch (IOException e) {
      // fail here
      throw Throwables.propagate(e);
    }
    log.debug("isExpired={} :: {}", expired, file);
    return expired;
  }
}