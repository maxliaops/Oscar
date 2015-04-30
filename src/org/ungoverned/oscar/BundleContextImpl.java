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
import java.io.InputStream;
import java.util.Dictionary;

import org.osgi.framework.*;

class BundleContextImpl implements BundleContext
{
    private Oscar m_oscar = null;
    private BundleImpl m_bundle = null;

    protected BundleContextImpl(Oscar oscar, BundleImpl bundle)
    {
        m_oscar = oscar;
        m_bundle = bundle;
    }

    public String getProperty(String name)
    {
        return m_oscar.getProperty(name);
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public Filter createFilter(String expr)
        throws InvalidSyntaxException
    {
        return new FilterImpl(expr);
    }

    public Bundle installBundle(String location)
        throws BundleException
    {
        return installBundle(location, null);
    }

    public Bundle installBundle(String location, InputStream is)
        throws BundleException
    {
        return m_oscar.installBundle(location, is);
    }

    public Bundle getBundle(long id)
    {
        return m_oscar.getBundle(id);
    }

    public Bundle[] getBundles()
    {
        return m_oscar.getBundles();
    }

    public void addBundleListener(BundleListener l)
    {
        m_oscar.addBundleListener(m_bundle, l);
    }

    public void removeBundleListener(BundleListener l)
    {
        m_oscar.removeBundleListener(l);
    }

    public void addServiceListener(ServiceListener l)
    {
        try {
            addServiceListener(l, null);
        } catch (InvalidSyntaxException ex) {
            // This will not happen since the filter is null.
        }
    }

    public void addServiceListener(ServiceListener l, String s)
        throws InvalidSyntaxException
    {
        Oscar.debug("BundleContext.addServiceListener(l, \"" + s + "\")");
        m_oscar.addServiceListener(m_bundle, l, s);
    }

    public void removeServiceListener(ServiceListener l)
    {
        m_oscar.removeServiceListener(l);
    }

    public void addFrameworkListener(FrameworkListener l)
    {
        m_oscar.addFrameworkListener(m_bundle, l);
    }

    public void removeFrameworkListener(FrameworkListener l)
    {
        m_oscar.removeFrameworkListener(l);
    }

    public ServiceRegistration registerService(
        String clazz, Object svcObj, Dictionary dict)
    {
        return registerService(new String[] { clazz }, svcObj, dict);
    }

    public ServiceRegistration registerService(
        String[] clazzes, Object svcObj, Dictionary dict)
    {
        return m_oscar.registerService(m_bundle, clazzes, svcObj, dict);
    }

    public ServiceReference getServiceReference(String clazz)
    {
        Oscar.debug("BundleContext.getServiceReference()");
        try {
            ServiceReference[] refs = getServiceReferences(clazz, null);
            return getBestServiceReference(refs);
        } catch (InvalidSyntaxException ex) {
            Oscar.error("BundleContextImpl: " + ex);
        }
        return null;
    }

    private ServiceReference getBestServiceReference(ServiceReference[] refs)
    {
        if (refs == null)
        {
            return null;
        }

        if (refs.length == 1)
        {
            return refs[0];
        }

        // Loop through all service references and return
        // the "best" one according to its rank and ID.
        ServiceReference bestRef = null;
        Integer bestRank = null;
        Long bestId = null;
        for (int i = 0; i < refs.length; i++)
        {
            ServiceReference ref = refs[i];

            // The first time through the loop just
            // assume that the first reference is best.
            if (bestRef == null)
            {
                bestRef = ref;
                bestRank = (Integer) bestRef.getProperty("service.ranking");
                // The spec says no ranking defaults to zero.
                if (bestRank == null)
                {
                    bestRank = new Integer(0);
                }
                bestId = (Long) bestRef.getProperty("service.id");
            }

            // Compare current and best references to see if
            // the current reference is a better choice.
            Integer rank = (Integer) ref.getProperty("service.ranking");

            // The spec says no ranking defaults to zero.
            if (rank == null)
            {
                rank = new Integer(0);
            }

            // If the current reference ranking is greater than the
            // best ranking, then keep the current reference.
            if (bestRank.compareTo(rank) < 0)
            {
                bestRef = ref;
                bestRank = rank;
                bestId = (Long) bestRef.getProperty("service.id");
            }
            // If rankings are equal, then compare IDs and
            // keep the smallest.
            else if (bestRank.compareTo(rank) == 0)
            {
                Long id = (Long) ref.getProperty("service.id");
                // If either reference has a null ID, then keep
                // the one with a non-null ID.
                if ((bestId == null) || (id == null))
                {
                    bestRef = (bestId == null) ? ref : bestRef;
                    // bestRank = bestRank; // No need to update since they are equal.
                    bestId = (Long) bestRef.getProperty("service.id");
                }
                // Otherwise compare IDs.
                else
                {
                    // If the current reference ID is less than the
                    // best ID, then keep the current reference.
                    if (bestId.compareTo(id) > 0)
                    {
                        bestRef = ref;
                        // bestRank = bestRank; // No need to update since they are equal.
                        bestId = (Long) bestRef.getProperty("service.id");
                    }
                }
            }
        }

        return bestRef;
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter)
        throws InvalidSyntaxException
    {
        Oscar.debug("BundleContext.getServiceReferences()");
        return m_oscar.getServiceReferences(clazz, filter);
    }

    public Object getService(ServiceReference ref)
    {
        if (ref == null)
        {
            throw new NullPointerException("Specified service reference cannot be null.");
        }
        return m_oscar.getService(m_bundle, ref);
    }

    public boolean ungetService(ServiceReference ref)
    {
        if (ref == null)
        {
            throw new NullPointerException("Specified service reference cannot be null.");
        }

        // Unget the specified service.
        Object svcObj = m_oscar.ungetService(m_bundle, ref);

        // Return false if no service object was found.
        return (svcObj != null);
    }

    public File getDataFile(String s)
    {
        Oscar.debug("BundleContext.getDataFile()");
        return m_oscar.getBundleDataFile(m_bundle, s);
    }
}