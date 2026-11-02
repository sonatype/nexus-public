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
package org.sonatype.nexus.repository.httpclient;

import java.util.List;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;

public class HttpClientConfig
{
  @Valid
  @Nullable
  public ConnectionConfiguration connection;

  @Valid
  @Nullable
  public AuthenticationConfiguration authentication;

  @Nullable
  public Boolean blocked;

  @Nullable
  public Boolean autoBlock;

  /**
   * The exact set of HTTP status codes that trigger auto-block for this repository. When non-empty, only these
   * codes trigger auto-blocking and this list fully replaces the format-specific default -- any code not listed
   * will not auto-block. When null or empty, the format-specific default {@link AutoBlockConfiguration} applies
   * instead. Each element must be a valid HTTP status code in the range 100-599.
   */
  @Nullable
  @Size(max = 500, message = "autoBlockStatusCodes may contain at most 500 entries")
  public List<@NotNull @Min(100) @Max(599) Integer> autoBlockStatusCodes;
}
