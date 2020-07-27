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
package org.sonatype.nexus.repository.browse.node;

/**
 * @since 3.6
 */
public class BrowseListItem
{
  private final String resourceUri;

  private final String name;

  private final boolean collection;

  private final String lastModified;

  private final String size;

  private final String description;

  public BrowseListItem(final String resourceUri,
                        final String name,
                        final boolean collection,
                        final String lastModified,
                        final String size,
                        final String description)
  {
    this.resourceUri = resourceUri;
    this.name = name;
    this.collection = collection;
    this.lastModified = lastModified;
    this.size = size;
    this.description = description;
  }

  public String getResourceUri() {
    return resourceUri;
  }

  public String getName() {
    return name;
  }

  public boolean isCollection() {
    return collection;
  }

  public String getLastModified() {
    return lastModified;
  }

  public String getSize() {
    return size;
  }

  public String getDescription() {
    return description;
  }
}
