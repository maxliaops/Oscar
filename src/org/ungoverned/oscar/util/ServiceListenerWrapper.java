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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.*;

public class ServiceListenerWrapper extends ListenerWrapper implements ServiceListener
{
    // LDAP query filter.
    private Filter m_filter = null;
    // Remember the security context.
    private AccessControlContext m_acc = null;

    public ServiceListenerWrapper(Bundle bundle, ServiceListener l, Filter filter)
    {
        super(bundle, ServiceListener.class, l);
        m_filter = filter;

        // Remember security context for filtering
        // events based on security.
        if (System.getSecurityManager() != null)
        {
            m_acc = AccessController.getContext();
        }
    }

    public void setFilter(Filter filter)
    {
        m_filter = filter;
    }
    
    public void serviceChanged(final ServiceEvent event)
    {
        // Service events should be delivered to STARTING,
        // STOPPING, and ACTIVE bundles.
        if ((getBundle().getState() != Bundle.STARTING) &&
            (getBundle().getState() != Bundle.STOPPING) &&
            (getBundle().getState() != Bundle.ACTIVE))
        {
            return;
        }

        // Check that the bundle has permission to get at least
        // one of the service interfaces; the objectClass property
        // of the service stores its service interfaces.
        ServiceReference ref = event.getServiceReference();
        String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);

        // On the safe side, if there is no objectClass property
        // then ignore event altogether.
        if (objectClass != null)
        {
            boolean hasPermission = false;
            if (m_acc != null)
            {
                for (int i = 0;
                    !hasPermission && (i < objectClass.length);
                    i++)
                {
                    try {
                        ServicePermission perm =
                            new ServicePermission(
                                objectClass[i], ServicePermission.GET);
                        m_acc.checkPermission(perm);
                        hasPermission = true;
                    } catch (Exception ex) {
                    }
                }
            }
            else
            {
                hasPermission = true;
            }

            if (hasPermission)
            {
                // Dispatch according to the filter.
                if ((m_filter == null) || m_filter.match(event.getServiceReference()))
                {
                    if (System.getSecurityManager() != null)
                    {
                        AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run()
                            {
                                ((ServiceListener) getListener()).serviceChanged(event);
                                return null;
                            }
                        });
                    }
                    else
                    {
                        ((ServiceListener) getListener()).serviceChanged(event);
                    }
                }
            }
        }
    }
}