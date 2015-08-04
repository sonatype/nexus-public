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
package org.sonatype.nexus.rest.model;

import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import com.thoughtworks.xstream.XStream;

/**
 * This is the "old" XStream configurator, and is used on server side, in Nexus core. It's main problem is that it
 * assumes plexus-restlet-bridge is on classpath (untrue in case of clients, at least it's not wanted, as it has huge
 * dependency trail), as it relies on "error message" classes defined over there. If you are on client side, you don't
 * want to use this configurator, use the{@link XStreamConfiguratorLightweight} instead.
 *
 * @author cstamas
 */
public class XStreamConfigurator
{
  public static XStream configureXStream(XStream xstream) {
    return XStreamConfiguratorLightweight.configureXStream(xstream, ErrorResponse.class, ErrorMessage.class);
  }
}
