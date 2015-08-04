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
package org.sonatype.nexus.ruby;

/**
 * enum of possible file types with a rubygems repo including
 * the gem-artifacts and some virtual files like "not_found", etc
 *
 * they all carry the mime-type, the encoding and a varyAccept boolean.
 *
 * @author christian
 */
public enum FileType
{
  GEM("binary/octet-stream", true),
  GEMSPEC("binary/octet-stream", true),
  DEPENDENCY("application/octet-stream", true),
  MAVEN_METADATA("application/xml", "utf-8", true),
  MAVEN_METADATA_SNAPSHOT("application/xml", "utf-8", true),
  POM("application/xml", "utf-8", true),
  SPECS_INDEX("application/octet-stream", true),
  SPECS_INDEX_ZIPPED("application/gzip", true),
  DIRECTORY("text/html", "utf-8"),
  BUNDLER_API("application/octet-stream", true),
  API_V1("text/plain", "ASCII"), // for the api_key
  GEM_ARTIFACT("binary/octet-stream", true),
  SHA1("text/plain", "ASCII"),
  NOT_FOUND(null),
  FORBIDDEN(null),
  TEMP_UNAVAILABLE(null);

  private final String encoding;

  private final String mime;

  private final boolean varyAccept;

  private FileType(String mime) {
    this(mime, null, false);
  }

  private FileType(String mime, boolean varyAccept) {
    this(mime, null, varyAccept);
  }

  private FileType(String mime, String encoding) {
    this(mime, encoding, false);
  }

  private FileType(String mime, String encoding, boolean varyAccept) {
    this.mime = mime;
    this.encoding = encoding;
    this.varyAccept = varyAccept;
  }

  public boolean isVaryAccept() {
    return varyAccept;
  }

  public String encoding() {
    return encoding;
  }

  public String mime() {
    return this.mime;
  }
}