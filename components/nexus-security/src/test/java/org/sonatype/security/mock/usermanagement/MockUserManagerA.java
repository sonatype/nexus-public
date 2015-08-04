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
package org.sonatype.security.mock.usermanagement;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.UserManager;

@Singleton
@Typed(UserManager.class)
@Named("MockUserManagerA")
public class MockUserManagerA
    extends AbstractMockUserManager

{
  public MockUserManagerA() {

    DefaultUser a = new DefaultUser();
    a.setName("Joe Coder");
    a.setEmailAddress("jcoder@sonatype.org");
    a.setSource(this.getSource());
    a.setUserId("jcoder");
    a.addRole(new RoleIdentifier(this.getSource(), "RoleA"));
    a.addRole(new RoleIdentifier(this.getSource(), "RoleB"));
    a.addRole(new RoleIdentifier(this.getSource(), "RoleC"));

    DefaultUser b = new DefaultUser();
    b.setName("Christine H. Dugas");
    b.setEmailAddress("cdugas@sonatype.org");
    b.setSource(this.getSource());
    b.setUserId("cdugas");
    b.addRole(new RoleIdentifier(this.getSource(), "RoleA"));
    b.addRole(new RoleIdentifier(this.getSource(), "RoleB"));
    b.addRole(new RoleIdentifier(this.getSource(), "Role1"));

    DefaultUser c = new DefaultUser();
    c.setName("Patricia P. Peralez");
    c.setEmailAddress("pperalez@sonatype.org");
    c.setSource(this.getSource());
    c.setUserId("pperalez");

    DefaultUser d = new DefaultUser();
    d.setName("Danille S. Knudsen");
    d.setEmailAddress("dknudsen@sonatype.org");
    d.setSource(this.getSource());
    d.setUserId("dknudsen");

    DefaultUser e = new DefaultUser();
    e.setName("Anon e Mous");
    e.setEmailAddress("anonymous@sonatype.org");
    e.setSource(this.getSource());
    e.setUserId("anonymous-user");

    this.addUser(a, a.getUserId());
    this.addUser(b, b.getUserId());
    this.addUser(c, c.getUserId());
    this.addUser(d, d.getUserId());
    this.addUser(e, e.getUserId());
  }

  public String getSource() {
    return "MockUserManagerA";
  }

  public String getAuthenticationRealmName() {
    return "MockRealmA";
  }

}
