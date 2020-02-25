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
package org.sonatype.nexus.repository.view;

/**
 * Common content-type values.
 *
 * @since 3.0
 */
public class ContentTypes
{

  private ContentTypes() {
    // empty
  }

  public static final String TEXT_PLAIN = "text/plain";

  public static final String TEXT_HTML = "text/html";

  public static final String APPLICATION_JSON = "application/json";

  public static final String APPLICATION_XML = "application/xml";

  public static final String APPLICATION_GZIP = "application/gzip";

  public static final String APPLICATION_ZIP = "application/zip";

  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

  public static final String APPLICATION_TAR = "application/x-tar";
}
