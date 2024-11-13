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
package org.sonatype.nexus.cleanup.config;

/**
 * Cleanup policy constants.
 *
 * @since 3.24
 */
public interface CleanupPolicyConstants
{
  String CLEANUP_ATTRIBUTES_KEY = "cleanup";

  String CLEANUP_NAME_KEY = "policyName";

  String IS_PRERELEASE_KEY = "isPrerelease";

  String LAST_BLOB_UPDATED_KEY = "lastBlobUpdated";

  String LAST_DOWNLOADED_KEY = "lastDownloaded";

  String RETAIN_KEY = "retain";

  String RETAIN_SORT_BY_KEY = "sortBy";

  String REGEX_KEY = "regex";

  String MAVEN2_FORMAT = "maven2";

  String DOCKER_FORMAT = "docker";
}
