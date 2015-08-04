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

public class RepoComboFormField
    extends Combobox<String>
{
  public static final String DEFAULT_HELP_TEXT = "Select the repository or repository group.";

  public static final String DEFAULT_LABEL = "Repository";

  public RepoComboFormField(String id, String label, String helpText, boolean required, String regexValidation) {
    super(id, label, helpText, required, regexValidation);
  }

  public RepoComboFormField(String id, String label, String helpText, boolean required) {
    super(id, label, helpText, required);
  }

  public RepoComboFormField(String id, boolean required) {
    super(id, DEFAULT_LABEL, DEFAULT_HELP_TEXT, required);
  }

  public RepoComboFormField(String id) {
    super(id, DEFAULT_LABEL, DEFAULT_HELP_TEXT, false);
  }

  public String getType() {
    return "repo";
  }

  @Override
  public String getStorePath() {
    return restlet1xStore("/repositories");
  }

  @Override
  public String getStoreRoot() {
    return "data";
  }

}
