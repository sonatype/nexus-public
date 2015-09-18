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
package org.sonatype.nexus.repository.http;

/**
 * Helper defining common HTTP methods names.
 *
 * @since 3.0
 */
public class HttpMethods
{
  private HttpMethods() {}

  // http://ietf.org/rfc/rfc2616

  public static final String OPTIONS = "OPTIONS";

  public static final String GET = "GET";

  public static final String HEAD = "HEAD";

  public static final String POST = "POST";

  public static final String PUT = "PUT";

  public static final String DELETE = "DELETE";

  public static final String TRACE = "TRACE";

  public static final String CONNECT = "CONNECT";

  // http://ietf.org/rfc/rfc2518

  public static final String PROPFIND = "PROPFIND";

  public static final String PROPPATCH = "PROPPATCH";

  public static final String MKCOL = "MKCOL";

  public static final String COPY = "COPY";

  public static final String MOVE = "MOVE";

  public static final String LOCK = "LOCK";

  public static final String UNLOCK = "UNLOCK";

  // http://ietf.org/rfc/rfc3253

  public static final String VERSION_CONTROL = "VERSION-CONTROL";

  public static final String REPORT = "REPORT";

  public static final String CHECKOUT = "CHECKOUT";

  public static final String CHECKIN = "CHECKIN";

  public static final String UNCHECKOUT = "UNCHECKOUT";

  public static final String MKWORKSPACE = "MKWORKSPACE";

  public static final String UPDATE = "UPDATE";

  public static final String LABEL = "LABEL";

  public static final String MERGE = "MERGE";

  public static final String BASELINE_CONTROL = "BASELINE-CONTROL";

  public static final String MKACTIVITY = "MKACTIVITY";

  // http://ietf.org/rfc/rfc3648

  public static final String ORDERPATCH = "ORDERPATCH";

  // http://ietf.org/rfc/rfc3744

  public static final String ACL = "ACL";

  // https://datatracker.ietf.org/drafts/draft-dusseault-http-patch/

  public static final String PATCH = "PATCH";

  // https://datatracker.ietf.org/drafts/draft-reschke-webdav-search/

  public static final String SEARCH = "SEARCH";
}
