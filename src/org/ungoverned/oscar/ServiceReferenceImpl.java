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
import org.osgi.framework.ServiceReference;

class ServiceReferenceImpl implements ServiceReference
{
    private ServiceRegistrationImpl m_registration = null;
    private Bundle m_bundle = null;

    public ServiceReferenceImpl(ServiceRegistrationImpl reg, Bundle bundle)
    {
        m_registration = reg;
        m_bundle = bundle;
    }

    protected ServiceRegistrationImpl getServiceRegistration()
    {
        return m_registration;
    }

    public Object getProperty(String s)
    {
        Oscar.debug("ServiceReference.getProperty(\"" + s + "\")");
        return m_registration.getProperty(s);
    }

    public String[] getPropertyKeys()
    {
        Oscar.debug("ServiceReference.getPropertyKeys()");
        return m_registration.getPropertyKeys();
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public Bundle[] getUsingBundles()
    {
        return m_registration.getUsingBundles();
    }

    public boolean equals(Object obj)
    {
        try	{
            ServiceReferenceImpl ref = (ServiceReferenceImpl) obj;
            return ref.m_registration == m_registration;
        } catch (ClassCastException ex)	{
            // Ignore and return false.
        } catch (NullPointerException ex) {
            // Ignore and return false.
        }

        return false;
    }

    public int hashCode()
    {
        if (m_registration.getReference() != null)
        {
            if (m_registration.getReference() != this)
                return m_registration.getReference().hashCode();
            return super.hashCode();
        }
        return 0;
    }

    public String toString()
    {
        String[] ocs = (String[]) getProperty("objectClass");
        String oc = "[";
        for(int i = 0; i < ocs.length; i++)
        {
            oc = oc + ocs[i];
            if (i < ocs.length - 1)
                oc = oc + ", ";
        }
        oc = oc + "]";
        return oc;
    }
}