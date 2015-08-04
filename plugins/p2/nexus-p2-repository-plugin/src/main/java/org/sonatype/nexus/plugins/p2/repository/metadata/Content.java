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
package org.sonatype.nexus.plugins.p2.repository.metadata;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Content
    extends AbstractMetadata
{
  public Content(final Xpp3Dom dom) {
    super(dom);
  }

  public Content(final String name) {
    super(new Xpp3Dom("repository"));
    setRepositoryAttributes(name);
  }

  public void setRepositoryAttributes(final String name) {
    getDom().setAttribute("name", name);
    getDom().setAttribute("type", "org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository");
    getDom().setAttribute("version", "1");
  }

  public static class Unit
      extends AbstractMetadata
  {

    protected Unit(final Xpp3Dom dom) {
      super(dom);
    }

    public Unit(final Unit other) {
      super(other);
    }

    public String getId() {
      return dom.getAttribute("id");
    }

    public String getVersion() {
      return dom.getAttribute("version");
    }
  }

  public List<Unit> getUnits() {
    final Xpp3Dom unitsDom = dom.getChild("units");

    return getUnits(unitsDom);
  }

  public static List<Unit> getUnits(final Xpp3Dom unitsDom) {
    final List<Unit> result = new ArrayList<Unit>();

    if (unitsDom != null) {
      for (final Xpp3Dom unitDom : unitsDom.getChildren("unit")) {
        result.add(new Unit(unitDom));
      }
    }

    return result;
  }

  public void setUnits(final List<Unit> units) {
    removeChild(dom, "units");
    final Xpp3Dom unitsDom = new Xpp3Dom("units");

    for (final Unit unit : units) {
      unitsDom.addChild(unit.getDom());
    }
    unitsDom.setAttribute("size", Integer.toString(units.size()));

    dom.addChild(unitsDom);
  }

}
