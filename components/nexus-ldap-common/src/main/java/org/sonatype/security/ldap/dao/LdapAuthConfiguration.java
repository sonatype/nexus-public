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
package org.sonatype.security.ldap.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.util.StringUtils;

public class LdapAuthConfiguration
{

  /**
   * The Constant DEFAULT_NAME_ATTRIBUTE.
   */
  public static final String DEFAULT_NAME_ATTRIBUTE = "sn";

  /**
   * The Constant DEFAULT_USER_MEMBER_OF.
   */
  public static final String DEFAULT_USER_MEMBER_OF = "memberOf";

  /**
   * The Constant DEFAULT_EMAIL_ADDRESS_ATTRIBUTE.
   */
  public static final String DEFAULT_EMAIL_ADDRESS_ATTRIBUTE = "mail";

  /**
   * The Constant DEFAULT_WEBSITE_ATTRIBUTE.
   */
  public static final String DEFAULT_WEBSITE_ATTRIBUTE = "labeledUri";

  /**
   * The Constant DEFAULT_WEB_URI_LABEL.
   */
  public static final String DEFAULT_WEB_URI_LABEL = "Web Site";

  /**
   * The Constant DEFAULT_USERNAME_ATTRIBUTE.
   */
  private static final String DEFAULT_USERNAME_ATTRIBUTE = "uid";

  /**
   * The Constant DEFAULT_GROUP_ID_ATTRIBUTE.
   */
  private static final String DEFAULT_GROUP_ID_ATTRIBUTE = "cn";

  /**
   * The Constant DEFAULT_USER_PASSWORD_ATTRIBUTE.
   */
  private static final String DEFAULT_USER_PASSWORD_ATTRIBUTE = "userPassword";

  /**
   * The Constant DEFAULT_USER_PASSWORD_ENCODING.
   */
  private static final String DEFAULT_USER_PASSWORD_ENCODING = "crypt";

  /**
   * The Constant DEFAULT_USER_OBJECTCLASS.
   */
  private static final String DEFAULT_USER_OBJECTCLASS = "inetOrgPerson";

  /**
   * The Constant DEFAULT_GROUP_OBJECTCLASS.
   */
  private static final String DEFAULT_GROUP_OBJECTCLASS = "groupOfNames";

  // calculated.

  /**
   * The group reverse mappings.
   */
  private Map<String, Set<String>> groupReverseMappings;

  /**
   * The group mappings.
   */
  private Map<String, String> groupMappings;

  /**
   * The group member format.
   */
  private String groupMemberFormat;

  /**
   * The group object class.
   */
  private String groupObjectClass = DEFAULT_GROUP_OBJECTCLASS;

  /**
   * The group base dn.
   */
  private String groupBaseDn;

  /**
   * The group id attribute.
   */
  private String groupIdAttribute = DEFAULT_GROUP_ID_ATTRIBUTE;

  /**
   * The group member attribute.
   */
  private String groupMemberAttribute;

  /**
   * The user object class.
   */
  private String userObjectClass = DEFAULT_USER_OBJECTCLASS;

  /**
   * The user base dn.
   */
  private String userBaseDn;

  /**
   * The user id attribute.
   */
  private String userIdAttribute = DEFAULT_USERNAME_ATTRIBUTE;

  /**
   * The password attribute.
   */
  private String passwordAttribute = DEFAULT_USER_PASSWORD_ATTRIBUTE;

  //    /** The password encoding. */
  //    private String passwordEncoding = DEFAULT_USER_PASSWORD_ENCODING;

  /**
   * The user real name attribute.
   */
  private String userRealNameAttribute = DEFAULT_NAME_ATTRIBUTE;

  /**
   * The user member of attribute.
   */
  private String userMemberOfAttribute = DEFAULT_USER_MEMBER_OF;

  /**
   * The email address attribute.
   */
  private String emailAddressAttribute = DEFAULT_EMAIL_ADDRESS_ATTRIBUTE;

  /**
   * The website attribute.
   */
  private String websiteAttribute = DEFAULT_WEBSITE_ATTRIBUTE;

  /**
   * for parsing labeledUri attributes...
   */
  private String websiteUriLabel = DEFAULT_WEB_URI_LABEL;

  /**
   * If not, don't parse it as a labelUri.
   */
  private boolean isWebsiteAttributeLabelUri = true;

  private String ldapFilter;

  private boolean ldapGroupsAsRoles;

  private boolean userSubtree;

  private boolean groupSubtree;


  /**
   * Gets the group base dn.
   *
   * @return the group base dn
   */
  public String getGroupBaseDn() {
    return groupBaseDn;
  }

  /**
   * Sets the group base dn.
   *
   * @param groupBaseDn the new group base dn
   */
  public void setGroupBaseDn(String groupBaseDn) {
    this.groupBaseDn = groupBaseDn;
  }

  /**
   * Gets the group id attribute.
   *
   * @return the group id attribute
   */
  public String getGroupIdAttribute() {
    return groupIdAttribute;
  }

