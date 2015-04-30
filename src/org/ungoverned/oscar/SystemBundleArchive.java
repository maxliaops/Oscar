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

import java.io.File;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.ungoverned.oscar.util.OscarConstants;

public class SystemBundleArchive implements BundleArchive
{
    private Map m_headerMap = null;

    public long getId()
    {
        return 0;
    }
    
    public String getLocation()
        throws Exception
    {
        return OscarConstants.SYSTEM_BUNDLE_LOCATION;
    }

    public int getPersistentState()
        throws Exception
    {
        return Bundle.ACTIVE;
    }

    public void setPersistentState(int state)
        throws Exception
    {
    }

    public int getStartLevel()
        throws Exception
    {
        return OscarConstants.SYSTEMBUNDLE_DEFAULT_STARTLEVEL;
    }

    public void setStartLevel(int level)
        throws Exception
    {
    }

    public File getDataFile(String fileName)
        throws Exception
    {
        return null;
    }

    public BundleActivator getActivator(ClassLoader loader)
        throws Exception
    {
        return null;
    }

    public void setActivator(Object obj)
        throws Exception
    {
    }

    public int getRevisionCount()
        throws Exception
    {
        return 1;
    }

    public Map getManifestHeader(int revision)
        throws Exception
    {
        return m_headerMap;
    }
    
    protected void setManifestHeader(Map headerMap)
    {
        m_headerMap = headerMap;
    }

    public String[] getClassPath(int revision)
        throws Exception
    {
        return null;
    }

    public String findLibrary(int revision, String libName)
        throws Exception
    {
        return null;
    }
}