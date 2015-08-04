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
package org.sonatype.nexus.rest.model;

import com.thoughtworks.xstream.converters.basic.StringConverter;
import org.apache.commons.lang.StringEscapeUtils;

public class HtmlEscapeStringConverter
    extends StringConverter
{
  /**
   * A flag denoting should we ignore flakey clients or not.
   */
  private final boolean nullResilient;

  public HtmlEscapeStringConverter() {
    this(false);
  }

  public HtmlEscapeStringConverter(boolean nullResilient) {
    this.nullResilient = nullResilient;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean canConvert(Class type) {
    if (nullResilient && type == null) {
      return true;
    }
    return super.canConvert(type);
  }

  @Override
  public Object fromString(String str) {
    return StringEscapeUtils.escapeHtml(str);
  }
}
