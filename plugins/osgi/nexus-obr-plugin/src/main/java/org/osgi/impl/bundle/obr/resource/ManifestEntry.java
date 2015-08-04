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
package org.osgi.impl.bundle.obr.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ManifestEntry
    implements Comparable
{
  String name;

  VersionRange version;

  Map attributes;

  public Map directives;

  public Set uses;

  public ManifestEntry(String name) {
    this.name = name;
  }

  public ManifestEntry(String name, VersionRange version) {
    this.name = name;
    this.version = version;
  }

  public String toString() {
    if (version == null) {
      return name;
    }
    return name + " ;version=" + version;
  }

  public String getName() {
    return name;
  }

  public VersionRange getVersion() {
    if (version != null) {
      return version;
    }
    return new VersionRange("0");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    ManifestEntry p = (ManifestEntry) o;
    return name.compareTo(p.name);
  }

  /**
   * @return
   */
  public Object getPath() {
    return getName().replace('.', '/');
  }

  public Map getDirectives() {
    return directives;
  }

  public Map getAttributes() {
    return attributes;
  }

  /**
   * @param parameter
   */
  public void addParameter(Parameter parameter) {
    switch (parameter.type) {
      case Parameter.ATTRIBUTE:
        if (attributes == null) {
          attributes = new HashMap();
        }
        attributes.put(parameter.key, parameter.value);
        if (parameter.key.equalsIgnoreCase("version")
            || parameter.key
            .equalsIgnoreCase("specification-version")) {
          this.version = new VersionRange(parameter.value);
        }
        break;

      case Parameter.DIRECTIVE:
        if (directives == null) {
          directives = new HashMap();
        }
        directives.put(parameter.key, parameter.value);
        break;
    }
  }

  public ManifestEntry getAlias(String key) {
    ManifestEntry me = new ManifestEntry(key);
    me.attributes = attributes;
    me.directives = directives;
    me.version = version;
    return me;
  }

  public String getDirective(String directive) {
    if (directives == null) {
      return null;
    }

    return (String) directives.get(directive);
  }

  public String getAttribute(String attribute) {
    if (attributes == null) {
      return null;
    }

    return (String) attributes.get(attribute);
  }

}
