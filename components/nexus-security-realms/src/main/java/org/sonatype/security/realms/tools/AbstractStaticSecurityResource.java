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
package org.sonatype.security.realms.tools;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Reader;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkState;

/**
 * An abstract class that removes the boiler plate code of reading in the security configuration.
 *
 * @author Brian Demers
 */
public abstract class AbstractStaticSecurityResource
    extends ComponentSupport
    implements StaticSecurityResource
{
  protected boolean dirty = false;

  public boolean isDirty() {
    return dirty;
  }

  protected void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  protected abstract String getResourcePath();

  public Configuration getConfiguration() {
    String resourcePath = this.getResourcePath();

    if (StringUtils.isNotEmpty(resourcePath)) {
      URL url = getClass().getResource(resourcePath);
      checkState(url != null, "Missing static security configuration resource: %s", resourcePath);
      assert url != null;

      log.debug("Loading static security configuration: {}", url);
      try (InputStream is = url.openStream();
           Reader fr = new InputStreamReader(is)) {
        SecurityConfigurationXpp3Reader reader = new SecurityConfigurationXpp3Reader();
        return reader.read(fr);
      }
      catch (Exception e) {
        log.error("Failed to read configuration", e);
      }
    }

    // any other time just return null
    return null;
  }
}
