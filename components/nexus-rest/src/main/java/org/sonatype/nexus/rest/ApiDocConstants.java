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
package org.sonatype.nexus.rest;

/**
 * Constants for REST API documentation.
 *
 * @since 3.20
 */
public class ApiDocConstants
{
  private ApiDocConstants() {
  }

  public static final String SUCCESS = "Success";

  public static final String AUTHENTICATION_REQUIRED = "Authentication required";

  public static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions";

  public static final String REPOSITORY_NOT_FOUND = "Repository not found";

  public static final String PROJECT_NOT_FOUND = "Project not found";

  public static final String REPOSITORY_CREATED = "Repository created";

  public static final String REPOSITORY_UPDATED = "Repository updated";

  public static final String REPOSITORY_DELETED = "Repository deleted";

  public static final String API_REPOSITORY_MANAGEMENT = "Repository Management";

  public static final String API_BLOB_STORE = "Blob store";

  public static final String S3_BLOB_STORE_CREATED = "S3 blob store created";

  public static final String S3_BLOB_STORE_UPDATED = "S3 blob store updated";

  public static final String S3_BLOB_STORE_DELETED = "S3 blob store deleted";

  public static final String UNKNOWN_S3_BLOB_STORE = "Specified S3 blob store doesn't exist";

  public static final String BLOBSTORE_NOT_FOUND = "Blob store not found";

  public static final String BLOBSTORE_CHANGE_NOT_ALLOWED =
      "You cannot change a repository’s blob store through the API";

  public static final String BAD_REQUEST = "Bad request";

  public static final String DISABLED_IN_HIGH_AVAILABILITY = "Feature is disabled in High Availability";
}
