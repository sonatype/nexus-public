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
package org.sonatype.nexus.proxy.repository;

import java.util.List;

import org.sonatype.nexus.plugins.RepositoryType;

/**
 * A hosted repository that serves up "web" content (static HTML files). Default behaviour: If a request results in
 * collection, it will look in that collection for any existing welcome file and serve that up instead of collection.
 * If
 * no welcome file found, it falls back to collection/index view.
 *
 * @author cstamas
 */
@RepositoryType(pathPrefix = "sites")
public interface WebSiteRepository
    extends HostedRepository
{
  /**
   * Key to be used in a repository request to signal if we should use the welcome files (index.html, index.htm, etc)
   * or if we should return the collection.
   */
  public static final String USE_WELCOME_FILES_KEY = "useWelcomeFiles";

  /**
   * Gets the list of unmodifiable "welcome" file names. Example: "index.html", "index.htm".
   */
  List<String> getWelcomeFiles();

  /**
   * Sets the list of welcome files.
   */
  void setWelcomeFiles(List<String> files);
}
