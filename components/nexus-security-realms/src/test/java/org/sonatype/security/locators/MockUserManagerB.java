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
package org.sonatype.security.locators;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserStatus;

@Singleton
@Typed(UserManager.class)
@Named("MockUserManagerB")
public class MockUserManagerB
    extends AbstractTestUserManager
{

  public String getSource() {
    return "MockUserManagerB";
  }

  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    DefaultUser a = new DefaultUser();
    a.setName("Brenda D. Burton");
    a.setEmailAddress("bburton@sonatype.org");
    a.setSource(this.getSource());
    a.setUserId("bburton");
    a.setStatus(UserStatus.active);
    a.addRole(this.createFakeRole("RoleA"));
    a.addRole(this.createFakeRole("RoleB"));
    a.addRole(this.createFakeRole("RoleC"));

    DefaultUser b = new DefaultUser();
    b.setName("Julian R. Blevins");
    b.setEmailAddress("jblevins@sonatype.org");
    b.setSource(this.getSource());
    b.setUserId("jblevins");
    b.setStatus(UserStatus.active);
    b.addRole(this.createFakeRole("RoleA"));
    b.addRole(this.createFakeRole("RoleB"));

    DefaultUser c = new DefaultUser();
    c.setName("Kathryn J. Simmons");
    c.setEmailAddress("ksimmons@sonatype.org");
    c.setSource(this.getSource());
    c.setUserId("ksimmons");
    c.setStatus(UserStatus.active);
    c.addRole(this.createFakeRole("RoleA"));
    c.addRole(this.createFakeRole("RoleB"));

    DefaultUser d = new DefaultUser();
    d.setName("Florence T. Dahmen");
    d.setEmailAddress("fdahmen@sonatype.org");
    d.setSource(this.getSource());
    d.setUserId("fdahmen");
    d.setStatus(UserStatus.active);
    d.addRole(this.createFakeRole("RoleA"));
    d.addRole(this.createFakeRole("RoleB"));

    DefaultUser e = new DefaultUser();
    e.setName("Jill  Codar");
    e.setEmailAddress("jcodar@sonatype.org");
    e.setSource(this.getSource());
    e.setUserId("jcodar");
    e.setStatus(UserStatus.active);

    // DefaultUser f = new DefaultUser();
    // f.setName( "Joe Coder" );
    // f.setEmailAddress( "jcoder@sonatype.org" );
    // f.setSource( this.getSource() );
    // f.setUserId( "jcoder" );
    // f.addRole( this.createFakeRole( "Role1" ) );
    // f.addRole( this.createFakeRole( "Role2" ) );
    // f.addRole( this.createFakeRole( "Role3" ) );

    users.add(a);
    users.add(b);
    users.add(c);
    users.add(d);
    users.add(e);
    // users.add( f );

    return users;
  }

  public String getAuthenticationRealmName() {
    return null;
  }

}
