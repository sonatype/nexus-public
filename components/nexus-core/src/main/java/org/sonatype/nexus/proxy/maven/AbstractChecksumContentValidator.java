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
package org.sonatype.nexus.proxy.maven;

import java.util.List;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEvent;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.repository.ItemContentValidator;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

public abstract class AbstractChecksumContentValidator
    extends ComponentSupport
    implements ItemContentValidator
{

  public AbstractChecksumContentValidator() {
    super();
  }

  @Override
  public boolean isRemoteItemContentValid(final ProxyRepository proxy, final ResourceStoreRequest req,
                                          final String baseUrl,
                                          final AbstractStorageItem item,
                                          final List<RepositoryItemValidationEvent> events)
      throws LocalStorageException
  {
    ChecksumPolicy checksumPolicy = getChecksumPolicy(proxy, item);

    final ChecksumPolicy requestChecksumPolicy =
        (ChecksumPolicy) req.getRequestContext().get(ChecksumPolicy.REQUEST_CHECKSUM_POLICY_KEY);
    if (requestChecksumPolicy != null) {
      // found, it overrides the repository-set checksum policy then
      checksumPolicy = requestChecksumPolicy;
    }

    if (checksumPolicy == null || !checksumPolicy.shouldCheckChecksum()) {
      return true;
    }

    RemoteHashResponse remoteHash = retrieveRemoteHash(item, proxy, baseUrl);

    // let compiler make sure I did not forget to populate validation results
    String msg;
    boolean contentValid;

    if (remoteHash == null && ChecksumPolicy.STRICT.equals(checksumPolicy)) {
      msg =
          "The artifact " + item.getPath() + " has no remote checksum in repository " + item.getRepositoryId()
              + "! The checksumPolicy of repository forbids downloading of it.";

      contentValid = false;
    }
    else if (remoteHash == null) {
      msg =
          "Warning, the artifact " + item.getPath() + " has no remote checksum in repository "
              + item.getRepositoryId() + "!";

      contentValid = true; // policy is STRICT_IF_EXIST or WARN
    }
    else if (remoteHash.getRemoteHash().equals(retrieveLocalHash(item, remoteHash.getInspector()))) {
      // remote hash exists and matches item content
      return true;
    }
    else if (ChecksumPolicy.WARN.equals(checksumPolicy)) {
      msg =
          "Warning, the artifact " + item.getPath() + " and it's remote checksums does not match in repository "
              + item.getRepositoryId() + "!";

      contentValid = true;
    }
    else
    // STRICT or STRICT_IF_EXISTS
    {
      msg =
          "The artifact " + item.getPath() + " and it's remote checksums does not match in repository "
              + item.getRepositoryId() + "! The checksumPolicy of repository forbids downloading of it.";

      contentValid = false;
    }

    if (!contentValid) {
      log.debug("Validation failed due: " + msg);
    }

    events.add(newChechsumFailureEvent(proxy, item, msg));

    cleanup(proxy, remoteHash, contentValid);

    return contentValid;
  }

  protected String retrieveLocalHash(AbstractStorageItem item, String inspector) {
    return item.getRepositoryItemAttributes().get(inspector);
  }

  protected abstract void cleanup(ProxyRepository proxy, RemoteHashResponse remoteHash, boolean contentValid)
      throws LocalStorageException;

  protected abstract RemoteHashResponse retrieveRemoteHash(AbstractStorageItem item, ProxyRepository proxy,
                                                           String baseUrl)
      throws LocalStorageException;

  protected abstract ChecksumPolicy getChecksumPolicy(ProxyRepository proxy, AbstractStorageItem item)
      throws LocalStorageException;

  private RepositoryItemValidationEvent newChechsumFailureEvent(final ProxyRepository proxy,
                                                                final AbstractStorageItem item, final String msg)
  {
    return new MavenChecksumContentValidationEventFailed(proxy, item, msg);
  }

}