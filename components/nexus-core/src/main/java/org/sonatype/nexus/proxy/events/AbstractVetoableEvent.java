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
package org.sonatype.nexus.proxy.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.events.AbstractEvent;

public class AbstractVetoableEvent<T>
    extends AbstractEvent<T>
    implements Vetoable
{
  private final ArrayList<Veto> vetos = new ArrayList<Veto>();

  public AbstractVetoableEvent(T component) {
    super(component);
  }

  public List<Veto> getVetos() {
    return Collections.unmodifiableList(vetos);
  }

  public boolean isVetoed() {
    return !vetos.isEmpty();
  }

  public void putVeto(Veto veto) {
    vetos.add(veto);
  }

  public void putVeto(Object vetoer, Throwable reason) {
    vetos.add(new Veto(vetoer, reason));
  }

  public boolean removeVeto(Veto veto) {
    return vetos.remove(veto);
  }

}
