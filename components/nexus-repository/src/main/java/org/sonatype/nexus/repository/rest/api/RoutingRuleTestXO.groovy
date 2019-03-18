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
package org.sonatype.nexus.repository.rest.api

import org.sonatype.nexus.repository.routing.RoutingMode

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.builder.Builder
import org.hibernate.validator.constraints.NotBlank

/**
 * Routing Rule Test transfer object for REST APIs.
 *
 * @since 3.next
 */
@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
class RoutingRuleTestXO
{
  @NotBlank
  RoutingMode mode

  @NotBlank
  List<String> matchers

  @NotBlank
  String path
}
