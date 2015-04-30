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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.osgi.framework.BundleException;
import org.ungoverned.oscar.LibraryInfo;

public class TextUtil
{
    /**
     * Parses comma delimited string and returns an array containing the tokens.
     * @param value the comma delimited string to parse.
     * @return an array of string tokens or null if there were no tokens.
    **/
    public static String[] parseCommaDelimitedString(String value)
    {
        if (value == null)
        {
           value = "";
        }

        StringTokenizer tok =
            new StringTokenizer(value, OscarConstants.CLASS_PATH_SEPARATOR);

        int count = tok.countTokens();

        if (count == 0)
        {
            return null;
        }

        // Create an array containing the tokens.
        String[] tokens = new String[count];

        for (int i = 0; tok.hasMoreElements(); i++)
        {
            tokens[i] = tok.nextToken().trim();
        }

        return tokens;
    }

    /**
     * Parse package strings from manifest.
    **/
    public static Object[][] parsePackageStrings(String[] packages)
        throws IllegalArgumentException
    {
        if (packages == null)
        {
            return null;
        }

        Object[][] pkgs = new Object[packages.length][3];

        for (int pkgIdx = 0; pkgIdx < packages.length; pkgIdx++)
        {
            StringTokenizer tokPkg =
                new StringTokenizer(packages[pkgIdx], OscarConstants.PACKAGE_SEPARATOR);

            String pkg = tokPkg.nextToken().trim();

            if (pkg.length() == 0)
            {
                throw new IllegalArgumentException("Package name cannot have length of zero.");
            }

            String versionString = tokPkg.hasMoreTokens()
                ? tokPkg.nextToken().trim() : null;
            int[] version = new int[OscarConstants.VERSION_SEGMENT_COUNT];

            if (versionString != null)
            {
                if (!versionString.toLowerCase().startsWith(
                    OscarConstants.PACKAGE_VERSION_TOKEN))
                {
                    throw new IllegalArgumentException("Missing '"
                        + OscarConstants.PACKAGE_VERSION_TOKEN + "' parameter.");
                }

                int idx = versionString.indexOf('=');

                if (idx < 0)
                {
                    throw new IllegalArgumentException(
                        "Version specification missing assignment.");
                }

                versionString = versionString.substring(idx + 1).trim();

                if (versionString.startsWith("\"") && versionString.endsWith("\""))
                {
                    versionString = versionString.substring(1, versionString.length() - 1);
                }

                StringTokenizer tokVersion = new StringTokenizer(versionString,
                    OscarConstants.VERSION_SEGMENT_SEPARATOR);

                if ((tokVersion.countTokens() < 1)
                    || (tokVersion.countTokens() > OscarConstants.VERSION_SEGMENT_COUNT))
                {
                    throw new IllegalArgumentException("Improper version number: "
                        + versionString);
                }

                try
                {
                    version[0] = Integer.parseInt(tokVersion.nextToken());
                    if (tokVersion.hasMoreTokens())
                    {
                        version[1] = Integer.parseInt(tokVersion.nextToken());
                        if (tokVersion.hasMoreTokens())
                        {
                            version[2] = Integer.parseInt(tokVersion.nextToken());
                        }
                    }
                }
                catch (NumberFormatException ex)
                {
                    throw new IllegalArgumentException("Improper version number.");
                }
            }

            pkgs[pkgIdx][0] = pkg;
            pkgs[pkgIdx][1] = version;
        }

        return pkgs;
    }

    /**
     * Parses the <tt>Import-Package</tt> or <tt>Export-Package</tt>
     * manifest header. This routine will throw an exception if the
     * passed in string value is an empty string, but a <tt>null</tt>
     * value is acceptable.
     * @param s the value of the import or export manifest header.
     * @return an array of <tt>Object</tt> arrays, one for each parsed
     *         package, or an empty array if there are no packages.
     * throws org.osgi.framework.BundleException if there is an error
     *        parsing the string.
    **/
    public static Object[][] parseImportExportHeader(String s)
        throws BundleException
    {
        Object[][] pkgs = null;
        if (s != null)
        {
            if (s.length() == 0)
            {
                throw new BundleException(
                    "The import and export headers cannot be an empty string.");
            }
            pkgs = TextUtil.parsePackageStrings(
                TextUtil.parseCommaDelimitedString(s));
        }
        return (pkgs == null) ? new Object[0][3] : pkgs;
    }

    /**
     * Parse native code from manifest.
    **/
    public static LibraryInfo[] parseLibraryStrings(String[] libStrs)
        throws IllegalArgumentException
    {
        if (libStrs == null)
        {
            return null;
        }

        List libList = new ArrayList();

        for (int i = 0; i < libStrs.length; i++)
        {
            LibraryInfo[] libs = LibraryInfo.parse(libStrs[i]);
            for (int libIdx = 0;
                (libs != null) && (libIdx < libs.length);
                libIdx++)
            {
                libList.add(libs[libIdx]);
            }
        }

        if (libList.size() == 0)
        {
            return null;
        }

        return (LibraryInfo[]) libList.toArray(new LibraryInfo[libList.size()]);
    }
}