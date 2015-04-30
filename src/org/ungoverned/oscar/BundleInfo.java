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

import java.util.*;

import org.osgi.framework.*;
import org.ungoverned.moduleloader.Module;

class BundleInfo
{
    private BundleArchive m_archive = null;
    private Module[] m_modules = null;
    private int m_state = 0;
    private BundleActivator m_activator = null;
    private BundleContext m_context = null;
    // Indicates that the bundle was either updated
    // or uninstalled and is waiting to be removed or refreshed.
    private boolean m_removalPending = false;
    // List of service registrations for this bundle; these are
    // the services that the bundle has registered.
    private List m_svcRegList = null;
    // Maps service references to usage count objects; these are
    // the services used by the bundle.
    private Map m_svcUsageMap = null;

    protected BundleInfo(BundleArchive archive, Module module)
        throws Exception
    {
        m_archive = archive;
        m_modules = (module == null) ? new Module[0] : new Module[] { module };

        m_state = Bundle.INSTALLED;
        m_removalPending = false;
        m_activator = null;
        m_context = null;

        m_svcRegList = new ArrayList();
        m_svcUsageMap = new HashMap();
    }

    /**
     *  Returns the bundle archive associated with this bundle.
     * @return the bundle archive associated with this bundle.
    **/
    public BundleArchive getArchive()
    {
        return m_archive;
    }

    /**
     * Returns an array of all modules associated with the bundle represented by
     * this <tt>BundleInfo</tt> object. A module in the array corresponds to a
     * revision of the bundle's JAR file and is ordered from oldest to newest.
     * Multiple revisions of a bundle JAR file might exist if a bundle is
     * updated, without refreshing the framework. In this case, exports from
     * the prior revisions of the bundle JAR file are still offered; the
     * current revision will be bound to packages from the prior revision,
     * unless the packages were not offered by the prior revision. There is
     * no limit on the potential number of bundle JAR file revisions.
     * @return array of modules corresponding to the bundle JAR file revisions.
    **/
    public Module[] getModules()
    {
        return m_modules;
    }

