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
package org.sonatype.nexus.blobstore.s3.internal.ui;

/**
 * S3 Region exchange object.
 *
 * @since 3.12
 */
public class S3RegionXO
{
  private int order;

  private String id;

  private String name;

  public int getOrder() {
    return order;
  }

  public void setOrder(final int order) {
    this.order = order;
  }

  public S3RegionXO withOrder(final int order) {
    this.order = order;
    return this;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public S3RegionXO withId(final String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public S3RegionXO withName(final String name) {
    this.name = name;
    return this;
  }
}
