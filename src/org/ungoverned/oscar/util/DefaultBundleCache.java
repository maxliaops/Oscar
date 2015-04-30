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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

import org.ungoverned.oscar.*;

/**
 * <p>
 * This class, combined with <tt>DefaultBundleArchive</tt>, implements the
 * default file system-based bundle cache for Oscar. It is possible to
 * configure the default behavior of this class by setting system properties
 * or passing properties into Oscar's constructor. The configuration
 * properties for this class are:
 * </p>
 * <ul>
 *   <li><tt>oscar.cache.bufsize</tt> - Sets the buffer size to be used by
 *       the cache; the default value is 4096. The integer
 *       value of this string provides control over the size of the
 *       internal buffer of the disk cache for performance reasons.
 *   </li>
 *   <li><tt>oscar.cache.dir</tt> - Sets the directory to be used by the
 *       cache as its cache directory. The cache directory is where all
 *       profile directories are stored and a profile directory is where a
 *       set of installed bundles are stored. By default, the cache
 *       directory is <tt>.oscar</tt> in the user's home directory. If
 *       this property is specified, then its value will be used as the cache
 *       directory instead of <tt>.oscar</tt>. This directory will be created
 *       if it does not exist.
 *   </li>
 *   <li><tt>oscar.cache.profile</tt> - Sets the profile name that will be
 *       used to create a profile directory inside of the cache directory.
 *       The created directory will contained all installed bundles associated
 *       with the profile.
 *   </li>
 *   <li><tt>oscar.cache.profiledir</tt> - Sets the directory to use as the
 *       profile directory for the bundle cache; by default the profile
 *       name is used to create a directory in the <tt>.oscar</tt> cache
 *       directory. If this property is specified, then the cache directory
 *       and profile name properties are ignored. The specified value of this
 *       property is used directly as the directory to contain all cached
 *       bundles. If this property is set, it is not necessary to set the
 *       cache directory or profile name properties. This directory will be
 *       created if it does not exist.
 *   </li>
 * </ul>
 * <p>
 * For specific information on how to configure Oscar using system properties,
 * refer to the Oscar usage documentation.
 * </p>
 * @see org.ungoverned.oscar.util.DefaultBundleArchive
**/
public class DefaultBundleCache implements BundleCache
{
    public static final String CACHE_BUFSIZE_PROP = "oscar.cache.bufsize";
    public static final String CACHE_DIR_PROP = "oscar.cache.dir";
    public static final String CACHE_PROFILE_DIR_PROP = "oscar.cache.profiledir";
    public static final String CACHE_PROFILE_PROP = "oscar.cache.profile";

    protected static transient int BUFSIZE = 4096;
    protected static transient final String CACHE_DIR_NAME = ".oscar";
    protected static transient final String BUNDLE_DIR_PREFIX = "bundle";

    private Oscar m_oscar = null;
    private File m_profileDir = null;
    private BundleArchive[] m_archives = null;

    public DefaultBundleCache()
    {
    }

