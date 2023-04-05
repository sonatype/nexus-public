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
package org.sonatype.nexus.repository.search.index;

/**
 * Search constants.
 *
 * @since 3.25
 */
public interface SearchConstants
{
  String TYPE = "component";

  String FORMAT = "format";

  String REPOSITORY_NAME = "repository_name";

  String GROUP = "group";

  String ID = "id";

  String NAME = "name";

  String VERSION = "version";

  String NORMALIZED_VERSION = "normalized_version";

  String IS_PRERELEASE_KEY = "isPrerelease";

  String ASSETS = "assets";

  String ATTRIBUTES = "attributes";

  String CHECKSUM = "checksum";

  String CONTENT_TYPE = "content_type";

  String LAST_BLOB_UPDATED_KEY = "lastBlobUpdated";

  String LAST_DOWNLOADED_KEY = "lastDownloaded";

  String FILE_SIZE = "fileSize";

  String UPLOADER = "uploader";

  String UPLOADER_IP = "uploaderIp";
}
