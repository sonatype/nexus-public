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
package org.sonatype.nexus.ruby.cuba.api;

import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.Cuba;
import org.sonatype.nexus.ruby.cuba.State;

/**
 * cuba for /api/v1
 *
 * @author christian
 */
public class ApiV1Cuba
    implements Cuba
{
  private static final String GEMS = "gems";

  private static final String API_KEY = "api_key";

  public static final String DEPENDENCIES = "dependencies";

  private final Cuba apiV1Dependencies;

  public ApiV1Cuba(Cuba cuba) {
    this.apiV1Dependencies = cuba;
  }

  /**
   * directory [dependencies], files [api_key,gems]
   */
  @Override
  public RubygemsFile on(State state) {
    switch (state.name) {
      case DEPENDENCIES:
        return state.nested(apiV1Dependencies);
      case GEMS:
      case API_KEY:
        return state.context.factory.apiV1File(state.name);
      case "":
        return state.context.factory.directory(state.context.original, API_KEY, DEPENDENCIES);
      default:
        return state.context.factory.notFound(state.context.original);
    }
  }
}