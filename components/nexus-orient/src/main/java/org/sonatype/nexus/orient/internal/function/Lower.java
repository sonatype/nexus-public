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

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Custom LOWER function for OrientDB, using the {@link Locale#ENGLISH english} locale.
 *
 * @since 3.0
 */
@Named
@Singleton
public class Lower
    extends OSQLFunctionAbstract
{
  public Lower() {
    super("lower", 1, 1);
  }

  @Override
  public Object execute(final Object iThis,
                        final OIdentifiable iCurrentRecord,
                        final Object iCurrentResult,
                        final Object[] iParams,
                        final OCommandContext iContext)
  {
    checkArgument(iParams.length == 1);

    final Object param = iParams[0];

    if (param == null) {
      return null;
    }

    checkArgument(param instanceof String, "lower() parameter must be a string");

    return ((String) param).toLowerCase(Locale.ENGLISH);
  }


  @Override
  public String getSyntax() {
    return "lower(<string>)";
  }

  @Override
  public boolean aggregateResults() {
    return false;
  }
}
