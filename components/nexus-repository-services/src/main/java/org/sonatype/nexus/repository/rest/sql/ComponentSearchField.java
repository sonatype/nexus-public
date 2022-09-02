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
package org.sonatype.nexus.repository.rest.sql;

import org.sonatype.nexus.repository.rest.SearchFieldSupport;

/**
 * A field on the component table.
 *
 * @since 3.38
 */
public class ComponentSearchField
    extends SearchFieldSupport
{
  public static final SearchFieldSupport FORMAT = new ComponentSearchField("format");

  private static final String TABLE = "${format}_component";

  public static final ComponentSearchField NAMESPACE = new ComponentSearchField("namespace");

  public static final SearchFieldSupport NAME = new ComponentSearchField("search_component_name");

  public static final SearchFieldSupport VERSION = new ComponentSearchField("version");

  public static final SearchFieldSupport PRERELEASE = new ComponentSearchField("prerelease");

  public static final SearchFieldSupport FORMAT_FIELD_1 = new ComponentSearchField("format_field_1");

  public static final SearchFieldSupport FORMAT_FIELD_2 = new ComponentSearchField("format_field_2");

  public static final SearchFieldSupport FORMAT_FIELD_3 = new ComponentSearchField("format_field_3");

  public static final SearchFieldSupport FORMAT_FIELD_4 = new ComponentSearchField("format_field_4");

  public static final SearchFieldSupport FORMAT_FIELD_5 = new ComponentSearchField("format_field_5");

  public ComponentSearchField(final String columnName) {
    super(TABLE, columnName);
  }
}
