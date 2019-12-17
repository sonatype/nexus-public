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
package org.sonatype.nexus.datastore.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link DataAccess} template whose schema contains a placeholder variable.
 *
 * To use a template extend the type and prefix the simple name. The lower-case form
 * of that prefix will be used to replace the placeholder variable in the template:
 *
 * <pre>
 * &#64;SchemaTemplate("format")
 * public interface AssetDAO
 * {
 *   // DAO whose schema is a template containing ${format}
 * }
 *
 * public interface MavenAssetDAO
 *     extends AssetDAO
 * {
 *   // uses AssetDAO schema with ${format} replaced by maven
 * }
 * </pre>
 *
 * @since 3.20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SchemaTemplate
{
  /**
   * The name of the placeholder variable in the schema.
   */
  String value();
}
