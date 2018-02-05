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
/**
 * This package contains classes originally lifted from the Apache Geronimo project.  These classes need to keep the ASF headers.<BR/><BR/>
 * KeystoreInstance and Exceptions came from geronimo-management: https://svn.apache.org/repos/asf/geronimo/server/trunk@1211757 <BR/>
 * FileKeystoreInstance from geronimo-security: https://svn.apache.org/repos/asf/geronimo/server/trunk@1183230
 *
 * These classes contain slight modifications to remove other geronimo dependencies.  We will try to make as few changes to these
 * classes as possible with the hope we can merge in future changes/bug fixes.
 */

package org.sonatype.nexus.ssl.internal.geronimo;