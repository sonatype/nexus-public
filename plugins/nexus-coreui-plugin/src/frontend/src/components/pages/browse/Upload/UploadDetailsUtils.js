/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * In compound field structures, this value is used to denote the field which is mapped to the parent path in the
 * form submission data.  For instance the 'bar' data in { foo: { _: 'bar', a: 'baz' } } would be submitted
 * under the key 'foo' while the 'baz' data would be submitted under the key 'foo.a'
 */
export const COMPOUND_FIELD_PARENT_NAME = '_';

/**
 * A regex which can be used to check whether a given field key represents an asset upload name and if so to capture
 * the numeric id of the asset in its first (and only) capture group
 */
export const ASSET_NUM_MATCHER = /^asset(\d+)$/;

export const MAVEN_FORMAT = 'maven2';
export const MAVEN_COMPONENT_COORDS_GROUP = 'Component coordinates';
export const MAVEN_GENERATE_POM_FIELD_NAME = 'generate-pom';
export const MAVEN_PACKAGING_FIELD_NAME = 'packaging';