  /**
   * Sets the group id attribute.
   *
   * @param groupIdAttribute the new group id attribute
   */
  public void setGroupIdAttribute(String groupIdAttribute) {
    this.groupIdAttribute = groupIdAttribute;
  }

  /**
   * Gets the group mappings.
   *
   * @return the group mappings
   */
  public Map<String, String> getGroupMappings() {
    return groupMappings;
  }

  /**
   * Sets the group mappings.
   *
   * @param groupMappings the group mappings
   */
  public synchronized void setGroupMappings(Map<String, String> groupMappings) {
    this.groupMappings = groupMappings;
    this.groupReverseMappings = null;
  }

  /**
   * Gets the group member attribute.
   *
   * @return the group member attribute
   */
  public String getGroupMemberAttribute() {
    return groupMemberAttribute;
  }

  /**
   * Sets the group member attribute.
   *
   * @param groupMemberAttribute the new group member attribute
   */
  public void setGroupMemberAttribute(String groupMemberAttribute) {
    this.groupMemberAttribute = groupMemberAttribute;
  }

  /**
   * Gets the group reverse mappings.
   *
   * @return the group reverse mappings
   */
  public synchronized Map<String, Set<String>> getGroupReverseMappings() {
    if (groupReverseMappings == null) {
      groupReverseMappings = new HashMap<String, Set<String>>();

      if (groupMappings != null) {
        for (Iterator it = groupMappings.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry = (Map.Entry) it.next();
          String logical = (String) entry.getKey();
          String real = (String) entry.getValue();

          Set<String> logicalMappings = groupReverseMappings.get(real);
          if (logicalMappings == null) {
            logicalMappings = new LinkedHashSet<String>();
            groupReverseMappings.put(real, logicalMappings);
          }

          logicalMappings.add(logical);
        }
      }
    }

    return groupReverseMappings;
  }

  /**
   * Gets the email address attribute.
   *
   * @return the email address attribute
   */
  public String getEmailAddressAttribute() {
    return emailAddressAttribute;
  }

  /**
   * Sets the email address attribute.
   *
   * @param emailAddressAttribute the new email address attribute
   */
  public void setEmailAddressAttribute(String emailAddressAttribute) {
    this.emailAddressAttribute = emailAddressAttribute;
  }

  /**
   * Gets the user real name attribute.
   *
   * @return the user real name attribute
   */
  public String getUserRealNameAttribute() {
    return userRealNameAttribute;
  }

  /**
   * Sets the user real name attribute.
   *
   * @param nameAttribute the new user real name attribute
   */
  public void setUserRealNameAttribute(String nameAttribute) {
    this.userRealNameAttribute = nameAttribute;
  }

  /**
   * Gets the password attribute.
   *
   * @return the password attribute
   */
  public String getPasswordAttribute() {
    return passwordAttribute;
  }

  /**
   * Sets the password attribute.
   *
   * @param passwordAttribute the new password attribute
   */
  public void setPasswordAttribute(String passwordAttribute) {
    this.passwordAttribute = passwordAttribute;
  }

  /**
   * Gets the user base dn.
   *
   * @return the user base dn
   */
  public String getUserBaseDn() {
    return userBaseDn;
  }

  /**
   * Sets the user base dn.
   *
   * @param userBaseDn the new user base dn
   */
  public void setUserBaseDn(String userBaseDn) {
    this.userBaseDn = userBaseDn;
  }

  /**
   * Gets the user id attribute.
   *
   * @return the user id attribute
   */
  public String getUserIdAttribute() {
    return userIdAttribute;
  }

  /**
   * Sets the user id attribute.
   *
   * @param userIdAttribute the new user id attribute
   */
  public void setUserIdAttribute(String userIdAttribute) {
    this.userIdAttribute = userIdAttribute;
  }

  /**
   * Gets the website attribute.
   *
   * @return the website attribute
   */
  public String getWebsiteAttribute() {
    return websiteAttribute;
  }

  /**
   * Sets the website attribute.
   *
   * @param websiteAttribute the new website attribute
   */
  public void setWebsiteAttribute(String websiteAttribute) {
    this.websiteAttribute = websiteAttribute;
  }

  /**
   * Checks if is website attribute label uri.
   *
   * @return true, if is website attribute label uri
   */
  public boolean isWebsiteAttributeLabelUri() {
    return isWebsiteAttributeLabelUri;
  }

  /**
   * Sets the website attribute label uri.
   *
   * @param websiteIsLabelUri the new website attribute label uri
   */
  public void setWebsiteAttributeLabelUri(boolean websiteIsLabelUri) {
    this.isWebsiteAttributeLabelUri = websiteIsLabelUri;
  }

  /**
   * Gets the website uri label.
   *
   * @return the website uri label
   */
  public String getWebsiteUriLabel() {
    return websiteUriLabel;
  }

  /**
   * Sets the website uri label.
   *
   * @param websiteUriLabel the new website uri label
   */
  public void setWebsiteUriLabel(String websiteUriLabel) {
    this.websiteUriLabel = websiteUriLabel;
  }

