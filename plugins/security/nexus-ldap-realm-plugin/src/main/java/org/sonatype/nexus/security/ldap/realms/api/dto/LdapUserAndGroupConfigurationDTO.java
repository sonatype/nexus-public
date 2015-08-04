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
package org.sonatype.nexus.security.ldap.realms.api.dto;

public class LdapUserAndGroupConfigurationDTO
{
  /**
   * Email Address Attribute.  The attribute that stores the
   * users email address.
   */
  private String emailAddressAttribute;

  /**
   * Use LDAP groups as roles.  True if LDAP groups should be
   * used as roles.
   */
  private boolean ldapGroupsAsRoles = false;

  /**
   * Group Base DN.  The base DN that defines Groups.
   */
  private String groupBaseDn;

  /**
   * Group Id Attribute. The ID attribute for the Group.
   */
  private String groupIdAttribute;

  /**
   * Group Member Attribute,  An attribute that defines the a
   * user is a member of the group.
   */
  private String groupMemberAttribute;

  /**
   * Group Member Format. The format that the user info is stored
   * in the groupMappingsAttribute.  Such as ${username}, or
   * uid=${username},ou=people,o=yourBiz.  ${username} will be
   * replaced with the username.
   */
  private String groupMemberFormat;

  /**
   * Group Object Class. The Object class used for groups.
   */
  private String groupObjectClass;

  /**
   * User Password Attribute.  The attribute that stores the
   * users password.
   */
  private String userPasswordAttribute;

  /**
   * User Id Attribute.  THe attribute of the userId field.
   */
  private String userIdAttribute;

  /**
   * User Object Class.  The object class used for users.
   */
  private String userObjectClass;

  /**
   * User Base DN. The base DN for the users.
   */
  private String userBaseDn;

  /**
   * User Real Name Attribute.  The attribute that defines the
   * users real name.
   */
  private String userRealNameAttribute;

  /**
   * Users are Stored in a subtree of the userBaseDn.
   */
  private boolean userSubtree = false;

  /**
   * Groups are Stored in a subtree of the groupBaseDn.
   */
  private boolean groupSubtree = false;

  /**
   * Groups are generally one of two types in LDAP systems -
   * static or dynamic. A static group maintains its own
   * membership list. A dynamic group records its membership on a
   * user entry. If dynamic groups this should be set to the
   * attribute used to store the group string in the user object.
   */
  private String userMemberOfAttribute;

  /**
   * Filter to retrieve only users with specific attribute set.
   */
  private String ldapFilter;

  /**
   * @return the emailAddressAttribute
   */
  public String getEmailAddressAttribute() {
    return emailAddressAttribute;
  }

  /**
   * @param emailAddressAttribute the emailAddressAttribute to set
   */
  public void setEmailAddressAttribute(String emailAddressAttribute) {
    this.emailAddressAttribute = emailAddressAttribute;
  }

  /**
   * @return the groupBaseDn
   */
  public String getGroupBaseDn() {
    return groupBaseDn;
  }

  /**
   * @param groupBaseDn the groupBaseDn to set
   */
  public void setGroupBaseDn(String groupBaseDn) {
    this.groupBaseDn = groupBaseDn;
  }

  /**
   * @return the groupIdAttribute
   */
  public String getGroupIdAttribute() {
    return groupIdAttribute;
  }

  /**
   * @param groupIdAttribute the groupIdAttribute to set
   */
  public void setGroupIdAttribute(String groupIdAttribute) {
    this.groupIdAttribute = groupIdAttribute;
  }

  /**
   * @return the groupMemberAttribute
   */
  public String getGroupMemberAttribute() {
    return groupMemberAttribute;
  }

  /**
   * @param groupMemberAttribute the groupMemberAttribute to set
   */
  public void setGroupMemberAttribute(String groupMemberAttribute) {
    this.groupMemberAttribute = groupMemberAttribute;
  }

  /**
   * @return the groupMemberFormat
   */
  public String getGroupMemberFormat() {
    return groupMemberFormat;
  }

