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
package org.sonatype.nexus.repository.upload;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @since 3.8
 */
public class UploadRegexMap
{
  private final String regex;

  private final List<String> fieldList;

  public UploadRegexMap(final String regex, final String... fieldList) {
    this.regex = regex;
    this.fieldList = Arrays.asList(fieldList);
  }

  /**
   * Regex to be used to match against the file input. Groups from the regex will be mapped to the field list.
   */
  public String getRegex() {
    return regex;
  }

  /**
   * List of fields to populate from regex groups. (Use <code>null</code> for unused regex groups)
   */
  public List<String> getFieldList() {
    return fieldList;
  }
}
