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
package org.sonatype.nexus.configuration.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.util.Inflector;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.reflection.Reflector;
import org.codehaus.plexus.util.reflection.ReflectorException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractXpp3DomExternalConfigurationHolder
    implements Cloneable
{
  private final Xpp3Dom configuration;

  private static final Reflector reflector = new Reflector();

  public AbstractXpp3DomExternalConfigurationHolder(Xpp3Dom configuration) {
    this.configuration = configuration;
  }

  public Xpp3Dom getRootNode() {
    return configuration;
  }

  public void apply(AbstractXpp3DomExternalConfigurationHolder configHolder) {
    //I took out the merge code here, as the incoming config will always contain what
    //we want to store in config, there is no need to pull in stale data from the original
    //config.  Below we simply clear the old config, and insert into it, the new config
    Xpp3Dom newConfig = configHolder.getRootNode();

    // shave off config root node
    while (configuration.getChildCount() > 0) {
      configuration.removeChild(0);
    }

    // and put beneath it the merge result
    for (int i = 0; i < newConfig.getChildCount(); i++) {
      configuration.addChild(newConfig.getChild(i));
    }
  }

  @Override
  public Object clone() {
    try {
      return reflector.newInstance(this.getClass(), new Object[]{copyTree(configuration)});
    }
    catch (ReflectorException e) {
      throw new IllegalArgumentException(
          "AbstractExternalConfigurationHolder class sublasses must have contructor that takes Xpp3Dom! This instance \""
              + this.getClass().getName() + "\" does not have it!", e);
    }
  }

  public final void validate(ApplicationConfiguration applicationConfiguration, CoreConfiguration owner)
      throws ConfigurationException
  {
    checkValidationResponse(doValidateChanges(applicationConfiguration, owner, configuration));
  }

  public abstract ValidationResponse doValidateChanges(ApplicationConfiguration applicationConfiguration,
                                                       CoreConfiguration owner, Xpp3Dom configuration);

  // ==

  protected void checkValidationResponse(ValidationResponse response)
      throws ConfigurationException
  {
    if (!response.isValid()) {
      throw new InvalidConfigurationException(response);
    }
  }

  // ==

  protected Xpp3Dom copyTree(Xpp3Dom root) {
    Xpp3Dom clone = new Xpp3Dom(root.getName());

    // copy attributes
    for (String attrName : root.getAttributeNames()) {
      clone.setAttribute(attrName, root.getAttribute(attrName));
    }

    // copy value
    clone.setValue(root.getValue());

    // copy children
    for (Xpp3Dom rootChild : root.getChildren()) {
      Xpp3Dom cloneChild = copyTree(rootChild);

      clone.addChild(cloneChild);
    }

    return clone;
  }

  // ==

  /**
   * Gets the node value, creating it on the fly if not existing.
   */
  protected String getNodeValue(Xpp3Dom parent, String name, String defaultValue) {
    Xpp3Dom node = parent.getChild(name);

    if (node == null) {
      return defaultValue;

      // do NOT create nodes for inspection, only explicitly with setNodeValue()
      // node = new Xpp3Dom( name );

      // node.setValue( defaultValue );

      // parent.addChild( node );
    }

    return node.getValue();
  }

  /**
   * Sets node value, creating it on the fly if not existing.
   */
  protected void setNodeValue(Xpp3Dom parent, String name, String value) {
    Xpp3Dom node = parent.getChild(name);

    if (node == null) {
      node = new Xpp3Dom(name);

      parent.addChild(node);
    }

    node.setValue(value);
  }

  /**
   * Gets a collection as unmodifiable list.
   *
   * @return a List, never null
   */
  protected List<String> getCollection(Xpp3Dom parent, String name) {
    Xpp3Dom node = parent.getChild(name);

    if (node == null) {
      return Collections.emptyList();

      // do NOT create nodes for inspection, only explicitly with setNodeValue()
      // node = new Xpp3Dom( name );

      // parent.addChild( node );
    }

    ArrayList<String> result = new ArrayList<String>(node.getChildCount());

    for (Xpp3Dom child : node.getChildren()) {
      result.add(child.getValue());
    }

    return Collections.unmodifiableList(result);
  }

  protected void setCollection(Xpp3Dom parent, String name, Collection<String> values) {
    Xpp3Dom node = parent.getChild(name);

    if (node == null) {
      node = new Xpp3Dom(name);
      parent.addChild(node);
    }

    for (int i = node.getChildCount() - 1; i >= 0; i--) {
      node.removeChild(i);
    }

    String childName = Inflector.getInstance().singularize(name);

    for (String childVal : values) {
      Xpp3Dom child = new Xpp3Dom(childName);

      child.setValue(childVal);

      node.addChild(child);
    }
  }

  /**
   * Adds a value to collection. Collection node is created on the fly.
   */
  protected boolean addToCollection(Xpp3Dom parent, String name, String value, boolean keepUnique) {
    Xpp3Dom node = parent.getChild(name);

    if (node == null) {
      node = new Xpp3Dom(name);

      parent.addChild(node);
    }

    // if keeping it unique, we must first look is value aready in
    if (keepUnique) {
      for (Xpp3Dom child : node.getChildren()) {
        if (StringUtils.equals(value, child.getValue())) {
          return false;
        }
      }
    }

    String childName = Inflector.getInstance().singularize(name);

    Xpp3Dom child = new Xpp3Dom(childName);

    child.setValue(value);

    node.addChild(child);

    return true;
  }

  /**
   * Removes a value from a collection. Collection node is created if not exists.
   */
  protected boolean removeFromCollection(Xpp3Dom parent, String name, String value) {
    Xpp3Dom node = parent.getChild(name);

    if (node == null) {
      return false;
    }

    // Unfortunately simply removing the child node from the Xpp3Dom object is causing
    // merge issues at a later point (seems some remnants are left in the Xpp3Dom object
    // and is causing a child to get duplicated).
    // So we simply build new child list and call setCollection()

    List<String> children = new ArrayList<String>();

    boolean removed = false;

    for (Xpp3Dom child : node.getChildren()) {
      if (!StringUtils.equals(value, child.getValue())) {
        children.add(child.getValue());
      }
      else {
        removed = true;
      }
    }

    setCollection(parent, name, children);

    return removed;
  }
}
