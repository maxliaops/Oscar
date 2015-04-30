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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.PackagePermission;
import org.ungoverned.moduleloader.Module;
import org.ungoverned.moduleloader.ModuleEvent;
import org.ungoverned.moduleloader.ModuleListener;
import org.ungoverned.moduleloader.search.ImportSearchPolicy;
import org.ungoverned.moduleloader.search.SelectionPolicy;
import org.ungoverned.moduleloader.search.CompatibilityPolicy;

public class OSGiSelectionPolicy implements SelectionPolicy, ModuleListener
{
    private Oscar m_oscar = null;
    private Map m_resolvedPackageMap = new HashMap();
    private Map m_resolvedModuleMap = new HashMap();

    public OSGiSelectionPolicy(Oscar oscar)
    {
        m_oscar = oscar;
    }

    /**
     * Selects a single module to resolve the specified import
     * from the array of compatible candidate modules. If the import
     * target has not been resolved before, then this selection policy
     * chooses the module that exports the newest version of the
     * import target. If the import target has been resolved already,
     * then the same module that was chosen before is chosen again.
     * This ensures that all modules use the same version of all
     * exported classes, as described in the OSGi specification.
     * @param module the module that is importing the target.
     * @param identifier the identifier of the import target.
     * @param version the version number of the import target.
     * @param candidates array of compatible candidate modules from which to choose.
     * @param compatPolicy the compatibility policy that is being used.
     * @return the selected module or <tt>null</tt> if no module
     *         can be selected.
    **/
    public synchronized Module select(Module module, Object identifier,
        Object v, Module[] candidates, CompatibilityPolicy compatPolicy)
    {
        String pkgName = (String) identifier;
        int[] version = (int[]) v;

        // See if a module was already selected to resolve this package.
        Module selModule = (Module) m_resolvedPackageMap.get(pkgName);

        // If no module was previously selected to resolve the package,
        // then try to choose one now.
        if (selModule == null)
        {
            int[] selVersion = version;
            Bundle selBundle = null;

            // Examine all exported instances of the package and
            // choose the one with the newest version number. If
            // there is more than one source for the newest version,
            // then select the package coming from the bundle with
            // the with the smallest bundle ID.
            for (int i = 0; i < candidates.length; i++)
            {
                // Get the bundle associated with the module.
                long id = BundleInfo.getBundleIdFromModuleId(candidates[i].getId());
                if (id < 0)
                {
                    // Ignore modules for which there is no bundle.
                    continue;
                }

                BundleImpl bundle = (BundleImpl) m_oscar.getBundle(id);

                // The bundle may be uninstalled, so just ignore that case.
                if (bundle == null)
                {
                    continue;
                }

                // Ignore the package if its bundle is not resolved,
                // active, or installed.
                if ((bundle.getState() != Bundle.RESOLVED)
                    && (bundle.getState() != Bundle.ACTIVE)
                    && (bundle.getState() != Bundle.INSTALLED))
                {
                    continue;
                }

                // If the security manager is set, then check if the
                // exporting bundle is allowed to export the package,
                // unless the bundle is the system bundle.
                if ((System.getSecurityManager() != null) && (bundle.getBundleId() != 0))
                {
                    URL url = null;
                    try
                    {
                        url = new URL(bundle.getInfo().getLocation());
                    }
                    catch (MalformedURLException ex)
                    {
                        // For safety, ignore if we can't get its
                        // location URL.
                        continue;
                    }
                    try
                    {
                        AccessController.doPrivileged(
                            new CheckExportPrivileged(url, pkgName));
                    }
                    catch (PrivilegedActionException ex)
                    {
                        // If we are here, then most likely the security
                        // check failed, so ignore this package.
                        continue;
                    }
                }

                int[] tmpVersion = (int[])
                    ImportSearchPolicy.getExportVersion(candidates[i], pkgName);

                // If this is the first match, then just select it.
                if ((selModule == null) &&
                    (compatPolicy.compare(pkgName, tmpVersion, pkgName, selVersion) >= 0))
                {
                    selModule = candidates[i];
                    selVersion = tmpVersion;
                    selBundle = bundle;
                }
                // If the current export package version is greater
                // than the selected export package version, then
                // record it instead.
                else if (compatPolicy.compare(pkgName, tmpVersion, pkgName, selVersion) > 0)
                {
                    selModule = candidates[i];
                    selVersion = tmpVersion;
                    selBundle = bundle;
                }
                // If the current export package version is equal to
                // the selected export package version, but has a lower
                // bundle ID, then record it instead.
                else if ((compatPolicy.compare(pkgName, tmpVersion, pkgName, selVersion) == 0)
                    && (bundle.getBundleId() < selBundle.getBundleId()))
                {
                    selModule = candidates[i];
                    selVersion = tmpVersion;
                    selBundle = bundle;
                }
            }

            m_resolvedPackageMap.put(pkgName, selModule);
            m_resolvedModuleMap.put(selModule, selModule);
        }
        // See if the previously selected export module satisfies
        // the current request, otherwise return null.
        else
        {
            int[] selVersion = (int[])
                ImportSearchPolicy.getExportVersion(selModule, pkgName);
            Module tmpModule = selModule;
            selModule = null;
            if (compatPolicy.isCompatible(pkgName, selVersion, pkgName, version))
            {
                selModule = tmpModule;
            }
        }

        return selModule;
    }

    public void moduleAdded(ModuleEvent event)
    {
    }

    public void moduleReset(ModuleEvent event)
    {
        moduleRemoved(event);
    }

    public synchronized void moduleRemoved(ModuleEvent event)
    {
// TODO: Synchronization?
        // If the module that was removed was chosen for
        // exporting packages, then flush it from our
        // data structures.
        if (m_resolvedModuleMap.get(event.getModule()) != null)
        {
            // Remove from module map.
            m_resolvedModuleMap.remove(event.getModule());
            // Remove each exported package from package map.
            Iterator iter = m_resolvedPackageMap.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry) iter.next();
                if (entry.getValue() == event.getModule())
                {
                    iter.remove();
                }
            }
        }
    }

    /**
     * This simple class is used to perform the privileged action of
     * checking if a bundle has permission to export a package.
    **/
    private static class CheckExportPrivileged implements PrivilegedExceptionAction
    {
        private URL m_url = null;
        private String m_pkgName = null;

        public CheckExportPrivileged(URL url, String pkgName)
        {
            m_url = url;
            m_pkgName = pkgName;
        }

        public Object run() throws Exception
        {
            // Get permission collection for code source; we cannot
            // call AccessController.checkPermission() directly since
            // the bundle's code is not on the access context yet and
            // might never be if it is only a library bundle, for example.
            CodeSource cs = new CodeSource(m_url, (Certificate[]) null);
            PermissionCollection pc = Policy.getPolicy().getPermissions(cs);
            PackagePermission perm = new PackagePermission(
                m_pkgName, PackagePermission.EXPORT);
            if (!pc.implies(perm))
            {
                throw new AccessControlException("access denied " + perm);
            }

            return null;
        }
    }
}