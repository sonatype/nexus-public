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
package org.sonatype.nexus.client.internal.rest;

import org.sonatype.nexus.client.internal.msg.ErrorMessage;
import org.sonatype.nexus.client.internal.msg.ErrorResponse;
import org.sonatype.nexus.rest.model.XStreamConfiguratorLightweight;
import org.sonatype.plexus.rest.xstream.xml.LookAheadXppDriver;

import com.thoughtworks.xstream.XStream;

/**
 * Creates XStream instances "preconfigured" for Nexus Core DTOs.
 *
 * @since 2.1
 */
public class NexusXStreamFactory
{

  /**
   * Just creates a fresh XStream instance.
   */
  public XStream createForXml() {
    final XStream xstream = new XStream(new LookAheadXppDriver());
    xstream.setMode(XStream.NO_REFERENCES);
    xstream.autodetectAnnotations(false);
    xstream.ignoreUnknownElements();
    return xstream;
  }

  /**
   * Configures the passed in instance to make it able to consume and produce the core Nexus REST DTOs.
   */
  public XStream configure(final XStream xstream) {
    // core (lightweight one)
    XStreamConfiguratorLightweight.configureXStream(xstream, ErrorResponse.class, ErrorMessage.class);
    return xstream;
  }

  /**
   * Creates and configures an XML XStream for Nexus REST API DTOs.
   */
  public XStream createAndConfigureForXml() {
    return configure(createForXml());
  }
}
