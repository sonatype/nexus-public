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
package org.sonatype.nexus.repository.config;

/**
 * Repository configuration constants.
 *
 * @since 3.22
 */
public interface ConfigurationConstants
{
  String STORAGE = "storage";

  String DATA_STORE_NAME = "dataStoreName";

  String BLOB_STORE_NAME = "blobStoreName";

  String WRITE_POLICY = "writePolicy";

  /**
   * Docker specific constant indicating whether the 'latest' tag should be allowed to be updated with the ALLOW_ONCE
   * write policy.
   */
  String LATEST_POLICY = "latestPolicy";

  String WRITE_POLICY_DEFAULT = "ALLOW";

  String STRICT_CONTENT_TYPE_VALIDATION = "strictContentTypeValidation";

  String GROUP_WRITE_MEMBER = "groupWriteMember";

  String COMPONENT = "component";

  String PROPRIETARY_COMPONENTS = "proprietaryComponents";
}
