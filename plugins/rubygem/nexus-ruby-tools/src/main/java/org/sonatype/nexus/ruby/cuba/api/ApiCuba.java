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
import org.sonatype.nexus.ruby.cuba.RootCuba;
import org.sonatype.nexus.ruby.cuba.State;

/**
 * cuba for /api/
 *
 * @author christian
 */
public class ApiCuba
    implements Cuba
{
  public static final String V1 = "v1";

  private final Cuba v1;

  private final Cuba quick;
  
  private final Cuba gems;

  public ApiCuba(Cuba v1, Cuba quick, Cuba gems) {
    this.v1 = v1;
    this.quick = quick;
    this.gems = gems;
  }

  /**
   * directory [v1,quick]
   */
  @Override
  public RubygemsFile on(State state) {
    switch (state.name) {
      case V1:
        return state.nested(v1);
      case RootCuba.QUICK:
        return state.nested(quick);
      case RootCuba.GEMS:
        return state.nested(gems);
      case "":
        String[] items = {V1, RootCuba.QUICK, RootCuba.GEMS};
        return state.context.factory.directory(state.context.original, items);
      default:
        return state.context.factory.notFound(state.context.original);
    }
  }
}