    public void initialize(Oscar oscar) throws Exception
    {
        // Save Oscar reference.
        m_oscar = oscar;

        // Get buffer size value.
        try
        {
            String sBufSize =
                m_oscar.getConfigProperty(CACHE_BUFSIZE_PROP);
            if (sBufSize != null)
            {
                BUFSIZE = Integer.parseInt(sBufSize);
            }
        }
        catch (NumberFormatException ne)
        {
            // Use the default value.
        }

        // See if the profile directory is specified.
        String profileDirStr =
            m_oscar.getConfigProperty(CACHE_PROFILE_DIR_PROP);
        if (profileDirStr != null)
        {
            m_profileDir = new File(profileDirStr);
        }
        else
        {
            // Since no profile directory was specified, then the profile
            // directory will be a directory in the cache directory named
            // after the profile.

            // First, determine the location of the cache directory; it
            // can either be specified or in the default location.
            String cacheDirStr =
                m_oscar.getConfigProperty(CACHE_DIR_PROP);
            if (cacheDirStr == null)
            {
                // Since no cache directory was specified, put it
                // ".oscar" in the user's home by default.
                cacheDirStr = System.getProperty("user.home");
                cacheDirStr = cacheDirStr.endsWith(File.separator)
                    ? cacheDirStr : cacheDirStr + File.separator;
                cacheDirStr = cacheDirStr + CACHE_DIR_NAME;
            }

            // Now, get the profile name.
            String profileName =
                m_oscar.getConfigProperty(CACHE_PROFILE_PROP);
            if (profileName == null)
            {
                throw new IllegalArgumentException(
                    "No profile name or directory has been specified.");
            }
            // Profile name cannot contain the File.separator char.
            else if (profileName.indexOf(File.separator) >= 0)
            {
                throw new IllegalArgumentException(
                    "The profile name cannot contain the file separator character.");
            }

            m_profileDir = new File(cacheDirStr, profileName);
        }

        // Create profile directory.
        if (!m_profileDir.exists())
        {
            if (!m_profileDir.mkdirs())
            {
                Oscar.error("Unable to create directory: " + m_profileDir);
                throw new RuntimeException("Unable to create profile directory.");
            }
        }

        // Create the existing bundle archives in the profile directory,
        // if any exist.
        File[] children = m_profileDir.listFiles();
        int count = 0;
        for (int i = 0; (children != null) && (i < children.length); i++)
        {
            // Count the legitimate bundle directories.
            if (children[i].getName().startsWith(BUNDLE_DIR_PREFIX))
            {
                count++;
            }
        }
        m_archives = new BundleArchive[count];
        count = 0;
        for (int i = 0; (children != null) && (i < children.length); i++)
        {
            // Ignore directories that aren't bundle directories.
            if (children[i].getName().startsWith(BUNDLE_DIR_PREFIX))
            {
                String id = children[i].getName().substring(BUNDLE_DIR_PREFIX.length());
                m_archives[count] = new DefaultBundleArchive(
                    children[i], Long.parseLong(id));
                count++;
            }
        }
    }

    public BundleArchive[] getArchives()
        throws Exception
    {
        return m_archives;
    }

    public BundleArchive getArchive(long id)
        throws Exception
    {
        for (int i = 0; i < m_archives.length; i++)
        {
            if (m_archives[i].getId() == id)
            {
                return m_archives[i];
            }
        }
        return null;
    }

    public BundleArchive create(long id, String location, InputStream is)
        throws Exception
    {
        // Define new bundle's directory.
        File bundleDir = new File(m_profileDir, "bundle" + id);

        try
        {
            // Buffer the input stream.
            is = new BufferedInputStream(is, DefaultBundleCache.BUFSIZE);
            // Create an archive instance for the new bundle.
            BundleArchive ba = new DefaultBundleArchive(bundleDir, id, location, is);
            // Add the archive instance to the list of bundle archives.
            BundleArchive[] bas = new BundleArchive[m_archives.length + 1];
            System.arraycopy(m_archives, 0, bas, 0, m_archives.length);
            bas[m_archives.length] = ba;
            m_archives = bas;
            return ba;
        }
        finally
        {
            if (is != null) is.close();
        }
    }

    public void update(BundleArchive ba, InputStream is)
        throws Exception
    {
        try
        {
            // Buffer the input stream.
            is = new BufferedInputStream(is, DefaultBundleCache.BUFSIZE);
            // Do the update.
            ((DefaultBundleArchive) ba).update(is);
        }
        finally
        {
            if (is != null) is.close();
        }
    }

    public void purge(BundleArchive ba)
        throws Exception
    {
        ((DefaultBundleArchive) ba).purge();
    }

    public void remove(BundleArchive ba)
        throws Exception
    {
        ((DefaultBundleArchive) ba).remove();
    }
}