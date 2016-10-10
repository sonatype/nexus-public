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
package org.sonatype.nexus.orient.internal.function;

import javax.inject.Named;
import javax.inject.Singleton;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

/**
 * Custom CONCAT function for OrientDB.
 *
 * @since 3.0
 */
@Named
@Singleton
public class Concat
    extends OSQLFunctionAbstract
{
  public Concat() {
    super("concat", 1, 32);
  }

  @Override
  public Object execute(final Object iThis,
                        final OIdentifiable iCurrentRecord,
                        final Object iCurrentResult,
                        final Object[] iParams,
                        final OCommandContext iContext)
  {
    StringBuilder b = new StringBuilder();

    for (Object param : iParams) {
      b.append(param);
    }

    return b.toString();
  }


  @Override
  public String getSyntax() {
    return "concat(<string> [, <string ...>])";
  }

  @Override
  public boolean aggregateResults() {
    return false;
  }
}
