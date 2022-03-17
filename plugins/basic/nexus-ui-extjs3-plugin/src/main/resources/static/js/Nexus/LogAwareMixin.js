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
/*global define,NX, Nexus*/

/**
 * Mixin to make classes logging aware.
 *
 * @since 2.4
 */
NX.define('Nexus.LogAwareMixin', {

    statics: {
        /**
         * True to include class-names in log message (default); false to omit it.
         *
         * @property
         */
        includeName: true,

        /**
         * True to use simple class-names (default); false to use full class-names.
         *
         * @property
         */
        simpleName: true,

        /**
         * True to include level in log message (default); false to omit it.
         *
         * @property
         */
        includeLevel: true
    },

    /**
     * @private
     *
     * @param {String} level
     * @param {Array} args
     */
    logFormat: function (level, args) {
        var name,
            config = Nexus.LogAwareMixin; // config pulled from static properties

        // maybe prepend class-name
        if (config.includeName === true) {
            name = this.$className;
            if (config.simpleName === true) {
                name = this.$simpleClassName;
            }
            args.unshift(name + ':');
        }

        // maybe prepend level
        if (config.includeLevel === true) {
            args.unshift('[' + level.toUpperCase() + ']');
        }

        return args;
    },

    /**
     * @private
     *
     * @param {String} level
     * @param {Array} args
     */
    logx: function (level, args) {
        var fn;

        NX.assert(NX.log.levels[level] !== undefined, 'Invalid log level: ' + level);
        NX.assert(args.length !== 0, 'Missing log message detail');

        args = this.logFormat(level, args);

        fn = NX.log[level];
        fn.apply(NX.log, args);
    },

    /**
     * @protected
     */
    logDebug: function () {
        this.logx('debug', Array.prototype.slice.call(arguments));
    },

    /**
     * @protected
     */
    logInfo: function () {
        this.logx('info', Array.prototype.slice.call(arguments));
    },

    /**
     * @protected
     */
    logWarn: function () {
        this.logx('warn', Array.prototype.slice.call(arguments));
    },

    /**
     * @protected
     */
    logError: function () {
        this.logx('error', Array.prototype.slice.call(arguments));
    },

    /**
     * @protected
     */
    logGroup: function () {
        NX.log.group.apply(NX.log, this.logFormat('group', Array.prototype.slice.call(arguments)));
    },

    /**
     * @protected
     */
    logGroupEnd: function () {
        NX.log.groupEnd();
    }

});
