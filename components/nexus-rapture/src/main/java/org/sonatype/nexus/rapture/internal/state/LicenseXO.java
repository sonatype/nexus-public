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
package org.sonatype.nexus.rapture.internal.state;

import java.util.ArrayList;
import java.util.List;

/**
 * License exchange object.
 *
 * @since 3.0
 */
public class LicenseXO
{
  private boolean required;

  private boolean installed;

  private boolean valid;

  private int daysToExpiry;

  private List<String> features = new ArrayList<>();

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isInstalled() {
    return installed;
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public int getDaysToExpiry() {
    return daysToExpiry;
  }

  public void setDaysToExpiry(int daysToExpiry) {
    this.daysToExpiry = daysToExpiry;
  }

  public List<String> getFeatures() {
    return features;
  }

  public void setFeatures(List<String> features) {
    this.features = features;
  }

  @Override
  public String toString() {
    return "LicenseXO{" +
        "required=" + required +
        ", installed=" + installed +
        ", valid=" + valid +
        ", daysToExpiry=" + daysToExpiry +
        ", features=" + features +
        '}';
  }
}