    /**
     * Determines if the specified module is associated with this bundle.
     * @param module the module to determine if it is associate with this bundle.
     * @return <tt>true</tt> if the specified module is in the array of modules
     *         associated with this bundle, <tt>false</tt> otherwise.
    **/
    public boolean hasModule(Module module)
    {
        for (int i = 0; i < m_modules.length; i++)
        {
            if (m_modules[i] == module)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the newest module, which corresponds to the last module
     * in the module array.
     * @return the newest module.
    **/
    public Module getCurrentModule()
    {
        return m_modules[m_modules.length - 1];
    }
    
    /**
     * Add a module that corresponds to a new bundle JAR file revision for
     * the bundle associated with this <tt>BundleInfo</tt> object.
     * @param module the module to add.
    **/
    public void addModule(Module module)
    {
        Module[] dest = new Module[m_modules.length + 1];
        System.arraycopy(m_modules, 0, dest, 0, m_modules.length);
        dest[m_modules.length] = module;
        m_modules = dest;
    }

    public long getBundleId()
    {
        return m_archive.getId();
    }
    
    public String getLocation()
    {
        try
        {
            return m_archive.getLocation();
        }
        catch (Exception ex)
        {
            Oscar.error("Unable to read location.", ex);
            return null;
        }
    }

    public int getStartLevel(int defaultLevel)
    {
        try
        {
            return m_archive.getStartLevel();
        }
        catch (Exception ex)
        {
            Oscar.error("Unable to read start level.", ex);
            return defaultLevel;
        }
    }

    public void setStartLevel(int i)
    {
        try
        {
            m_archive.setStartLevel(i);
        }
        catch (Exception ex)
        {
            Oscar.error("Error writing to bundle archive.", ex);
        }
    }

    public Map getCurrentHeader()
    {
        try
        {
            // Return the header for the most recent bundle revision only,
            // since we shouldn't ever need access to older revisions.
            return m_archive.getManifestHeader(m_archive.getRevisionCount() - 1);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public int getState()
    {
        return m_state;
    }

    public void setState(int i)
    {
        m_state = i;
    }

    public int getPersistentState()
    {
        try
        {
            return m_archive.getPersistentState();
        }
        catch (Exception ex)
        {
            Oscar.error("Error reading bundle archive.", ex);
            return Bundle.INSTALLED;
        }
    }

    public void setPersistentStateInactive()
    {
        try
        {
            m_archive.setPersistentState(Bundle.INSTALLED);
        }
        catch (Exception ex)
        {
            Oscar.error("Error writing to bundle archive.", ex);
        }
    }

    public void setPersistentStateActive()
    {
        try
        {
            m_archive.setPersistentState(Bundle.ACTIVE);
        }
        catch (Exception ex)
        {
            Oscar.error("Error writing to bundle archive.", ex);
        }
    }

    public void setPersistentStateUninstalled()
    {
        try
        {
            m_archive.setPersistentState(Bundle.UNINSTALLED);
        }
        catch (Exception ex)
        {
            Oscar.error("Error writing to bundle archive.", ex);
        }
    }

    public BundleContext getContext()
    {
        return m_context;
    }

    public void setContext(BundleContext context)
    {
        m_context = context;
    }

    public BundleActivator getActivator()
    {
        return m_activator;
    }

    public void setActivator(BundleActivator activator)
    {
        m_activator = activator;
    }

    public boolean isRemovalPending()
    {
        return m_removalPending;
    }

    public void setRemovalPending()
    {
        m_removalPending = true;
    }

    public int getServiceRegistrationCount()
    {
        return m_svcRegList.size();
    }

    public ServiceRegistrationImpl getServiceRegistration(int i)
    {
        try
        {
            return (ServiceRegistrationImpl) m_svcRegList.get(i);
        }
        catch (IndexOutOfBoundsException exc)
        {
            // Ignore and return null.
        }
        return null;
    }

    public void addServiceRegistration(ServiceRegistrationImpl reg)
    {
        m_svcRegList.add(reg);
    }

    public void removeServiceRegistration(ServiceRegistrationImpl reg)
    {
        m_svcRegList.remove(reg);
    }

// TODO: Would we be better off putting "used services" into a
// list, like registered services above...for consistency?
    public Iterator getServiceUsageCounters()
    {
        return m_svcUsageMap.keySet().iterator();
    }

    public UsageCounter getServiceUsageCounter(ServiceReference ref)
    {
        return (UsageCounter) m_svcUsageMap.get(ref);
    }

    public void putServiceUsageCounter(ServiceReference ref, UsageCounter usage)
    {
        m_svcUsageMap.put(ref, usage);
    }

    public void removeServiceUsageCounter(ServiceReference ref)
    {
        m_svcUsageMap.remove(ref);
    }

    /**
     * Converts a module identifier to a bundle identifier. Module IDs
     * are typically <tt>&lt;bundle-id&gt;.&lt;revision&gt;</tt>; this
     * method returns only the portion corresponding to the bundle ID.
    **/
    protected static long getBundleIdFromModuleId(String id)
    {
        try
        {
            String bundleId = (id.indexOf('.') >= 0)
                ? id.substring(0, id.indexOf('.')) : id;
            return Long.parseLong(bundleId);
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    /**
     * Converts a module identifier to a bundle identifier. Module IDs
     * are typically <tt>&lt;bundle-id&gt;.&lt;revision&gt;</tt>; this
     * method returns only the portion corresponding to the revision.
    **/
    protected static int getModuleRevisionFromModuleId(String id)
    {
        try
        {
            String rev = (id.indexOf('.') >= 0)
                ? id.substring(id.indexOf('.') + 1) : id;
            return Integer.parseInt(rev);
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    static class UsageCounter
    {
        public int m_count = 0;
        public Object m_svcObj = null;
        public Object[] m_svcObjArray = null;
    }
}