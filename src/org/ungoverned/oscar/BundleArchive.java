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
import java.util.Map;

import org.osgi.framework.BundleActivator;

/**
 * <p>
 * This interface represents an individual cached bundle in the
 * bundle cache. Oscar uses this interface to access all information
 * about the associated bundle's cached information. Classes that implement
 * this interface will be related to a specific implementation of the
 * <tt>BundleCache</tt> interface.
 * </p>
 * @see org.ungoverned.oscar.BundleCache
**/
public interface BundleArchive
{
    /**
     * <p>
     * Returns the identifier of the bundle associated with this archive.
     * </p>
     * @return the identifier of the bundle associated with this archive.
    **/
    public long getId();
    
    /**
     * <p>
     * Returns the location string of the bundle associated with this archive.
     * </p>
     * @return the location string of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public String getLocation()
        throws Exception;

    /**
     * <p>
     * Returns the persistent state of the bundle associated with the archive;
     * this value will be either <tt>Bundle.INSTALLED</tt> or <tt>Bundle.ACTIVE</tt>.
     * </p>
     * @return the persistent state of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public int getPersistentState()
        throws Exception;

    /**
     * <p>
     * Sets the persistent state of the bundle associated with this archive;
     * this value will be either <tt>Bundle.INSTALLED</tt> or <tt>Bundle.ACTIVE</tt>.
     * </p>
     * @param state the new bundle state to write to the archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public void setPersistentState(int state)
        throws Exception;

    /**
     * <p>
     * Returns the start level of the bundle associated with this archive.
     * </p>
     * @return the start level of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public int getStartLevel()
        throws Exception;

    /**
     * <p>
     * Sets the start level of the bundle associated with this archive.
     * </p>
     * @param level the new bundle start level to write to the archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public void setStartLevel(int level)
        throws Exception;

    /**
     * <p>
     * Returns an appropriate data file for the bundle associated with the
     * archive using the supplied file name.
     * </p>
     * @return a <tt>File</tt> corresponding to the requested data file for
     *         the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public File getDataFile(String fileName)
        throws Exception;

    /**
     * <p>
     * Returns the persistent bundle activator of the bundle associated with
     * this archive; this is a non-standard OSGi method that is only called
     * when Oscar is running in non-strict OSGi mode.
     * </p>
     * @param loader the class loader to use when trying to instantiate
     *        the bundle activator.
     * @return the persistent bundle activator of the bundle associated with
     *         this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public BundleActivator getActivator(ClassLoader loader)
        throws Exception;

    /**
     * <p>
     * Sets the persistent bundle activator of the bundle associated with
     * this archive; this is a non-standard OSGi method that is only called
     * when Oscar is running in non-strict OSGi mode.
     * </p>
     * @param obj the new persistent bundle activator to write to the archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public void setActivator(Object obj)
        throws Exception;

    /**
     * <p>
     * Returns the number of revisions of the bundle associated with the
     * archive. When a bundle is updated, the previous version of the bundle
     * is maintained along with the new revision until old revisions are
     * purged. The revision count reflects how many revisions of the bundle
     * are currently available in the cache.
     * </p>
     * @return the number of revisions of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public int getRevisionCount()
        throws Exception;

    /**
     * <p>
     * Returns the main attributes of the JAR file manifest header of the
     * specified revision of the bundle associated with this archive. The
     * returned map should be case insensitive.
     * </p>
     * @param revision the specified revision.
     * @return the case-insensitive JAR file manifest header of the specified
     *         revision of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public Map getManifestHeader(int revision)
        throws Exception;

    /**
     * <p>
     * Returns an array of <tt>String</tt>s that represent the class path of
     * the specified revision of the bundle associated with this archive.
     * Currently, these values are restricted to absolute paths in the file
     * system, but this may be lifted in the future (perhaps they should be
     * <tt>ResourceSource</tt>s from the Module Loader.
     * </p>
     * @param revision the specified revision.
     * @return a <tt>String</tt> array of the absolute path names that
     *         comprise the class path of the specified revision of the
     *         bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public String[] getClassPath(int revision)
        throws Exception;

    /**
     * <p>
     * Returns the absolute file path for the specified native library of the
     * specified revision of the bundle associated with this archive.
     * </p>
     * @param revision the specified revision.
     * @param libName the name of the library.
     * @return a <tt>String</tt> that contains the absolute path name to
     *         the requested native library of the specified revision of the
     *         bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public String findLibrary(int revision, String libName)
        throws Exception;
}