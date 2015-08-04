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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility.Version;

import org.codehaus.plexus.util.StringUtils;

/**
 * String storage
 *
 * @author Oleg Gusakov
 * @version $Id: StringOperand.java 726701 2008-12-15 14:31:34Z hboutemy $
 */
public class StringOperand
    extends AbstractOperand
{
  private final String str;

  public StringOperand(final Version originModelVersion, final String data) {
    super(originModelVersion);

    if (StringUtils.isBlank(data)) {
      throw new IllegalArgumentException("String operand may not carry empty/null string!");
    }

    this.str = data;
  }

  public String getOperand() {
    return str;
  }
}