  /**
   * @param groupMemberFormat the groupMemberFormat to set
   */
  public void setGroupMemberFormat(String groupMemberFormat) {
    this.groupMemberFormat = groupMemberFormat;
  }

  /**
   * @return the groupObjectClass
   */
  public String getGroupObjectClass() {
    return groupObjectClass;
  }

  /**
   * @param groupObjectClass the groupObjectClass to set
   */
  public void setGroupObjectClass(String groupObjectClass) {
    this.groupObjectClass = groupObjectClass;
  }

  /**
   * @return the userPasswordAttribute
   */
  public String getUserPasswordAttribute() {
    return userPasswordAttribute;
  }

  /**
   * @param userPasswordAttribute the userPasswordAttribute to set
   */
  public void setUserPasswordAttribute(String userPasswordAttribute) {
    this.userPasswordAttribute = userPasswordAttribute;
  }

  /**
   * @return the userIdAttribute
   */
  public String getUserIdAttribute() {
    return userIdAttribute;
  }

  /**
   * @param userIdAttribute the userIdAttribute to set
   */
  public void setUserIdAttribute(String userIdAttribute) {
    this.userIdAttribute = userIdAttribute;
  }

  /**
   * @return the userObjectClass
   */
  public String getUserObjectClass() {
    return userObjectClass;
  }

  /**
   * @param userObjectClass the userObjectClass to set
   */
  public void setUserObjectClass(String userObjectClass) {
    this.userObjectClass = userObjectClass;
  }

  /**
   * @return the userBaseDn
   */
  public String getUserBaseDn() {
    return userBaseDn;
  }

  /**
   * @param userBaseDn the userBaseDn to set
   */
  public void setUserBaseDn(String userBaseDn) {
    this.userBaseDn = userBaseDn;
  }

  /**
   * @return the userRealNameAttribute
   */
  public String getUserRealNameAttribute() {
    return userRealNameAttribute;
  }

  /**
   * @param userRealNameAttribute the userRealNameAttribute to set
   */
  public void setUserRealNameAttribute(String userRealNameAttribute) {
    this.userRealNameAttribute = userRealNameAttribute;
  }


  /**
   * @return the ldapGroupsAsRoles
   */
  public boolean isLdapGroupsAsRoles() {
    return ldapGroupsAsRoles;
  }

  /**
   * @param ldapGroupsAsRoles the ldapGroupsAsRoles to set
   */
  public void setLdapGroupsAsRoles(boolean ldapGroupsAsRoles) {
    this.ldapGroupsAsRoles = ldapGroupsAsRoles;
  }


  /**
   * @return the userSubtree
   */
  public boolean isUserSubtree() {
    return userSubtree;
  }

  /**
   * @param userSubtree the userSubtree to set
   */
  public void setUserSubtree(boolean userSubtree) {
    this.userSubtree = userSubtree;
  }

  /**
   * @return the groupSubtree
   */
  public boolean isGroupSubtree() {
    return groupSubtree;
  }

  /**
   * @param groupSubtree the groupSubtree to set
   */
  public void setGroupSubtree(boolean groupSubtree) {
    this.groupSubtree = groupSubtree;
  }


  public String getUserMemberOfAttribute() {
    return userMemberOfAttribute;
  }

  public void setUserMemberOfAttribute(String userMemberOfAttribute) {
    this.userMemberOfAttribute = userMemberOfAttribute;
  }

  /**
   * @return the filter to select specific users
   */
  public String getLdapFilter() {
    return ldapFilter;
  }

