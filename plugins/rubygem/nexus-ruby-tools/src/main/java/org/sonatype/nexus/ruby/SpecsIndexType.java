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
package org.sonatype.nexus.ruby;

public enum SpecsIndexType
{
  RELEASE, PRERELEASE, LATEST;

  public String filename() {
    StringBuilder name = new StringBuilder();
    if (this != RELEASE) {
      name.append(name().toLowerCase().replaceFirst("^release", ""))
          .append("_");
    }
    return name.append("specs.4.8").toString();
  }

  public String filepath() {
    return "/" + filename();
  }

  public String filepathGzipped() {
    return filepath() + ".gz";
  }

  public static SpecsIndexType fromFilename(String name) {
    try {
      // possible names are:
      //    latest_specs.4.8  latest_specs.4.8.gz
      //    prerelease_specs.4.8  prerelease_specs.4.8.gz
      //    specs.4.8  specs.4.8.gz
      name = name.replace(".gz", "")
          .replace("/", "") // no leading slash
          .toUpperCase();
      if ("SPECS.4.8".equals(name)) // 'specs' case
      {
        return RELEASE;
      }
      name = name.replace("SPECS.4.8", "")
          .replace("_", "");
      return valueOf(name);
    }
    catch (IllegalArgumentException e) {
      return null; // not a valid filename
    }
  }
}