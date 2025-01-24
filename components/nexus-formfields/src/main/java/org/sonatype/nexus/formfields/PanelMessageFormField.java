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

package org.sonatype.nexus.formfields;

public class PanelMessageFormField
    extends AbstractFormField<String>
{
  protected static final String PANEL_TYPE_ATTRIBUTE_KEY = "panelType";

  public static final String INFO_PANEL_TYPE = "info";

  public static final String WARNING_PANEL_TYPE = "warning";

  public PanelMessageFormField(
      final String id,
      final String label,
      final String helpText,
      final String panelType)
  {
    super(id, label, helpText, false);
    getAttributes().put(PANEL_TYPE_ATTRIBUTE_KEY, panelType);
  }

  public PanelMessageFormField(
      final String id,
      final String helpText,
      final String panelType)
  {
    super(id, null, helpText, false);
    getAttributes().put(PANEL_TYPE_ATTRIBUTE_KEY, panelType);
  }

  @Override
  public String getType() {
    return "panelMessage";
  }
}
