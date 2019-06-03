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
package org.sonatype.nexus.repository.golang.internal.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object for .info endpoint responses
 *
 * @since 3.next
 */
public class GolangInfo
{
  @JsonProperty(value = "Version")
  private String version;

  @JsonProperty(value = "Time")
  private String time;

  public GolangInfo(final String version, final String time) {
    this.version = version;
    this.time = time;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getTime() {
    return time;
  }

  public void setTime(final String time) {
    this.time = time;
  }
}
