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
package org.sonatype.security.ldap.upgrade.cipher;

import java.security.Provider;
import java.security.Security;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CryptoUtils
{
  /**
   * Exploratory part. This method returns all available services types
   */
  public static String[] getServiceTypes() {
    Set<String> result = new HashSet<String>();

    // All all providers
    Provider[] providers = Security.getProviders();
    for (int i = 0; i < providers.length; i++) {
      // Get services provided by each provider
      Set<Object> keys = providers[i].keySet();
      for (Iterator<Object> it = keys.iterator(); it.hasNext(); ) {
        String key = (String) it.next();
        key = key.split(" ")[0];

        if (key.startsWith("Alg.Alias.")) {
          // Strip the alias
          key = key.substring(10);
        }
        int ix = key.indexOf('.');
        result.add(key.substring(0, ix));
      }
    }
    return result.toArray(new String[result.size()]);
  }

  /**
   * This method returns the available implementations for a service type
   */
  public static String[] getCryptoImpls(String serviceType) {
    Set<String> result = new HashSet<String>();

    // All all providers
    Provider[] providers = Security.getProviders();
    for (int i = 0; i < providers.length; i++) {
      // Get services provided by each provider
      Set<Object> keys = providers[i].keySet();
      for (Iterator<Object> it = keys.iterator(); it.hasNext(); ) {
        String key = (String) it.next();
        key = key.split(" ")[0];

        if (key.startsWith(serviceType + ".")) {
          result.add(key.substring(serviceType.length() + 1));
        }
        else if (key.startsWith("Alg.Alias." + serviceType + ".")) {
          // This is an alias
          result.add(key.substring(serviceType.length() + 11));
        }
      }
    }
    return result.toArray(new String[result.size()]);
  }
}
