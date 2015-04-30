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
package org.ungoverned.oscar;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.osgi.framework.Constants;

public class LibraryInfo
{
    private String m_name = null;
    private String[] m_osnames = null;
    private String[] m_osversions = null;
    private String[] m_processors = null;
    private String[] m_languages = null;

    public LibraryInfo(String name, String[] osnames, String[] osversions,
        String[] processors, String[] languages)
    {
        m_name = name;
        m_osnames = osnames;
        m_osversions = osversions;
        m_processors = processors;
        m_languages = languages;
    }

    public LibraryInfo(LibraryInfo library)
    {
        m_name = library.m_name;
        m_osnames = library.m_osnames;
        m_osversions = library.m_osversions;
        m_processors = library.m_processors;
        m_languages = library.m_languages;
    }

    public String getName()
    {
        return m_name;
    }

    public String[] getOSNames()
    {
        return m_osnames;
    }

    public String[] getOSVersions()
    {
        return m_osversions;
    }

    public String[] getProcessors()
    {
        return m_processors;
    }

    public static LibraryInfo[] parse(String s)
    {
        try
        {
            if ((s == null) || (s.length() == 0))
            {
                return null;
            }

            // The tokens are separated by semicolons and may include
            // any number of libraries (whose name starts with a "/")
            // along with one set of associated properties.
            StringTokenizer st = new StringTokenizer(s, ";");
            String[] libs = new String[st.countTokens()];
            List osNameList = new ArrayList();
            List osVersionList = new ArrayList();
            List processorList = new ArrayList();
            List languageList = new ArrayList();
            int libCount = 0;
            while (st.hasMoreTokens())
            {
                String token = st.nextToken().trim();
                if (token.indexOf('=') < 0)
                {
                    // Remove the slash, if necessary.
                    libs[libCount] = (token.charAt(0) == '/')
                        ? token.substring(1)
                        : token;
                    libCount++;
                }
                else
                {
                    // Check for valid native library properties; defined as
                    // a property name, an equal sign, and a value.
                    StringTokenizer stProp = new StringTokenizer(token, "=");
                    if (stProp.countTokens() != 2)
                    {
                        throw new IllegalArgumentException(
                            "Bundle manifest native library entry malformed: " + token);
                    }
                    String property = stProp.nextToken().trim().toLowerCase();
                    String value = stProp.nextToken().trim();
                    
                    // Values may be quoted, so remove quotes if present.
                    if (value.charAt(0) == '"')
                    {
                        // This should always be true, otherwise the
                        // value wouldn't be properly quoted, but we
                        // will check for safety.
                        if (value.charAt(value.length() - 1) == '"')
                        {
                            value = value.substring(1, value.length() - 1);
                        }
                        else
                        {
                            value = value.substring(1);
                        }
                    }
                    // Add the value to its corresponding property list.
                    if (property.equals(Constants.BUNDLE_NATIVECODE_OSNAME))
                    {
                        osNameList.add(value);
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_OSVERSION))
                    {
                        osVersionList.add(value);
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_PROCESSOR))
                    {
                        processorList.add(value);
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_LANGUAGE))
                    {
                        languageList.add(value);
                    }
                }
            }

            if (libCount == 0)
            {
                return null;
            }

            LibraryInfo[] libraries = new LibraryInfo[libCount];
            for (int i = 0; i < libCount; i++)
            {
                libraries[i] =
                    new LibraryInfo(
                        libs[i],
                        (String[]) osNameList.toArray(new String[0]),
                        (String[]) osVersionList.toArray(new String[0]),
                        (String[]) processorList.toArray(new String[0]),
                        (String[]) languageList.toArray(new String[0]));
            }

            return libraries;

        }
        catch (RuntimeException ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }

    public String toString()
    {
        return m_name;
    }
}