  /**
   * @param ldapFilter the filter to select specific users
   */
  public void setLdapFilter(String ldapFilter) {
    this.ldapFilter = ldapFilter;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((emailAddressAttribute == null) ? 0 : emailAddressAttribute.hashCode());
    result = prime * result + ((groupBaseDn == null) ? 0 : groupBaseDn.hashCode());
    result = prime * result + ((groupIdAttribute == null) ? 0 : groupIdAttribute.hashCode());
    result = prime * result + ((groupMemberAttribute == null) ? 0 : groupMemberAttribute.hashCode());
    result = prime * result + ((groupMemberFormat == null) ? 0 : groupMemberFormat.hashCode());
    result = prime * result + ((groupObjectClass == null) ? 0 : groupObjectClass.hashCode());
    result = prime * result + (groupSubtree ? 1231 : 1237);
    result = prime * result + (ldapGroupsAsRoles ? 1231 : 1237);
    result = prime * result + ((userBaseDn == null) ? 0 : userBaseDn.hashCode());
    result = prime * result + ((userIdAttribute == null) ? 0 : userIdAttribute.hashCode());
    result = prime * result + ((userMemberOfAttribute == null) ? 0 : userMemberOfAttribute.hashCode());
    result = prime * result + ((userObjectClass == null) ? 0 : userObjectClass.hashCode());
    result = prime * result + ((userPasswordAttribute == null) ? 0 : userPasswordAttribute.hashCode());
    result = prime * result + ((userRealNameAttribute == null) ? 0 : userRealNameAttribute.hashCode());
    result = prime * result + (userSubtree ? 1231 : 1237);
    result = prime * result + ((ldapFilter == null) ? 0 : ldapFilter.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LdapUserAndGroupConfigurationDTO other = (LdapUserAndGroupConfigurationDTO) obj;
    if (emailAddressAttribute == null) {
      if (other.emailAddressAttribute != null) {
        return false;
      }
    }
    else if (!emailAddressAttribute.equals(other.emailAddressAttribute)) {
      return false;
    }
    if (groupBaseDn == null) {
      if (other.groupBaseDn != null) {
        return false;
      }
    }
    else if (!groupBaseDn.equals(other.groupBaseDn)) {
      return false;
    }
    if (groupIdAttribute == null) {
      if (other.groupIdAttribute != null) {
        return false;
      }
    }
    else if (!groupIdAttribute.equals(other.groupIdAttribute)) {
      return false;
    }
    if (groupMemberAttribute == null) {
      if (other.groupMemberAttribute != null) {
        return false;
      }
    }
    else if (!groupMemberAttribute.equals(other.groupMemberAttribute)) {
      return false;
    }
    if (groupMemberFormat == null) {
      if (other.groupMemberFormat != null) {
        return false;
      }
    }
    else if (!groupMemberFormat.equals(other.groupMemberFormat)) {
      return false;
    }
    if (groupObjectClass == null) {
      if (other.groupObjectClass != null) {
        return false;
      }
    }
    else if (!groupObjectClass.equals(other.groupObjectClass)) {
      return false;
    }
    if (groupSubtree != other.groupSubtree) {
      return false;
    }
    if (ldapGroupsAsRoles != other.ldapGroupsAsRoles) {
      return false;
    }
    if (userBaseDn == null) {
      if (other.userBaseDn != null) {
        return false;
      }
    }
    else if (!userBaseDn.equals(other.userBaseDn)) {
      return false;
    }
    if (userIdAttribute == null) {
      if (other.userIdAttribute != null) {
        return false;
      }
    }
    else if (!userIdAttribute.equals(other.userIdAttribute)) {
      return false;
    }
    if (userMemberOfAttribute == null) {
      if (other.userMemberOfAttribute != null) {
        return false;
      }
    }
    else if (!userMemberOfAttribute.equals(other.userMemberOfAttribute)) {
      return false;
    }
    if (userObjectClass == null) {
      if (other.userObjectClass != null) {
        return false;
      }
    }
    else if (!userObjectClass.equals(other.userObjectClass)) {
      return false;
    }
    if (userPasswordAttribute == null) {
      if (other.userPasswordAttribute != null) {
        return false;
      }
    }
    else if (!userPasswordAttribute.equals(other.userPasswordAttribute)) {
      return false;
    }
    if (userRealNameAttribute == null) {
      if (other.userRealNameAttribute != null) {
        return false;
      }
    }
    else if (!userRealNameAttribute.equals(other.userRealNameAttribute)) {
      return false;
    }
    if (userSubtree != other.userSubtree) {
      return false;
    }
    return true;
  }


}
