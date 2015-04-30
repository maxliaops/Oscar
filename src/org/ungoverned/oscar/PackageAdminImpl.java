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
import org.osgi.service.packageadmin.PackageAdmin;

class PackageAdminImpl implements PackageAdmin, Runnable
{
    private Oscar m_oscar = null;
    private Bundle[][] m_reqBundles = null;

    public PackageAdminImpl(Oscar oscar)
    {
        m_oscar = oscar;

        // Start a thread to perform asynchronous package refreshes.
        Thread t = new Thread(this, "OscarPackageAdmin");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns the exported package associated with the specified
     * package name.
     *
     * @param name the name of the exported package to find.
     * @return the exported package or null if no matching package was found.
    **/
    public ExportedPackage getExportedPackage(String name)
    {
        return m_oscar.getExportedPackage(name);
    }

    /**
     * Returns the packages exported by the specified bundle.
     *
     * @param bundle the bundle whose exported packages are to be returned.
     * @return an array of packages exported by the bundle or null if the
     *         bundle does not export any packages.
    **/
    public ExportedPackage[] getExportedPackages(Bundle b)
    {
        return m_oscar.getExportedPackages(b);
    }

    /**
     * The OSGi specification states that refreshing packages is
     * asynchronous; this method simply notifies the package admin
     * thread to do a refresh.
     * @param bundles array of bundles to refresh or <tt>null</tt> to refresh
     *                any bundles in need of refreshing.
    **/
    public synchronized void refreshPackages(Bundle[] bundles)
        throws SecurityException
    {
        // Save our request parameters and notify all.
        if (m_reqBundles == null)
        {
            m_reqBundles = new Bundle[][] { bundles };
        }
        else
        {
            Bundle[][] newReqBundles = new Bundle[m_reqBundles.length + 1][];
            System.arraycopy(m_reqBundles, 0,
                newReqBundles, 0, m_reqBundles.length);
            newReqBundles[m_reqBundles.length] = bundles;
            m_reqBundles = newReqBundles;
        }
        notifyAll();
    }

    /**
     * The OSGi specification states that package refreshes happen
     * asynchronously; this is the run() method for the package
     * refreshing thread.
    **/
    public void run()
    {
        // This thread loops forever, thus it should
        // be a daemon thread.
        Bundle[] bundles = null;
        while (true)
        {
            synchronized (this)
            {
                // Wait for a refresh request.
                while (m_reqBundles == null)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException ex)
                    {
                    }
                }

                // Get the bundles parameter for the current
                // refresh request.
                if (m_reqBundles != null)
                {
                    bundles = m_reqBundles[0];
                }
            }

            // Perform refresh.
            m_oscar.refreshPackages(bundles);

            // Remove the first request since it is now completed.
            synchronized (this)
            {
                if (m_reqBundles.length == 1)
                {
                    m_reqBundles = null;
                }
                else
                {
                    Bundle[][] newReqBundles = new Bundle[m_reqBundles.length - 1][];
                    System.arraycopy(m_reqBundles, 1,
                        newReqBundles, 0, m_reqBundles.length - 1);
                    m_reqBundles = newReqBundles;
                }
            }
        }
    }
}