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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import org.osgi.framework.*;
import org.ungoverned.moduleloader.LibrarySource;
import org.ungoverned.moduleloader.ResourceSource;
import org.ungoverned.moduleloader.SystemResourceSource;
import org.ungoverned.moduleloader.search.ImportSearchPolicy;
import org.ungoverned.oscar.util.CaseInsensitiveMap;
import org.ungoverned.oscar.util.OscarConstants;

class SystemBundle extends BundleImpl
{
    private List m_activatorList = null;
    private BundleActivator m_activator = null;
    private Thread m_shutdownThread = null;
    private Object[][] m_attributes = null;
    private ResourceSource[] m_resSources = null;
    private LibrarySource[] m_libSources = null;

    protected SystemBundle(Oscar oscar, BundleInfo info, List activatorList)
        throws BundleException
    {
        super(oscar, info);

        // Create an activator list if necessary.
        if (activatorList == null)
        {
            activatorList = new ArrayList();
        }

        // Add the bundle activator for the package admin service.
        activatorList.add(new PackageAdminActivator(oscar));

        // Add the bundle activator for the start level service.
        activatorList.add(new StartLevelActivator(oscar));

        m_activatorList = activatorList;

        int[] version120 = { 1, 2, 0 };
        int[] version100 = { 1, 0, 0 };
        Object[][] exports = new Object[][] {
            { "org.osgi.framework", version120, null },
            { "org.osgi.service.packageadmin", version120, null },
            { "org.osgi.service.startlevel", version100, null }
        };
        m_attributes = new Object[][] {
            new Object[] { "exports", exports },
            new Object[] { "imports", new Object[0][0] },
            new Object[] { "propagates", new Object[0] }
        };
        m_resSources = new ResourceSource[] {
            new SystemResourceSource()
        };
        m_libSources = null;

        String exportString = "";
        for (int i = 0; i < exports.length; i++)
        {
            if (i < (exports.length - 1))
            {
                exportString = exportString +
                    exports[i][ImportSearchPolicy.IDENTIFIER_IDX]
                    + "; specification-version=\""
                    + ((int[])exports[i][ImportSearchPolicy.VERSION_IDX])[0] + "."
                    + ((int[])exports[i][ImportSearchPolicy.VERSION_IDX])[1] + "."
                    + ((int[])exports[i][ImportSearchPolicy.VERSION_IDX])[2] + "\", ";
            }
            else
            {
                exportString = exportString +
                    exports[i][ImportSearchPolicy.IDENTIFIER_IDX]
                    + "; specification-version=\""
                    + ((int[])exports[i][ImportSearchPolicy.VERSION_IDX])[0] + "."
                    + ((int[])exports[i][ImportSearchPolicy.VERSION_IDX])[1] + "."
                    + ((int[])exports[i][ImportSearchPolicy.VERSION_IDX])[2] + "\"";
            }
        }

        // Initialize header map as a case insensitive map.
        Map map = new CaseInsensitiveMap();
        map.put(OscarConstants.BUNDLE_VERSION, OscarConstants.OSCAR_VERSION_VALUE);
        map.put(OscarConstants.BUNDLE_NAME, "System Bundle");
        map.put(OscarConstants.BUNDLE_DESCRIPTION,
            "This bundle is system specific; it implements various system services.");
        map.put(OscarConstants.EXPORT_PACKAGE, exportString);
        ((SystemBundleArchive) getInfo().getArchive()).setManifestHeader(map);
    }

    public Object[][] getAttributes()
    {
        return m_attributes;
    }

    public ResourceSource[] getResourceSources()
    {
        return m_resSources;
    }

    public LibrarySource[] getLibrarySources()
    {
        return m_libSources;
    }

    public synchronized void start() throws BundleException
    {
        Oscar.debug("SystemBundle.start()");

        // The system bundle is only started once and it
        // is started by Oscar.
        if (getState() == Bundle.ACTIVE)
        {
            throw new BundleException("Cannot start the system bundle.");
        }

        getInfo().setState(Bundle.STARTING);

        try {
            getInfo().setContext(new BundleContextImpl(getOscar(), this));
            getActivator().start(getInfo().getContext());
        } catch (Throwable throwable) {
throwable.printStackTrace();
            throw new BundleException(
                "Unable to start system bundle.", throwable);
        }

        // Do NOT set the system bundle state to active yet, this
        // must be done after all other bundles have been restarted.
        // This will be done in the Oscar.initialize() method.
    }

    public synchronized void stop() throws BundleException
    {
        Oscar.debug("SystemBundle.stop()");

        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(new AdminPermission());
        }

        // Spec says stop() on SystemBundle should return immediately and
        // shutdown framework on another thread.
        if (getOscar().getFrameworkStatus() == Oscar.RUNNING_STATUS)
        {
            // Initial call of stop, so kick off shutdown.
            m_shutdownThread = new Thread("OscarShutdown") {
                public void run()
                {
                    try {
                        getOscar().shutdown();
                    } catch (Exception ex) {
                        Oscar.error("SystemBundle: Error while shutting down.");
                        Oscar.error("SystemBundle: " + ex);
                    }

                    // Only shutdown the JVM if Oscar is running stand-alone.
                    String embedded = getOscar().getConfigProperty(
                        OscarConstants.EMBEDDED_EXECUTION_PROP);
                    boolean isEmbedded = (embedded == null)
                        ? false : embedded.equals("true");
                    if (!isEmbedded)
                    {
                        if (System.getSecurityManager() != null)
                        {
                            AccessController.doPrivileged(new PrivilegedAction() {
                                public Object run()
                                {
                                    System.exit(0);
                                    return null;
                                }
                            });
                        }
                        else
                        {
                            System.exit(0);
                        }
                    }
                }
            };
            getInfo().setState(Bundle.STOPPING);
            m_shutdownThread.start();
        }
        else if ((getOscar().getFrameworkStatus() == Oscar.STOPPING_STATUS) &&
                 (Thread.currentThread() == m_shutdownThread))
        {
            // Callback from shutdown thread, so do our own stop.
            try {
                getActivator().stop(getInfo().getContext());
            } catch (Throwable throwable) {
                throw new BundleException(
                        "Unable to stop system bundle.", throwable);
            }
        }
    }

    public synchronized void uninstall() throws BundleException
    {
        Oscar.debug("SystemBundle.uninstall()");
        throw new BundleException("Cannot uninstall the system bundle.");
    }

    public synchronized void update() throws BundleException
    {
        update(null);
    }

    public synchronized void update(InputStream is) throws BundleException
    {
        Oscar.debug("SystemBundle.update()");

        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(new AdminPermission());
        }

        // TODO: This is supposed to stop and then restart the framework.
        throw new BundleException("System bundle update not implemented yet.");
    }

    protected BundleActivator getActivator()
        throws Exception
    {
        if (m_activator == null)
        {
            m_activator = new SystemBundleActivator(getOscar(), m_activatorList);
        }
        return m_activator;
    }
}
