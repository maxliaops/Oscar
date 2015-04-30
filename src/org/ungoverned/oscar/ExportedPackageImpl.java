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

import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;

class ExportedPackageImpl implements ExportedPackage
{
    private Oscar m_oscar = null;
    private BundleImpl m_exporter = null;
    private String m_name = null;
    private int[] m_version = null;
    private String m_toString = null;
    private String m_versionString = null;

    public ExportedPackageImpl(
        Oscar oscar, BundleImpl exporter, String name, int[] version)
    {
        m_oscar = oscar;
        m_exporter = exporter;
        m_name = name;
        m_version = version;
    }

    public Bundle getExportingBundle()
    {
        // If remove is pending due to a bundle update, then
        // return null per the spec.
        if (m_exporter.getInfo().isRemovalPending())
        {
            return null;
        }
        return m_exporter;
    }

    /**
     * Returns the exporting bundle whether the package is state or
     * not. This is called internally to get access to the exporting
     * bundle during a refresh operation, which is not possible using
     * <tt>getExportingBundle</tt> since the specification says that
     * method must return <tt>null</tt> for stale packages.
     * @return the exporting bundle for the package.
    **/
    protected Bundle getExportingBundleInternal()
    {
        return m_exporter;
    }
    
    public Bundle[] getImportingBundles()
    {
        // If remove is pending due to a bundle update, then
        // return null per the spec.
        if (m_exporter.getInfo().isRemovalPending())
        {
            return null;
        }
        return m_oscar.getImportingBundles(this);
    }

    public String getName()
    {
        return m_name;
    }

    public String getSpecificationVersion()
    {
        if (m_versionString == null)
        {
            if (m_version == null)
            {
                m_versionString = "0.0.0";
            }
            else
            {
                m_versionString =
                    "" + m_version[0] + "." + m_version[1] + "." + m_version[2];
            }
        }
        return m_versionString;
    }

    public boolean isRemovalPending()
    {
        return m_exporter.getInfo().isRemovalPending();
    }

    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_name
                + "; specification-version=" + getSpecificationVersion();
        }
        return m_toString;
    }
}