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
package org.sonatype.security.ldap.realms.persist;

import java.io.File;

public class LdapConfigrationValidatorTestBean
{

  private File configFile;

  private int numberOfErrors;

  private int numberOfWarnings;

  public File getConfigFile() {
    return configFile;
  }

  public void setConfigFile(File configFile) {
    this.configFile = configFile;
  }

  public int getNumberOfErrors() {
    return numberOfErrors;
  }

  public void setNumberOfErrors(int numberOfErrors) {
    this.numberOfErrors = numberOfErrors;
  }

  public int getNumberOfWarnings() {
    return numberOfWarnings;
  }

  public void setNumberOfWarnings(int numberOfWarnings) {
    this.numberOfWarnings = numberOfWarnings;
  }


}
