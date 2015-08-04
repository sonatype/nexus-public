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
package org.sonatype.configuration.upgrade;

/**
 * An upgrade message used to hold the current version and the configuration itself. Since modello generated classes
 * differs from model version to model version, it is held as Object, and the needed converter will cast it to what it
 * needs.
 *
 * @author cstamas
 */
public class UpgradeMessage
{
  private String modelVersion;

  private Object configuration;

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  public Object getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Object configuration) {
    this.configuration = configuration;
  }
}
