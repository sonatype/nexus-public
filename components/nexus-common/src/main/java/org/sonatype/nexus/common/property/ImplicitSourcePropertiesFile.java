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
package org.sonatype.nexus.common.property;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

/**
 * Represents a properties file that maintains the state of the {@link Reader}/{@link java.io.Writer}
 * or {@link java.io.InputStream}/{@link java.io.OutputStream} internally and does not require a parameter
 * to {@code load()} or {@code store()}
 *
 * @since 3.15
 */
public abstract class ImplicitSourcePropertiesFile
    extends Properties
{
  public abstract void load() throws IOException;

  public abstract void store() throws IOException;

  public abstract boolean exists() throws IOException;
}
