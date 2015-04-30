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

import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

class BundleImpl implements Bundle
{
    private Oscar m_oscar = null;
    private BundleInfo m_info = null;

    protected BundleImpl(Oscar oscar, BundleInfo info)
    {
        m_oscar = oscar;
        m_info = info;
    }

    Oscar getOscar() // package protected
    {
        return m_oscar;
    }

    BundleInfo getInfo() // package protected
    {
        return m_info;
    }

    void setInfo(BundleInfo info) // package protected
    {
        m_info = info;
    }

    public long getBundleId()
    {
        return m_oscar.getBundleId(this);
    }

    public Dictionary getHeaders()
    {
        return m_oscar.getBundleHeaders(this);
    }

    public String getLocation()
    {
        return m_oscar.getBundleLocation(this);
    }

    /**
     * Returns a URL to a named resource in the bundle.
     *
     * @return a URL to named resource, or null if not found.
    **/
    public URL getResource(String name)
    {
        return m_oscar.getBundleResource(this, name);
    }

    /**
     * Returns an array of service references corresponding to
     * the bundle's registered services.
     *
     * @return an array of service references or null.
    **/
    public ServiceReference[] getRegisteredServices()
    {
        Oscar.debug("BundleImpl.getRegisteredServices()");
        return m_oscar.getBundleRegisteredServices(this);
    }

    public ServiceReference[] getServicesInUse()
    {
        Oscar.debug("BundleImpl.getServicesInUse()");
        return m_oscar.getBundleServicesInUse(this);
    }

    public int getState()
    {
        return m_oscar.getBundleState(this);
    }

    public boolean hasPermission(Object obj)
    {
        return m_oscar.bundleHasPermission(this, obj);
    }

    public void start() throws BundleException
    {
        Oscar.debug("BundleImpl.start() for " + getInfo().getLocation());
        m_oscar.startBundle(this);
    }

    public void update() throws BundleException
    {
        update(null);
    }

    public void update(InputStream is) throws BundleException
    {
        Oscar.debug("BundleImpl.update() for " + getInfo().getLocation());
        m_oscar.updateBundle(this, is);
    }

    public void stop() throws BundleException
    {
        Oscar.debug("BundleImpl.stop() for " + getInfo().getLocation());
        m_oscar.stopBundle(this);
    }

    public void uninstall() throws BundleException
    {
        Oscar.debug("BundleImpl.uninstall() for " + getInfo().getLocation());
        m_oscar.uninstallBundle(this);
    }

    public String toString()
    {
        return "[" + getBundleId() +"]";
    }
}