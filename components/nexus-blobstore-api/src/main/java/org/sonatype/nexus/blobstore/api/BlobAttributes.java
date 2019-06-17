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
package org.sonatype.nexus.blobstore.api;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.joda.time.DateTime;

/**
 * @since 3.4
 */
public interface BlobAttributes
{
  Map<String, String> getHeaders();

  BlobMetrics getMetrics();

  boolean isDeleted();

  void setDeleted(boolean deleted);

  void setDeletedReason(String deletedReason);

  String getDeletedReason();

  /**
   * @since 3.17
   */
  DateTime getDeletedDateTime();

  /**
   * @since 3.17
   */
  void setDeletedDateTime(DateTime deletedDateTime);

  Properties getProperties();

  /**
   * Update attributes based on the {@link BlobAttributes} provided.
   *
   * @since 3.7
   */
  void updateFrom(BlobAttributes blobAttributes);

  /**
   * Stores the attributes in the blob store.
   *
   * @since 3.12
   */
  void store() throws IOException;

}
