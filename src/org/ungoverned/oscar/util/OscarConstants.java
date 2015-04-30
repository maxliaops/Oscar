/*
 * Oscar - An implementation of the OSGi framework.
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.ungoverned.oscar.util;

public interface OscarConstants extends org.osgi.framework.Constants
{
    // Framework constants and values.
    public static final String FRAMEWORK_VERSION_VALUE = "3.0";
    public static final String FRAMEWORK_VENDOR_VALUE = "Oscar";

    // Oscar constants and values.
    public static final String OSCAR_VERSION_PROPERTY = "oscar.version";
    public static final String OSCAR_VERSION_VALUE = "1.0.5";

    // Miscellaneous manifest constants.
    public static final String CLASS_PATH_SEPARATOR = ",";
    public static final String CLASS_PATH_DOT = ".";
    public static final String PACKAGE_SEPARATOR = ";";
    public static final String PACKAGE_VERSION_TOKEN = "specification-version";
    public static final String VERSION_SEGMENT_SEPARATOR = ".";
    public static final int VERSION_SEGMENT_COUNT = 3;

    // Miscellaneous OSGi constants.
    public static final String BUNDLE_URL_PROTOCOL = "bundle";

    // Miscellaneous Oscar system property names.
    public static final String SYSTEM_PROPERTIES_PROP = "oscar.system.properties";
    public static final String BUNDLE_PROPERTIES_PROP = "oscar.bundle.properties";
    public static final String AUTO_INSTALL_PROP = "oscar.auto.install";
    public static final String AUTO_START_PROP = "oscar.auto.start";
    public static final String EMBEDDED_EXECUTION_PROP = "oscar.embedded.execution";
    public static final String STRICT_OSGI_PROP = "oscar.strict.osgi";
    public static final String CACHE_CLASS_PROP = "oscar.cache.class";
    public static final String FRAMEWORK_STARTLEVEL_PROP
        = "oscar.startlevel.framework";
    public static final String BUNDLE_STARTLEVEL_PROP
        = "oscar.startlevel.bundle";

    // Start level-related constants.
    public static final int FRAMEWORK_INACTIVE_STARTLEVEL = 0;
    public static final int FRAMEWORK_DEFAULT_STARTLEVEL = 1;
    public static final int SYSTEMBUNDLE_DEFAULT_STARTLEVEL = 0;
    public static final int BUNDLE_DEFAULT_STARTLEVEL = 1;

    // Miscellaneous properties values.
    public static final String SYSTEM_PROPERTY_FILE_VALUE = "system.properties";
    public static final String BUNDLE_PROPERTY_FILE_VALUE = "bundle.properties";
    public static final String FAKE_URL_PROTOCOL_VALUE = "location:";
    public static final String DEFAULT_DOMAIN_VALUE = "localdomain";
    public static final String DEFAULT_HOSTNAME_VALUE = "localhost";
}
