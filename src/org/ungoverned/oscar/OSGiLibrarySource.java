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

import org.osgi.framework.Constants;
import org.ungoverned.moduleloader.LibrarySource;

public class OSGiLibrarySource implements LibrarySource
{
    private boolean m_opened = false;
    private BundleCache m_cache = null;
    private long m_bundleId = -1;
    private int m_revision = -1;
    private String m_os = null;
    private String m_processor = null;
    private LibraryInfo[] m_libraries = null;

    public OSGiLibrarySource(
        BundleCache cache, long bundleId, int revision,
        String os, String processor, LibraryInfo[] libraries)
    {
        m_cache = cache;
        m_bundleId = bundleId;
        m_revision = revision;
        m_os = normalizePropertyValue(Constants.FRAMEWORK_OS_NAME, os);
        m_processor = normalizePropertyValue(Constants.FRAMEWORK_PROCESSOR, processor);
        m_libraries = libraries;
    }

    public void open()
    {
        m_opened = true;
    }

    public void close()
    {
        m_opened = false;
    }

    public String getPath(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("OSGiLibrarySource is not open");
        }

        if (m_libraries != null)
        {
            String libname = System.mapLibraryName(name);

            // Check to see if we have a matching library.
            // TODO: This "matching" algorithm does not fully
            // match the spec and should be improved.
            LibraryInfo library = null;
            for (int i = 0; (library == null) && (i < m_libraries.length); i++)
            {
                boolean osOkay = checkOS(m_libraries[i].getOSNames());
                boolean procOkay = checkProcessor(m_libraries[i].getProcessors());
                if (m_libraries[i].getName().endsWith(libname) && osOkay && procOkay)
                {
                    library = m_libraries[i];
                }
            }

            if (library != null)
            {
                try {
                    return m_cache.getArchive(m_bundleId)
                        .findLibrary(m_revision, library.getName());
                } catch (Exception ex) {
                    Oscar.error("OSGiLibrarySource: Error finding library.", ex);
                }
            }
        }

        return null;
    }

    private boolean checkOS(String[] osnames)
    {
        for (int i = 0; (osnames != null) && (i < osnames.length); i++)
        {
            String osname =
                normalizePropertyValue(Constants.FRAMEWORK_OS_NAME, osnames[i]);
            if (m_os.equals(osname))
            {
                return true;
            }
        }
        return false;
    }

    private boolean checkProcessor(String[] processors)
    {
        for (int i = 0; (processors != null) && (i < processors.length); i++)
        {
            String processor =
                normalizePropertyValue(Constants.FRAMEWORK_PROCESSOR, processors[i]);
            if (m_processor.equals(processor))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * This is simply a hack to try to create some standardized
     * property values, since there seems to be many possible
     * values for each JVM implementation.  Currently, this
     * focuses on Windows and Linux and will certainly need
     * to be changed in the future or at least edited.
    **/
    private String normalizePropertyValue(String prop, String value)
    {
        prop = prop.toLowerCase();
        value = value.toLowerCase();

        if (prop.equals(Constants.FRAMEWORK_OS_NAME))
        {
            if (value.startsWith("linux"))
            {
                return "linux";
            }
            else if (value.startsWith("win"))
            {
                String os = "win";
                if (value.indexOf("95") >= 0)
                {
                    os = "win95";
                }
                else if (value.indexOf("98") >= 0)
                {
                    os = "win98";
                }
                else if (value.indexOf("NT") >= 0)
                {
                    os = "winnt";
                }
                else if (value.indexOf("2000") >= 0)
                {
                    os = "win2000";
                }
                else if (value.indexOf("xp") >= 0)
                {
                    os = "winxp";
                }
                return os;
            }
        }
        else if (prop.equals(Constants.FRAMEWORK_PROCESSOR))
        {
            if (value.endsWith("86"))
            {
                return "x86";
            }
        }

        return value;
    }
}