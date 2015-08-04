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
package org.sonatype.nexus.test.utils;

import java.util.Comparator;

import org.sonatype.security.model.CUser;


public class UserComparator
    implements Comparator<CUser>
{

  public int compare(CUser user1, CUser user2) {
    // quick outs
    if (user1 == null || user2 == null) {
      return -1;
    }

    if (user1 == user2 || user1.equals(user2)) {
      return 0;
    }

    if (user1.getEmail() == null) {
      if (user2.getEmail() != null) {
        return -1;
      }
    }
    else if (!user1.getEmail().equals(user2.getEmail())) {
      return -1;
    }
        /*if ( user1.getModelEncoding() == null )
        {
            if ( user2.getModelEncoding() != null )
                return -1;
        }
        else if ( !user1.getModelEncoding().equals( user2.getModelEncoding() ) )
            return -1;*/
    if (user1.getFirstName() == null) {
      if (user2.getFirstName() != null) {
        return -1;
      }
    }
    else if (!user1.getFirstName().equals(user2.getFirstName())) {
      return -1;
    }
    //        if ( user1.getPassword() == null )
    //        {
    //            if ( user2.getPassword() != null )
    //                return -1;
    //        }
    //        else if ( !user1.getPassword().equals( user2.getPassword() ) )
    //            return -1;
    //        if ( user1.getRoles() == null )
    //        {
    //            if ( user2.getRoles() != null )
    //                return -1;
    //        }
    //        else if ( !user1.getRoles().equals( user2.getRoles() ) )
    //            return -1;
    if (user1.getStatus() == null) {
      if (user2.getStatus() != null) {
        return -1;
      }
    }
    else if (!user1.getStatus().equals(user2.getStatus())) {
      return -1;
    }
    if (user1.getId() == null) {
      if (user2.getId() != null) {
        return -1;
      }
    }
    else if (!user1.getId().equals(user2.getId())) {
      return -1;
    }
    return 0;
  }
}
