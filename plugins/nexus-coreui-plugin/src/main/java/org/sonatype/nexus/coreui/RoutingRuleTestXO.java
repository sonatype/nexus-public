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
package org.sonatype.nexus.coreui;

import java.util.List;
import javax.validation.constraints.NotBlank;

import org.sonatype.nexus.repository.routing.RoutingMode;

/**
 * Routing Rule Test transfer object for internal REST API.
 *
 * @since 3.16
 */
public class RoutingRuleTestXO
{
  @NotBlank
  private RoutingMode mode;

  @NotBlank
  private List<String> matchers;

  @NotBlank
  private String path;

  public RoutingMode getMode() {
    return mode;
  }

  public void setMode(RoutingMode mode) {
    this.mode = mode;
  }

  public List<String> getMatchers() {
    return matchers;
  }

  public void setMatchers(List<String> matchers) {
    this.matchers = matchers;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

}