  /**
   * Gets the user attributes.
   *
   * @return the user attributes
   */
  public synchronized String[] getUserAttributes() {
    List<String> result = new ArrayList<String>();
    String[] allAttributes =
        new String[]{
            userIdAttribute, passwordAttribute, userRealNameAttribute, emailAddressAttribute,
            websiteAttribute, userMemberOfAttribute
        };
    for (String attribute : allAttributes) {
      if (StringUtils.isNotBlank(attribute)) {
        result.add(attribute);
      }
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Gets the group member format.
   *
   * @return the group member format
   */
  public String getGroupMemberFormat() {
    return groupMemberFormat;
  }

  /**
   * Sets the group member format.
   *
   * @param groupMemberFormat the new group member format
   */
  public void setGroupMemberFormat(String groupMemberFormat) {
    this.groupMemberFormat = groupMemberFormat;
  }

  /**
   * Gets the group object class.
   *
   * @return the group object class
   */
  public String getGroupObjectClass() {
    return groupObjectClass;
  }

  /**
   * Sets the group object class.
   *
   * @param groupObjectClass the new group object class
   */
  public void setGroupObjectClass(String groupObjectClass) {
    this.groupObjectClass = groupObjectClass;
  }

  /**
   * Gets the user object class.
   *
   * @return the user object class
   */
  public String getUserObjectClass() {
    return userObjectClass;
  }

  /**
   * Sets the user object class.
   *
   * @param userObjectClass the new user object class
   */
  public void setUserObjectClass(String userObjectClass) {
    this.userObjectClass = userObjectClass;
  }


  //    /**
  //     * @return the passwordEncoding
  //     */
  //    public String getPasswordEncoding()
  //    {
  //        return passwordEncoding;
  //    }
  //
  //    /**
  //     * @param passwordEncoding the passwordEncoding to set
  //     */
  //    public void setPasswordEncoding( String passwordEncoding )
  //    {
  //        this.passwordEncoding = passwordEncoding;
  //    }


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

  public String getLdapFilter() {
    return ldapFilter;
  }

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
    result = prime * result + ((groupMappings == null) ? 0 : groupMappings.hashCode());
    result = prime * result + ((groupMemberAttribute == null) ? 0 : groupMemberAttribute.hashCode());
    result = prime * result + ((groupMemberFormat == null) ? 0 : groupMemberFormat.hashCode());
    result = prime * result + ((groupObjectClass == null) ? 0 : groupObjectClass.hashCode());
    result = prime * result + ((groupReverseMappings == null) ? 0 : groupReverseMappings.hashCode());
    result = prime * result + (groupSubtree ? 1231 : 1237);
    result = prime * result + (isWebsiteAttributeLabelUri ? 1231 : 1237);
    result = prime * result + (ldapGroupsAsRoles ? 1231 : 1237);
    result = prime * result + ((passwordAttribute == null) ? 0 : passwordAttribute.hashCode());
    result = prime * result + ((userBaseDn == null) ? 0 : userBaseDn.hashCode());
    result = prime * result + ((userIdAttribute == null) ? 0 : userIdAttribute.hashCode());
    result = prime * result + ((userMemberOfAttribute == null) ? 0 : userMemberOfAttribute.hashCode());
    result = prime * result + ((userObjectClass == null) ? 0 : userObjectClass.hashCode());
    result = prime * result + ((userRealNameAttribute == null) ? 0 : userRealNameAttribute.hashCode());
    result = prime * result + (userSubtree ? 1231 : 1237);
    result = prime * result + ((websiteAttribute == null) ? 0 : websiteAttribute.hashCode());
    result = prime * result + ((websiteUriLabel == null) ? 0 : websiteUriLabel.hashCode());
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
    final LdapAuthConfiguration other = (LdapAuthConfiguration) obj;
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
    if (groupMappings == null) {
      if (other.groupMappings != null) {
        return false;
      }
    }
    else if (!groupMappings.equals(other.groupMappings)) {
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
    if (groupReverseMappings == null) {
      if (other.groupReverseMappings != null) {
        return false;
      }
    }
    else if (!groupReverseMappings.equals(other.groupReverseMappings)) {
      return false;
    }
    if (groupSubtree != other.groupSubtree) {
      return false;
    }
    if (isWebsiteAttributeLabelUri != other.isWebsiteAttributeLabelUri) {
      return false;
    }
    if (ldapGroupsAsRoles != other.ldapGroupsAsRoles) {
      return false;
    }
    if (passwordAttribute == null) {
      if (other.passwordAttribute != null) {
        return false;
      }
    }
    else if (!passwordAttribute.equals(other.passwordAttribute)) {
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
    if (websiteAttribute == null) {
      if (other.websiteAttribute != null) {
        return false;
      }
    }
    else if (!websiteAttribute.equals(other.websiteAttribute)) {
      return false;
    }
    if (websiteUriLabel == null) {
      if (other.websiteUriLabel != null) {
        return false;
      }
    }
    else if (!websiteUriLabel.equals(other.websiteUriLabel)) {
      return false;
    }
    if (ldapFilter == null) {
      if (other.ldapFilter != null) {
        return false;
      }
    }
    else if (!ldapFilter.equals(other.ldapFilter)) {
      return false;
    }
    return true;
  }


}
