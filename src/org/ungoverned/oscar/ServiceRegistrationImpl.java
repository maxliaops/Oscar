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

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.*;
import org.ungoverned.oscar.util.CaseInsensitiveMap;

class ServiceRegistrationImpl implements ServiceRegistration
{
    // Oscar framework.
    private Oscar m_oscar = null;
    // Bundle implementing the service.
    private BundleImpl m_bundle = null;
    // Interfaces associated with the service object.
    private String[] m_classes = null;
    // Service Id associated with the service object.
    private Long m_serviceId = null;
    // Service object.
    private Object m_svcObj = null;
    // Service factory interface.
    private ServiceFactory m_factory = null;
    // Associated property dictionary.
    private Map m_propMap = null;
    // Re-usable service reference.
    private ServiceReferenceImpl m_ref = null;

    public ServiceRegistrationImpl(
        Oscar oscar, BundleImpl bundle,
        String[] classes, Long serviceId,
        Object svcObj, Dictionary dict)
    {
        m_oscar = oscar;
        m_bundle = bundle;
        m_classes = classes;
        m_serviceId = serviceId;
        m_svcObj = svcObj;
        m_factory = (m_svcObj instanceof ServiceFactory)
            ? (ServiceFactory) m_svcObj : null;

        initializeProperties(dict);

        // This reference is the "standard" reference for this
        // service and will always be returned by getReference().
        // Since all reference to this service are supposed to
        // be equal, we use the hashcode of this reference for
        // a references to this service in ServiceReference.
        m_ref = new ServiceReferenceImpl(this, m_bundle);
    }

    protected boolean isValid()
    {
        return (m_svcObj != null);
    }

    public ServiceReference getReference()
    {
        return m_ref;
    }

    public void setProperties(Dictionary dict)
    {
        // Make sure registration is valid.
        if (!isValid())
        {
            throw new IllegalStateException(
                "The service registration is no longer valid.");
        }
        // Set the properties.
        initializeProperties(dict);
        // Tell Oscar about it.
        m_oscar.servicePropertiesModified(this);
    }

    private void initializeProperties(Dictionary dict)
    {
        // Create a case insensitive map.
        if (m_propMap == null)
        {
            m_propMap = new CaseInsensitiveMap();
        }
        else
        {
            m_propMap.clear();
        }

        if (dict != null)
        {
            Enumeration keys = dict.keys();
            while (keys.hasMoreElements())
            {
                Object key = keys.nextElement();
                m_propMap.put(key, dict.get(key));
            }
        }

        // Add the framework assigned properties.
        m_propMap.put(Constants.OBJECTCLASS, m_classes);
        m_propMap.put(Constants.SERVICE_ID, m_serviceId);
    }

    public void unregister()
    {
        Oscar.debug("ServiceRegistration.unregister()");
        m_oscar.unregisterService(m_bundle, this);
        m_svcObj = null;
        m_factory = null;
    }

    /*
     * Utility methods.
     */

    protected Object getService(Bundle acqBundle)
    {
        // If the service object is a service factory, then
        // let it create the service object.
        if (m_factory != null)
        {
            try {
                if (System.getSecurityManager() != null)
                {
                    return AccessController.doPrivileged(
                        new ServiceFactoryPrivileged(acqBundle, null));
                }
                else
                {
                    return getFactoryUnchecked(acqBundle);
                }
            } catch (Exception ex) {
                Oscar.error("ServiceRegistrationImpl: Error getting service.", ex);
                return null;
            }
        }
        else
        {
            return m_svcObj;
        }
    }

    protected void ungetService(Bundle relBundle, Object svcObj)
    {
        // If the service object is a service factory, then
        // let is release the service object.
        if (m_factory != null)
        {
            try {
                if (System.getSecurityManager() != null)
                {
                    AccessController.doPrivileged(
                        new ServiceFactoryPrivileged(relBundle, svcObj));
                }
                else
                {
                    ungetFactoryUnchecked(relBundle, svcObj);
                }
            } catch (Exception ex) {
                Oscar.error("ServiceRegistrationImpl: Error ungetting service.", ex);
            }
        }
    }

    protected Bundle[] getUsingBundles()
    {
        return m_oscar.getUsingBundles(m_ref);
    }

    protected Object getProperty(String key)
    {
        return m_propMap.get(key);
    }

    private transient ArrayList m_list = new ArrayList();

    protected String[] getPropertyKeys()
    {
        synchronized (m_list)
        {
            m_list.clear();
            Iterator i = m_propMap.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry entry = (Map.Entry) i.next();
                m_list.add(entry.getKey());
            }
            return (String[]) m_list.toArray(new String[m_list.size()]);
        }
    }

    private Object getFactoryUnchecked(Bundle bundle)
    {
        return m_factory.getService(bundle, this);
    }

    private void ungetFactoryUnchecked(Bundle bundle, Object svcObj)
    {
        m_factory.ungetService(bundle, this, svcObj);
    }

    /**
     * This simple class is used to ensure that when a service factory
     * is called, that no other classes on the call stack interferes
     * with the permissions of the factory itself.
    **/
    private class ServiceFactoryPrivileged implements PrivilegedExceptionAction
    {
        private Bundle m_bundle = null;
        private Object m_svcObj = null;

        public ServiceFactoryPrivileged(Bundle bundle, Object svcObj)
        {
            m_bundle = bundle;
            m_svcObj = svcObj;
        }

        public Object run() throws Exception
        {
            if (m_svcObj == null)
            {
                return getFactoryUnchecked(m_bundle);
            }
            else
            {
                ungetFactoryUnchecked(m_bundle, m_svcObj);
            }
            return null;
        }
    }
}