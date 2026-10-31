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
package org.sonatype.nexus.repository.firewall.event;

import java.util.Set;

import org.sonatype.nexus.repository.Repository;

/**
 * Announces that Firewall's release feed reported these paths as released for the given
 * repository. Paths are exactly as IQ reports them, e.g. {@code jquery/-/jquery-3.7.0.tgz} or
 * {@code packages/six/1.16.0/six-1.16.0-py2.py3-none-any.whl}, with or without a leading slash.
 * <p>
 * Posted as soon as the release feed returns and only when there are paths, before the per-asset
 * loop that updates download-quarantine status - a path need not be stored locally for its
 * package's PCCS metadata to need a refresh.
 */
public class FirewallComponentsReleasedEvent
{
  private final Repository repository;

  private final Set<String> pathnames;

  public FirewallComponentsReleasedEvent(final Repository repository, final Set<String> pathnames) {
    this.repository = repository;
    this.pathnames = Set.copyOf(pathnames);
  }

  public Repository getRepository() {
    return repository;
  }

  public Set<String> getPathnames() {
    return pathnames;
  }
}
