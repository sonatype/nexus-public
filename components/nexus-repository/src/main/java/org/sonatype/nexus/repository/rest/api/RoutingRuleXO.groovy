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

import javax.validation.constraints.Pattern

import org.sonatype.nexus.repository.routing.RoutingMode
import org.sonatype.nexus.repository.routing.RoutingRule
import org.sonatype.nexus.validation.constraint.NamePatternConstants

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder
import io.swagger.annotations.ApiModelProperty

/**
 * Routing Rule transfer object for public REST API.
 *
 * @since 3.next
 */
@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ['name'])
class RoutingRuleXO
{
  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  String name

  @ApiModelProperty(allowEmptyValue = true)
  String description

  @ApiModelProperty(
      value = "Determines what should be done with requests when their path matches any of the matchers",
      allowableValues = "BLOCK,ALLOW"
  )
  RoutingMode mode

  @ApiModelProperty(
      value = "Regular expressions used to identify request paths that are allowed or blocked (depending on mode)"
  )
  List<String> matchers

  static RoutingRuleXO fromRoutingRule(final RoutingRule routingRule) {
    return builder()
      .name(routingRule.name())
      .description(routingRule.description())
      .mode(routingRule.mode())
      .matchers(routingRule.matchers())
      .build()
  }
}
