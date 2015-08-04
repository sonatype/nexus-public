/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/*global Ext */
//
// Core String customizations require for boot lib/* to function.
//

/**
 * Add support for String.replaceAll(String,String).
 *
 * @class String
 */
Ext.applyIf(String.prototype, {
    /**
     * @param strTarget
     * @param strSubString
     * @return {String}
     */
    replaceAll: function (strTarget, strSubString) {
        var strText = this, intIndexOfMatch = strText.indexOf(strTarget);

        while (intIndexOfMatch !== -1) {
            strText = strText.replace(strTarget, strSubString);
            intIndexOfMatch = strText.indexOf(strTarget);
        }

        // return new String, because returning "this" here without a #replace inbetween returns a char array,
        // not a String (seen in chrome).
        return String(strText);
    }
});

/**
 * Add support for String.startsWith(String).
 *
 * @class String
 */
Ext.applyIf(String.prototype, {
    /**
     * @param str
     * @return {Boolean}
     */
    startsWith: function (str) {
        return this.indexOf(str) === 0;
    }
});

/**
 * Add support for String.endsWith(String).
 *
 * @class String
 */
Ext.applyIf(String.prototype, {
    /**
     * @param str
     * @return {Boolean}
     */
    endsWith: function (str) {
        return this.slice(-str.length) === str;
    }
});
