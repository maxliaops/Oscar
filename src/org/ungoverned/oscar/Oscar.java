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

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.ungoverned.moduleloader.*;
import org.ungoverned.moduleloader.search.ImportSearchPolicy;
import org.ungoverned.moduleloader.search.ValidationException;
import org.ungoverned.moduleloader.search.ValidationListener;
import org.ungoverned.oscar.util.*;

public class Oscar
{
    // Debug output.
    private static PrintStream m_debugOut = null;
    // Error output.
    private static PrintStream m_errorOut = null;
    // Output lock.
    private static String m_outputLockObj = new String("output lock");

    // CONCURRENCY NOTE: The admin lock is used when
    // performing an administrative operation; this prevents
    // multiple administrative operations (e.g., install,
    // start, refresh, etc.) from happening at the same
    // time. The quick lock is used when performing a quick
    // operation that potentially only needs to read
    // information or to modify a single bundle in some
    // small way. To ensure total isolation in the framework
    // core (i.e., only one thread is present), it is necessary
    // to acquire both the admin and quick locks. In such a
    // scenario, the quick lock should only be held for the
    // shortest possible time so that other quick operations
    // can occur in parallel to admin operations; otherwise,
    // the potential for deadlock is great. For admin operations,
    // the exclusion lock should always be acquired before
    // the quick lock.
    //
    // When using only the quick lock, it is important that the
    // code inside the lock does not reference directly or
    // indirectly (i.e., via event callbacks) code that will
    // try to acquire the admin lock. If this happens then
    // deadlocks can occur.

    // Admin lock.
    private String m_adminLock = new String("admin lock");
    // Quick lock.
    private String m_quickLock = new String("quick lock");

    // MODULE MANAGER.
    private ModuleManager m_mgr = null;

    // Map of configuration properties passed into constructor.
    private transient Map m_configPropMap = null;
    // Next available bundle identifier.
    private transient long m_nextId = 1L;
    // Next available service identifier.
    private transient long m_nextServiceId = 1L;
    // Framework's active start level.
    private transient int m_activeStartLevel =
        OscarConstants.FRAMEWORK_INACTIVE_STARTLEVEL;

    // List of event listeners.
    private transient OscarDispatchQueue m_dispatchQueue = null;
    // Re-usable event dispatchers.
    private Dispatcher m_frameworkDispatcher = null;
    private Dispatcher m_bundleDispatcher = null;
    private Dispatcher m_serviceDispatcher = null;

    // Maps a bundle location to a bundle.
    private transient HashMap m_installedBundleMap = null;
    // An array of uninstalled bundles before a refresh occurs.
    private transient BundleImpl[] m_uninstalledBundles = null;

    // Local file system cache.
    private transient BundleCache m_cache = null;
    // Place to store and obtain framework properties
    private transient Map m_frameworkPropMap = null;

    // Reusable admin permission object for all instances
    // of the BundleImpl.
    private static AdminPermission m_adminPerm = new AdminPermission();

    // Reusable privileged action object for starting/stopping
    // bundles.
    private StartStopPrivileged m_startStopPrivileged = new StartStopPrivileged(this);

    // Status flag for Oscar.
    public static final int UNKNOWN_STATUS  = -1;
    public static final int RUNNING_STATUS  = 0;
    public static final int STARTING_STATUS = 1;
    public static final int STOPPING_STATUS = 2;
    private transient int m_oscarStatus = UNKNOWN_STATUS;

    /**
     * This static code block automatically loads the system
     * properties associated with the Oscar installation as
     * soon as the class is loaded.
    **/
    static
    {
        initializeSystemProperties();
    }

    /**
     * <p>
     * Creates an instance of Oscar where all configuration properties
     * (e.g., profile name, profile directory, strictness, and
     * embedded execution) are specified using system properties.
     * If the <a href="util/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * is being used, then at a minimum a profile name or profile
     * directory must be specified in the system properties.
     * </p>
     * <p>
     * System properties can be set using a <tt>system.properties</tt>
     * file; refer to the <a href="http://oscar.objectweb.org/usage.html">usage
     * document</a> for more information.
     * </p>
     * @see #Oscar(java.util.Properties) For information on Oscar properties.
    **/
    public Oscar()
    {
        this(null, null);
    }

    /**
     * <p>
     * Creates an instance of Oscar where all configuration
     * properties (e.g., profile name, profile directory, strictness, and
     * embedded execution) are specified in the supplied <tt>Properties</tt>
     * instance. If <tt>Properties</tt> is a null instance, then
     * <tt>System.getProperty()</tt> is used to find all configuration
     * properties. If the
     * <a href="util/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * is being used, then at a minimum a profile name or profile
     * directory must be specified.
     * </p>
     * <p>
     * The following are framework configuration properties that can be
     * specified in the <tt>Properties</tt> instance:
     * </p>
     * <ul>
     *   <li><tt>oscar.bundle.properties</tt> - The location of the
     *       <tt>bundle.properties</tt> file; by default this file is
     *       located in the <tt>lib</tt> directory of the Oscar installation
     *       directory. This file contains attribute-value property pairs and
     *       Oscar will automatically load these properties and make them
     *       available via <tt>BundleContext.getProperty()</tt>.
     *   </li>
     *   <li><tt>oscar.cache.class</tt> - The class name to be used when
     *       creating an instance for the bundle cache; this class must
     *       implement the <tt>BundleCache</tt> interface and have a default
     *       constructor. By default, Oscar will create an instance of
     *       <tt>DefaultBundleCache</tt> for the bundle cache.
     *   </li>
     *   <li><tt>oscar.auto.install</tt> - Space-delimited list of bundles
     *       to automatically install when Oscar is started. By default,
     *       the bundles specified in this property will be installed into
     *       start level 1. It is also possible to append a specific
     *       start level to this property name to assign to the specified
     *       bundles (e.g., <tt>oscar.auto.install.2</tt>). These variants
     *       will be processed in sequence until a successor cannot be
     *       found.
     *   </li>
     *   <li><tt>oscar.auto.start</tt> - Space-delimited list of bundles
     *       to automatically install and start when Oscar is started.
     *       By default, the bundles specified in this property will be
     *       installed into start level 1. It is also possible to append a
     *       specific start level to this property name to assign to the
     *       specified bundles (e.g., <tt>oscar.auto.start.2</tt>). These
     *       variants will be processed in sequence until a successor cannot
     *       be found.
     *   </li>
     *   <li><tt>oscar.startlevel.framework</tt> - The initial start level
     *       of the Oscar framework once it starts execution; the default
     *       value is 1.
     *   </li>
     *   <li><tt>oscar.startlevel.bundle</tt> - The default start level for
     *       newly installed bundles; the default value is 1.
     *   </li>
     *   <li><tt>oscar.embedded.execution</tt> - Flag to indicate whether
     *       Oscar is embedded into a host application; the default value is
     *       "<tt>false</tt>". If this flag is "<tt>true</tt>" then Oscar
     *       will not called <tt>System.exit()</tt> upon termination.
     *   </li>
     *   <li><tt>oscar.strict.osgi</tt> - Flag to indicate whether Oscar is
     *       running in strict OSGi mode; the default value is "<tt>true</tt>".
     *       If this flag is "<tt>false</tt>" it enables a non-OSGi-compliant
     *       feature by persisting <tt>BundleActivator</tt>s that implement
     *       <tt>Serializable</tt>. This feature is not recommended since
     *       it is non-compliant.
     *   </li>
     * </ul>
     * <p>
     * Besides the above framework properties, it is also possible to set
     * properties for the bundle cache via the Oscar constructor. The
     * available bundle cache properties depend on the cache implementation
     * being used. For the properties of the default bundle cache, refer to the
     * <a href="util/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * API documentation. All of these properties can specified in one
     * of three ways:
     * </p>
     * <ul>
     *   <li>On the command line when starting the JVM using the "<tt>-D</tt>"
     *       option.
     *   </li>
     *   <li>In the <tt>system.properties</tt> file.
     *   </li>
     *   <li>In the <tt>Properties</tt> instance supplied to the Oscar
     *       constructor.</tt>
     *   </li>
     * </ul>
     * <p>
     * The <tt>system.properties</tt> file overwrites any property values
     * specified on the command line. If a <tt>Properties</tt> instance is
     * passed into Oscar's constructor, then all system properties are
     * ignored and only the <tt>Properties</tt> instance is used to locate
     * configuration property values.
     * </p>
     * @param props the properties used to initialize Oscar; may also
     *        be <tt>null</tt>.
    **/
    public Oscar(Properties props)
    {
        this(props, null);
    }

    /**
     * <p>
     * Creates an instance of Oscar where all configuration properties
     * (e.g., profile name, profile directory, strictness, and
     * embedded execution) are specified using system properties.
     * If the <a href="util/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * is being used, then at a minimum a profile name or profile
     * directory must be specified in the system properties.
     * This constructor accepts a list of <tt>BundleActivator</tt>
     * instances that will be started/stopped by the System Bundle when
     * the framework is started/stopped; this is useful for when Oscar
     * is embedded into a host application that wants to provide services
     * to the bundles inside of Oscar.
     * </p>
     * @param activatorList list of bundle activators to be started/stopped by
     *        the system bundle; may also be <tt>null</tt>
     * @see #Oscar(java.util.Properties) For information on Oscar properties.
    **/
    public Oscar(List activatorList)
    {
        this(null, activatorList);
    }

    /**
     * <p>
     * Creates an instance of Oscar where all configuration
     * properties (e.g., profile name, profile directory, strictness, and
     * embedded execution) are specified in the supplied <tt>Properties</tt>
     * instance. If <tt>Properties</tt> is a null instance, then
     * <tt>System.getProperty()</tt> is used to find all configuration
     * properties. If the
     * <a href="util/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * is being used, then at a minimum a profile name or profile
     * directory must be specified in the system properties.
     * This constructor also accepts a list of <tt>BundleActivator</tt>
     * instances that will be started/stopped by the System Bundle when
     * the framework is started/stopped; this is useful for when Oscar is
     * embedded into a host application that wants to provide services to
     * the bundles inside of Oscar.
     * </p>
     * @param props the properties used to initialize Oscar; may also
     *        be <tt>null</tt>.
     * @param activatorList list of bundle activators to be started/stopped by
     *        the system bundle; may also be <tt>null</tt>
     * @see #Oscar(java.util.Properties) For information on Oscar properties.
    **/
    public Oscar(Properties props, List activatorList)
    {
        // Create a copy of the passed in configuration properties.
        if (props != null)
        {
            m_configPropMap = new HashMap();
            for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
            {
                String name = (String) e.nextElement();
                m_configPropMap.put(name, props.getProperty(name));
            }
        }

        // Create default storage system from the specified cache class
        // or use the default cache if no custom cache was specified.
        String className = getConfigProperty(OscarConstants.CACHE_CLASS_PROP);
        if (className == null)
        {
            className = DefaultBundleCache.class.getName();
        }

        try
        {
            Class clazz = Class.forName(className);
            m_cache = (BundleCache) clazz.newInstance();
            m_cache.initialize(this);
        }
        catch (Exception ex)
        {
            System.err.println("Error creating bundle cache:");
            ex.printStackTrace();
            System.err.println("\nThis may result from the fact that Oscar 1.0 uses a");
            System.err.println("different bundle cache format than previous versions");
            System.err.println("of Oscar. Please read the bundle cache documentation for");
            System.err.println("more details: http://oscar.objectweb.org/cache.html.");

            // Only shutdown the JVM if Oscar is running stand-alone.
            String embedded = getConfigProperty(
                OscarConstants.EMBEDDED_EXECUTION_PROP);
            boolean isEmbedded = (embedded == null)
                ? false : embedded.equals("true");
            if (!isEmbedded)
            {
                System.exit(-1);
            }
            else
            {
                throw new RuntimeException(ex.toString());
            }
        }

        // Initialize.
        initialize(activatorList);
    }

    /**
     * <p>
     * Oscar uses this method whenever it needs to access any
     * configuration properties. This will look for a configuration
     * property either in the system properties or in the configuration
     * property map that was passed into the Oscar constructor; that is,
     * Oscar will look in one place or the other, but not both. This
     * approach was taken to simplify support for having multiple
     * instances of Oscar in memory at the same time.
     * </p>
     * <p>
     * The approach is very simple. If no <tt>Properties</tt> instance
     * was passed into the constructor, then this method only searches
     * <tt>System.getProperty()</tt> to find property values. If a
     * <tt>Properties</tt> instance was passed into the constructor,
     * then it only searches the supplied instance. When creating multiple
     * instances of Oscar, a <tt>Properties</tt> instance should be
     * supplied to the constructor so that all instances do not end up
     * using the same bundle cache directory.
     * </p>
     * @param name the name of the configuration property to retrieve.
     * @return the value of the specified configuration property or <tt>null</tt>
     *         if the property is not defined.
    **/
    public String getConfigProperty(String name)
    {
        if (m_configPropMap != null)
        {
            return (String) m_configPropMap.get(name);
        }
        return System.getProperty(name);
    }

    private void setConfigProperty(String name, String value)
    {
        // If there is a config property map, then
        // set the value in it, otherwise put it in
        // the system properties.
        if (m_configPropMap != null)
        {
            m_configPropMap.put(name, value);
        }
        else
        {
            System.setProperty(name, value);
        }
    }

    /**
     * Called to initialize a new instance.
    **/
    private void initialize(List activatorList)
    {
        ImportSearchPolicy searchPolicy = new OSGiImportSearchPolicy(this);
        m_mgr = new ModuleManager(searchPolicy, new OSGiURLPolicy(this));

        // Add the OSGi selection policy as a module listener, since
        // it needs to keep track of when modules are removed so that
        // it can flush its package export cache.
        m_mgr.addModuleListener((OSGiSelectionPolicy) searchPolicy.getSelectionPolicy());

        // Add a validation listener to the import/export search policy
        // so that we will be notified when modules are validated
        // (i.e., resolved) in order to update the bundle state.
        searchPolicy.addValidationListener(new ValidationListener() {
            public void moduleValidated(ModuleEvent event)
            {
                synchronized (m_quickLock)
                {
                    try
                    {
                        long id = BundleInfo.getBundleIdFromModuleId(
                            event.getModule().getId());
                        if (id >= 0)
                        {
                            // Update the bundle's state to resolved when the
                            // current module is resolved; just ignore resolve
                            // events for older revisions since this only occurs
                            // when an update is done on an unresolved bundle
                            // and there was no refresh performed.
                            BundleImpl bundle = (BundleImpl) getBundle(id);
                            if (bundle.getInfo().getCurrentModule() == event.getModule())
                            {
                                bundle.getInfo().setState(Bundle.RESOLVED);
                            }
                        }
                    }
                    catch (NumberFormatException ex)
                    {
                        // Ignore.
                    }
                }
            }

            public void moduleInvalidated(ModuleEvent event)
            {
                // We can ignore this, because the only time it
                // should happen is when a refresh occurs. The
                // refresh operation resets the bundle's state
                // by calling BundleInfo.reset(), thus it is not
                // necessary for us to reset the bundle's state
                // here.
            }
        });

        // Oscar is now in its startup sequence.
        m_oscarStatus = STARTING_STATUS;

        // Turn on error information...
        m_errorOut = System.err;

        // Initialize private members.
        m_dispatchQueue = new OscarDispatchQueue();
        m_installedBundleMap = new HashMap();

        // Set up the framework properties.
        m_frameworkPropMap = new CaseInsensitiveMap();
        initializeOsgiProperties();
        initializeBundleProperties();

        // Before we reload any cached bundles, let's create a system
        // bundle that is responsible for providing specific container
        // related services.
        SystemBundle systembundle = null;
        try
        {
            // Create a simple bundle info for the system bundle.
            BundleInfo info = new BundleInfo(
                new SystemBundleArchive(), null);
            systembundle = new SystemBundle(this, info, activatorList);
            systembundle.getInfo().addModule(
                m_mgr.addModule(
                    "0", systembundle.getAttributes(),
                    systembundle.getResourceSources(),
                    systembundle.getLibrarySources()));
            m_installedBundleMap.put(
                systembundle.getInfo().getLocation(), systembundle);

            // Manually validate the System Bundle, which will cause its
            // state to be set to RESOLVED.
            try
            {
                searchPolicy.validate(systembundle.getInfo().getCurrentModule());
            }
            catch (ValidationException ex)
            {
                // This should never happen.
                int[] v = (int[]) ex.getVersion();
                throw new BundleException("Unresolved package: "
                    + ex.getIdentifier() + "; specification-version=\""
                    + v[0] + "." + v[1] + "." + v[2] + "\"");
            }

            // Start the system bundle; this will set its state
            // to STARTING, we must set its state to ACTIVE after
            // all bundles are restarted below according to the spec.
            systembundle.start();
        }
        catch (Exception ex)
        {
            m_mgr = null;
            Oscar.error("Unable to start system bundle: " + ex);
            throw new RuntimeException("Unable to start system bundle.");
        }

        // Reload and cached bundles.
        BundleArchive[] archives = null;

        // First get cached bundle identifiers.
        try
        {
            archives = m_cache.getArchives();
        }
        catch (Exception ex)
        {
            Oscar.error("Oscar: Unable to list saved bundles: " + ex);
            archives = null;
        }

        BundleImpl bundle = null;

        // Now install all cached bundles.
        for (int i = 0; (archives != null) && (i < archives.length); i++)
        {
            // Make sure our id generator is not going to overlap.
            // TODO: This is not correct since it may lead to re-used
            // ids, which is not okay according to OSGi.
            m_nextId = Math.max(m_nextId, archives[i].getId() + 1);

            try
            {
                // It is possible that a bundle in the cache was previously
                // uninstalled, but not completely deleted (perhaps because
                // of a crash or a locked file), so if we see an archive
                // with an UNINSTALLED persistent state, then try to remove
                // it now.
                if (archives[i].getPersistentState() == Bundle.UNINSTALLED)
                {
                    m_cache.remove(archives[i]);
                }
                // Otherwise re-install the cached bundle.
                else
                {
                    // Install the cached bundle.
                    bundle = (BundleImpl) installBundle(
                        archives[i].getId(), archives[i].getLocation(), null);
                }
            }
            catch (BundleException ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                try
                {
                    Oscar.error("Oscar: Unable to re-install "
                        + archives[i].getLocation());
                }
                catch (Exception ex2)
                {
                    Oscar.error("Oscar: Unable to re-install bundle "
                        + archives[i].getId());
                }
                Oscar.error("Oscar: " + ex);
                // TODO: Perhaps we should remove the cached bundle?
            }
            catch (Exception ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                try
                {
                    Oscar.error("Oscar: Exception while re-installing "
                        + archives[i].getLocation());
                }
                catch (Exception ex2)
                {
                    Oscar.error("Oscar: Exception while re-installing bundle "
                        + archives[i].getId());
                }
                // TODO: Perhaps we should remove the cached bundle?
            }
        }

        // Get the framework's default start level.
        int startLevel = OscarConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
        String s = getConfigProperty(OscarConstants.FRAMEWORK_STARTLEVEL_PROP);
        if (s != null)
        {
            try
            {
                startLevel = Integer.parseInt(s);
            }
            catch (NumberFormatException ex)
            {
                startLevel = OscarConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
            }
        }

        // Load bundles from auto-install and auto-start properties;
        processAutoProperties();

        // This will restart bundles if necessary.
        setStartLevelInternal(startLevel);

        // Oscar is now running.
        m_oscarStatus = RUNNING_STATUS;

        // Set the system bundle state to ACTIVE.
        systembundle.getInfo().setState(Bundle.ACTIVE);

        // Fire started event for system bundle.
        fireBundleEvent(BundleEvent.STARTED, systembundle);

        // Send a framework event to indicate Oscar has started.
        fireFrameworkEvent(FrameworkEvent.STARTED, getBundle(0), null);
    }

    /**
     * This method cleanly shuts down Oscar, it must be called at the
     * end of a session in order to shutdown all active bundles.
    **/
    public void shutdown()
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        // Change Oscar status from running to stopping.
        synchronized (m_adminLock)
        {
            // If Oscar is not running, then just return.
            if (m_oscarStatus != RUNNING_STATUS)
            {
                return;
            }

            // Oscar is now in its shutdown sequence.
            m_oscarStatus = STOPPING_STATUS;
        }

        // Set the start level to zero in order to stop
        // all bundles in the framework.
        setStartLevelInternal(0);

        // Just like initialize() called the system bundle's start()
        // method, we must call its stop() method here so that it
        // can perform any necessary clean up.
        try {
            getBundle(0).stop();
        } catch (Exception ex) {
            fireFrameworkEvent(FrameworkEvent.ERROR, getBundle(0), ex);
            Oscar.error("Error stopping system bundle.", ex);
        }

        // Loop through all bundles and update any updated bundles.
        Bundle[] bundles = getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            BundleImpl bundle = (BundleImpl) bundles[i];
            if (bundle.getInfo().isRemovalPending())
            {
                try
                {
                    purgeBundle(bundle);
                }
                catch (Exception ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                    Oscar.error("Oscar: Unable to purge bundle "
                        + bundle.getInfo().getLocation());
                }
            }
        }

        // Remove any uninstalled bundles.
        for (int i = 0;
            (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
            i++)
        {
            try
            {
                removeBundle(m_uninstalledBundles[i]);
            }
            catch (Exception ex)
            {
                Oscar.error("Oscar: Unable to remove "
                    + m_uninstalledBundles[i].getInfo().getLocation());
            }
        }

        // Shutdown event dispatching queue.
        DispatchQueue.shutdown();

        // Oscar is no longer in a usable state.
        m_oscarStatus = UNKNOWN_STATUS;
    }

    /**
     * Returns the active start level of the framework; this method
     * implements functionality for the Start Level service.
     * @return the active start level of the framework.
    **/
    protected int getStartLevel()
    {
        return m_activeStartLevel;
    }

    /**
     * Implements the functionality of the <tt>setStartLevel()</tt>
     * method for the StartLevel service, but does not do the security or
     * parameter check. The security and parameter check are done in the
     * StartLevel service implementation because this method is called on
     * a separate thread and the caller's thread would already be gone if
     * we did the checks in this method.
     * @param requestedLevel the new start level of the framework.
    **/
    protected void setStartLevelInternal(int requestedLevel)
    {
        // Determine if we are lowering or raising the
        // active start level.
        boolean lowering = (requestedLevel < m_activeStartLevel);

        // Record new start level.
        m_activeStartLevel = requestedLevel;

        // Get exclusion lock to make sure that no one starts
        // an operation that might affect the bundle list.
        synchronized (m_adminLock)
        {
            // Get array of all installed bundles.
            Bundle[] bundles = getBundles();

            // Sort bundle array by start level either ascending or
            // descending depending on whether the start level is being
            // lowered or raised.
            Comparator comparator = null;
            if (lowering)
            {
                // Sort descending to stop highest start level first.
                comparator = new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        BundleImpl b1 = (BundleImpl) o1;
                        BundleImpl b2 = (BundleImpl) o2;
                        if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            < b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return 1;
                        }
                        else if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            > b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return -1;
                        }
                        return 0;
                    }
                };
            }
            else
            {
                // Sort ascending to start lowest start level first.
                comparator = new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        BundleImpl b1 = (BundleImpl) o1;
                        BundleImpl b2 = (BundleImpl) o2;
                        if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            > b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return 1;
                        }
                        else if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            < b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return -1;
                        }
                        return 0;
                    }
                };
            }

            Arrays.sort(bundles, comparator);

            // Stop or start the bundles according to the start level.
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                BundleImpl impl = (BundleImpl) bundles[i];

                // Ignore the system bundle, since its start() and
                // stop() methods get called explicitly in initialize()
                // and shutdown(), respectively.
                if (impl.getInfo().getBundleId() == 0)
                {
                    continue;
                }

                // Start or stop bundle accordingly.
                if (impl.getInfo().getStartLevel(getInitialBundleStartLevel())
                    <= m_activeStartLevel)
                {
                    try
                    {
                        startBundleWithStartLevel(impl);
                    }
                    catch (Throwable th)
                    {
th.printStackTrace();
                        fireFrameworkEvent(FrameworkEvent.ERROR, impl, th);
                        Oscar.error("Oscar: Error starting "
                            + impl.getInfo().getLocation());
                    }
                }
                else if (impl.getInfo().getStartLevel(getInitialBundleStartLevel())
                    > m_activeStartLevel)
                {
                    try
                    {
                        stopBundleWithStartLevel(impl);
                    }
                    catch (Throwable th)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, impl, th);
                        Oscar.error("Oscar: Error stopping "
                            + impl.getInfo().getLocation());
                    }
                }
            }
        }

        fireFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, getBundle(0), null);
    }

    /**
     * Returns the start level into which newly installed bundles will
     * be placed by default; this method implements functionality for
     * the Start Level service.
     * @return the default start level for newly installed bundles.
    **/
    protected int getInitialBundleStartLevel()
    {
        String s = getConfigProperty(OscarConstants.BUNDLE_STARTLEVEL_PROP);

        if (s != null)
        {
            try
            {
                return Integer.parseInt(s);
            }
            catch (NumberFormatException ex)
            {
                // Ignore and return the default value.
            }
        }
        return OscarConstants.BUNDLE_DEFAULT_STARTLEVEL;
    }

    /**
     * Sets the default start level into which newly installed bundles
     * will be placed; this method implements functionality for the Start
     * Level service.
     * @param startLevel the new default start level for newly installed
     *        bundles.
     * @throws java.lang.IllegalArgumentException if the specified start
     *         level is not greater than zero.
     * @throws java.security.SecurityException if the caller does not
     *         have <tt>AdminPermission</tt>.
    **/
    protected void setInitialBundleStartLevel(int startLevel)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        if (startLevel <= 0)
        {
            throw new IllegalArgumentException(
                "Initial start level must be greater than zero.");
        }

        setConfigProperty(
            OscarConstants.BUNDLE_STARTLEVEL_PROP,
            Integer.toString(startLevel));
    }

    /**
     * Returns the start level for the specified bundle; this method
     * implements functionality for the Start Level service.
     * @param bundle the bundle to examine.
     * @return the start level of the specified bundle.
     * @throws java.lang.IllegalArgumentException if the specified
     *          bundle has been uninstalled.
    **/
    protected int getBundleStartLevel(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return ((BundleImpl) bundle).getInfo().getStartLevel(getInitialBundleStartLevel());
    }

    /**
     * Sets the start level of the specified bundle; this method
     * implements functionality for the Start Level service.
     * @param bundle the bundle whose start level is to be modified.
     * @param startLevel the new start level of the specified bundle.
     * @throws java.lang.IllegalArgumentException if the specified
     *          bundle is the system bundle or if the bundle has been
     *          uninstalled.
     * @throws java.security.SecurityException if the caller does not
     *          have <tt>AdminPermission</tt>.
    **/
    protected void setBundleStartLevel(Bundle bundle, int startLevel)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        // Cannot change the system bundle.
        if (bundle.getBundleId() == 0)
        {
            throw new IllegalArgumentException(
                "Cannot change system bundle start level.");
        }
        else if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        if (startLevel >= 1)
        {
            BundleImpl impl = (BundleImpl) bundle;
            impl.getInfo().setStartLevel(startLevel);
            try
            {
                m_cache.getArchive(impl.getBundleId()).setStartLevel(startLevel);
            }
            catch (Exception ex)
            {
                Oscar.error("Unable to save start level.", ex);
            }

            try
            {
                // Start or stop the bundle if necessary.
                if (impl.getInfo().getStartLevel(getInitialBundleStartLevel())
                    <= getStartLevel())
                {
                    startBundleWithStartLevel(impl);
                }
                else
                {
                    stopBundleWithStartLevel(impl);
                }
            }
            catch (Throwable th)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, impl, th);
                Oscar.error("Error starting/stopping bundle.", th);
            }
        }
    }

    /**
     * Returns whether a bundle is persistently started; this is an
     * method implementation for the Start Level service.
     * @param bundle the bundle to examine.
     * @return <tt>true</tt> if the bundle is marked as persistently
     *          started, <tt>false</tt> otherwise.
     * @throws java.lang.IllegalArgumentException if the specified
     *          bundle has been uninstalled.
    **/
    protected boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return
            (((BundleImpl) bundle).getInfo().getPersistentState()
                == Bundle.ACTIVE);
    }

    //
    // Oscar framework attribute methods.
    //

    /**
     * Returns the current status of Oscar; this information is
     * used to determine which actions to perform during various
     * execution activities.  For example, during startup a bundle's
     * state should not be saved since the state was recorded at
     * and at shutdown a bundle's state should not be saved since
     * since the last active state is used.
     *
     * @return <tt>UNKNOWN_STATUS</tt> if Oscar is in a bad state,
     *         <tt>RUNNING_STATUS</tt> if Oscar is up and running,
     *         <tt>STARTING_STATUS</tt> if Oscar is in its startup sequence, or
     *         <tt>STOPPING_STATUS</tt> if Oscar is in its shutdown sequence.
    **/
    public int getFrameworkStatus()
    {
        return m_oscarStatus;
    }

    /**
     * Returns the framework flag that indicates whether Oscar is
     * strictly adhering to the OSGi specification. If this is false,
     * then Oscar may provides some extended functionality that is
     * not part of the OSGi specification.
     * @return <tt>true</tt> if Oscar is in strict mode, <tt>false</tt> otherwise.
     * @deprecate use <tt>getConfigProperty(String name)</tt> instead.
    **/
    public boolean isStrictOSGi()
    {
        String strict = getConfigProperty(OscarConstants.STRICT_OSGI_PROP);
        return (strict == null) ? true : strict.equals("true");
    }

    //
    // Package management methods.
    //

    /**
     * Returns the bundle that exports the specified package. If the
     * package is not currently resolved, then it is resolved and the
     * corresponding bundle is returned.
     *
     * @param name the name of the exported package to find.
     * @return the exported package or null if no matching package was found.
    **/
    private BundleImpl findExportingBundle(String pkgName)
    {
        // Use the search policy utility method to try to
        // resolve the package.
        BundleImpl bundle = null;
        ImportSearchPolicy search =
            (ImportSearchPolicy) m_mgr.getSearchPolicy();
        int[] version = { 0, 0, 0 };
        Module exporter = search.resolveImportTarget(pkgName, version);
        if (exporter != null)
        {
            bundle = (BundleImpl) getBundle(
                BundleInfo.getBundleIdFromModuleId(exporter.getId()));
        }
        return bundle;
    }

    /**
     * Returns the exported package associated with the specified
     * package name. This is used by the PackageAdmin service
     * implementation.
     *
     * @param name the name of the exported package to find.
     * @return the exported package or null if no matching package was found.
    **/
    protected ExportedPackage getExportedPackage(String name)
    {
        BundleImpl bundle = findExportingBundle(name);
        if (bundle != null)
        {
            // We need to find the version of the exported package, but this
            // is tricky since there may be multiple versions of the package
            // offered by a given bundle, since multiple revisions of the
            // bundle JAR file may exist if the bundle was updated without
            // refreshing the framework. In this case, each revision of the
            // bundle JAR file is represented as a module in the BundleInfo
            // module array, which is ordered from oldest to newest. We assume
            // that the first module found to be exporting the package is the
            // provider of the package, which makes sense since it must have
            // been resolved first.
            Module[] modules = bundle.getInfo().getModules();
            for (int modIdx = 0; modIdx < modules.length; modIdx++)
            {
                Object version =
                    ImportSearchPolicy.getExportVersion(modules[modIdx], name);
                if (version != null)
                {
                    return new ExportedPackageImpl(
                        this, bundle, name, (int[]) version);
                }
            }
        }

        return null;
    }

    /**
     * Returns an array of all actively exported packages from the specified
     * bundle or if the specified bundle is <tt>null</tt> an array
     * containing all actively exported packages by all bundles.
     *
     * @param b the bundle whose exported packages are to be retrieved
     *        or <tt>null</tt> if the exported packages of all bundles are
     *        to be retrieved.
     * @return an array of exported packages.
    **/
    protected ExportedPackage[] getExportedPackages(Bundle b)
    {
        ExportedPackage[] pkgs = null;
        List list = new ArrayList();

        // If a bundle is specified, then return its
        // exported packages.
        if (b != null)
        {
            BundleImpl bundle = (BundleImpl) b;
            getExportedPackages(bundle, list);
        }
        // Otherwise return all exported packages.
        else
        {
            // First get exported packages from uninstalled bundles.
            for (int bundleIdx = 0;
                (m_uninstalledBundles != null) && (bundleIdx < m_uninstalledBundles.length);
                bundleIdx++)
            {
                BundleImpl bundle = m_uninstalledBundles[bundleIdx];
                getExportedPackages(bundle, list);
            }

            // Now get exported packages from installed bundles.
            Bundle[] bundles = getBundles();
            for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
            {
                BundleImpl bundle = (BundleImpl) bundles[bundleIdx];
                getExportedPackages(bundle, list);
            }
        }

        return (ExportedPackage[]) list.toArray(new ExportedPackage[list.size()]);
    }

    /**
     * Adds any current active exported packages from the specified bundle
     * to the passed in list.
     * @param bundle the bundle from which to retrieve exported packages.
     * @param list the list to which the exported packages are added
    **/
    private void getExportedPackages(BundleImpl bundle, List list)
    {
        // Since a bundle may have many modules associated with it,
        // one for each revision in the cache, search each module
        // for each revision to get all exports.
        Module[] modules = bundle.getInfo().getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            Object[][] exports =
                ImportSearchPolicy.getExportsAttribute(modules[modIdx]);
            if (exports.length > 0)
            {
                for (int expIdx = 0; expIdx < exports.length; expIdx++)
                {
                    // If the resolving module is the same as the current
                    // module, then this bundle exports the package so add
                    // the package to the list of exported packages.
                    if (exports[expIdx][ImportSearchPolicy.RESOLVING_MODULE_IDX]
                        == modules[modIdx])
                    {
                        list.add(new ExportedPackageImpl(
                            this, bundle,
                            (String) exports[expIdx][ImportSearchPolicy.IDENTIFIER_IDX],
                            (int[]) exports[expIdx][ImportSearchPolicy.VERSION_IDX]));
                    }
                }
            }
        }
    }

    protected Bundle[] getImportingBundles(ExportedPackage ep)
    {
        // Get exporting bundle; we need to use this internal
        // method because the spec says ep.getExportingBundle()
        // should return null if the package is stale.
        BundleImpl exporter = (BundleImpl)
            ((ExportedPackageImpl) ep).getExportingBundleInternal();
        BundleInfo exporterInfo = exporter.getInfo();
        String exportName = ep.getName();

        // Create list for storing importing bundles.
        List list = new ArrayList();
        Bundle[] bundles = getBundles();

        // Check all bundles to see who imports the package.
        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            BundleImpl bundle = (BundleImpl) bundles[bundleIdx];

            // Flag to short-circuit the loops if we find that
            // the current bundle does import the package.
            boolean doesImport = false;

            // Check the imports and exports of each bundle.
            String[] searchAttrs = {
                ImportSearchPolicy.IMPORTS_ATTR,
                ImportSearchPolicy.EXPORTS_ATTR
            };

            for (int attrIdx = 0;
                (!doesImport) && (attrIdx < searchAttrs.length);
                attrIdx++)
            {
                // Check all revisions of each bundle.
                Module[] modules = bundle.getInfo().getModules();
                for (int modIdx = 0;
                    (!doesImport) && (modIdx < modules.length);
                    modIdx++)
                {
                    Object[][] imports =
                        ImportSearchPolicy.getImportsOrExports(
                            modules[modIdx], searchAttrs[attrIdx]);
                    for (int importIdx = 0;
                        (!doesImport) && (importIdx < imports.length);
                        importIdx++)
                    {
                        // Get import package name.
                        String importName = (String)
                            imports[importIdx][ImportSearchPolicy.IDENTIFIER_IDX];
                        // Get resolving module.
                        Module resolvingModule = (Module)
                            imports[importIdx][ImportSearchPolicy.RESOLVING_MODULE_IDX];
                        // If the export and import package names are the same
                        // and the resolving module is associated with the
                        // exporting, then add current bundle to list.
                        if (exportName.equals(importName) &&
                            exporterInfo.hasModule(resolvingModule))
                        {
                            // Add the bundle to the list of importers.
                            list.add(bundles[bundleIdx]);
                            // Set the import flag so we exit the loops.
                            doesImport = true;
                        }
                    }
                }
            }
        }


        // Return the results.
        if (list.size() > 0)
        {
            return (Bundle[]) list.toArray(new Bundle[list.size()]);
        }

        return null;
    }

    protected void refreshPackages(Bundle[] targets)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        synchronized (m_adminLock)
        {
            // If targets is null, then refresh all pending bundles.
            if (targets == null)
            {
                ArrayList list = new ArrayList();

                // First add all uninstalled bundles.
                for (int i = 0;
                    (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
                    i++)
                {
                    list.add(m_uninstalledBundles[i]);
                }
                m_uninstalledBundles = null;

                // Then add all updated bundles.
                Iterator iter = m_installedBundleMap.values().iterator();
                while (iter.hasNext())
                {
                    BundleImpl bundle = (BundleImpl) iter.next();
                    if (bundle.getInfo().isRemovalPending())
                    {
                        list.add(bundle);
                    }
                }

                // Create an array.
                if (list.size() > 0)
                {
                    targets = (Bundle[]) list.toArray(new Bundle[list.size()]);
                }
            }

            // If there are targets, then find all dependencies
            // for each one.
            if (targets != null)
            {
                // Create map of bundles that import the packages
                // from the target bundles.
                HashMap map = new HashMap();
                for (int targetIdx = 0; targetIdx < targets.length; targetIdx++)
                {
                    BundleImpl target = (BundleImpl) targets[targetIdx];

                    // Add the current target bundle to the map of
                    // bundles to be refreshed using a RefreshHelper.
                    // Only these target bundles should be refreshed.
                    map.put(new Long(target.getBundleId()),
                        new RefreshHelper(target));
                    // Add all importing bundles to map.
                    populateImportGraph(target, map);
                }

                // At this point the map contains every bundle that has been
                // updated and/or removed as well as all bundles that import
                // packages from these bundles.
                for (Iterator iter = map.values().iterator(); iter.hasNext(); )
                {
                    RefreshHelper helper = (RefreshHelper) iter.next();
                    helper.stop();
                    helper.purgeOrRemove();
                    helper.reinitialize();
                }

                // Now restart bundles that were previously running.
                for (Iterator iter = map.values().iterator(); iter.hasNext(); )
                {
                    RefreshHelper helper = (RefreshHelper) iter.next();
                    helper.restart();
                }
            }
        }

        fireFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, getBundle(0), null);
    }

    private void populateImportGraph(BundleImpl target, HashMap map)
    {
        // Get the exported packages for the specified bundle.
        ExportedPackage[] pkgs = getExportedPackages(target);

        for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
        {
            // Get all imports of this package.
            Bundle[] importers = getImportingBundles(pkgs[pkgIdx]);

            // Add importing bundle to map if not already present.
            for (int impIdx = 0;
                (importers != null) && (impIdx < importers.length);
                impIdx++)
            {
                Long id = new Long(importers[impIdx].getBundleId());
                if (map.get(id) == null)
                {
                    map.put(id, new RefreshHelper(importers[impIdx]));
                    // Now recurse into each bundle to get its importers.
                    populateImportGraph(
                        (BundleImpl) importers[impIdx], map);
                }
            }
        }
    }

    /**
     * This method adds a import target to the IMPORTS_ATTR attribute
     * array associated with the specified module. If the module is
     * already validated, then this method will not add the new
     * import target if it will cause the specified module to become
     * invalidate. It is possible to "force" the method to add the
     * new import target, but doing so might cause module and modules
     * that depend on it to be invalidated.
     * @param module the module whose IMPORTS_ATTR meta-data is to be modified.
     * @param target the target to import.
     * @param version the version of the target to import.
     * @param force indicates whether to force the operation, even in the
     *        case where the module will be invalidated.
     * @return <tt>true</tt> if the import target was added, <tt>false</tt>
     *         otherwise.
    **/
    protected boolean addImport(
        Module module, Object target, Object version, boolean force)
    {
// TODO: Import permission check
        // Synchronize on the module manager, because we don't want
        // anything to change while we are in the middle of this
        // operation.
// TODO: Is this lock sufficient?
        synchronized (m_mgr)
        {
            ImportSearchPolicy search = (ImportSearchPolicy) m_mgr.getSearchPolicy();
            boolean added = false;
            Module exporter = null;

            // Get the valid attribute.
            boolean valid = ImportSearchPolicy.getValidAttribute(module).booleanValue();

            // Only attempt to resolve the new import target if the
            // module is already valid.
            if (valid)
            {
                exporter = search.resolveImportTarget(target, version);
            }

            // There are three situations that will cause us to add the
            // new import target to the existing module: 1) we are
            // being forced to do so, 2) the module is not currently
            // validated so adding imports is okay, or 3) the module
            // is currently valid and we were able to resolve the new
            // import target. The follow if-statement checks for these
            // three cases.
            if (force
                || !valid
                || (valid && (exporter != null)))
            {
                // Create a new imports attribute array and
                // copy the old values into it.
                Object[][] imports = ImportSearchPolicy.getImportsAttribute(module);
                Object[][] newImports = new Object[imports.length + 1][3];
                for (int i = 0; i < imports.length; i++)
                {
                    newImports[i] = imports[i];
                }

                // This is the new import target.
                newImports[newImports.length - 1] = new Object[] { target, version, exporter };
                module.setAttribute(ImportSearchPolicy.IMPORTS_ATTR, newImports);
                added = true;

                // If it was not possible to resolve the new import target
                // and the module is currently valid, then the module must
                // be invalidated.
                if ((exporter == null) && valid)
                {
                    search.invalidate(
                        module, module.getAttributes(), module.getResourceSources(),
                        module.getLibrarySources());
                }
            }

            return added;
        }
    }

    //
    // Implementations for Bundle interface methods.
    //

    /**
     * Implementation for Bundle.getBundleId().
    **/
    protected long getBundleId(BundleImpl bundle)
    {
        return bundle.getInfo().getBundleId();
    }

    /**
     * Implementation for Bundle.getHeaders().
    **/
    protected Dictionary getBundleHeaders(BundleImpl bundle)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }
        return new MapToDictionary(bundle.getInfo().getCurrentHeader());
    }

    /**
     * Implementation for Bundle.getLocation().
    **/
    protected String getBundleLocation(BundleImpl bundle)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }
        return bundle.getInfo().getLocation();
    }

    /**
     * Implementation for Bundle.getResource().
    **/
    protected URL getBundleResource(BundleImpl bundle, String name)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }
        return bundle.getInfo().getCurrentModule().getClassLoader().getResource(name);
// We previously search the old revisions of the bundle for resources
// first, but this caused multiple resolves when a bundle was updated
// but not resolved yet. I think the following is the better way, but
// other frameworks do it like above.
//        Module[] modules = bundle.getInfo().getModules();
//        for (int modIdx = 0; modIdx < modules.length; modIdx++)
//        {
//            URL url = modules[modIdx].getClassLoader().getResource(name);
//            if (url != null)
//            {
//                return url;
//            }
//        }
//        return null;
    }

    /**
     * Implementation for Bundle.getRegisteredServices().
    **/
    protected ServiceReference[] getBundleRegisteredServices(BundleImpl bundle)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        synchronized (m_quickLock)
        {
            BundleInfo info = bundle.getInfo();

            if (info.getServiceRegistrationCount() > 0)
            {
                // Create a list of service references.
                ArrayList list = new ArrayList();
                for (int regIdx = 0; regIdx < info.getServiceRegistrationCount(); regIdx++)
                {
                    // Get service registration.
                    ServiceRegistrationImpl reg = (ServiceRegistrationImpl)
                        info.getServiceRegistration(regIdx);

                    // Check that the current security context has permission
                    // to get at least one of the service interfaces; the
                    // objectClass property of the service stores its service
                    // interfaces.
                    boolean hasPermission = false;
                    if (System.getSecurityManager() != null)
                    {
                        String[] objectClass = (String[])
                            reg.getProperty(Constants.OBJECTCLASS);
                        if (objectClass == null)
                        {
                            return null;
                        }
                        for (int ifcIdx = 0;
                            !hasPermission && (ifcIdx < objectClass.length);
                            ifcIdx++)
                        {
                            try
                            {
                                ServicePermission perm =
                                    new ServicePermission(
                                        objectClass[ifcIdx], ServicePermission.GET);
                                AccessController.checkPermission(perm);
                                hasPermission = true;
                            }
                            catch (Exception ex)
                            {
                            }
                        }
                    }
                    else
                    {
                        hasPermission = true;
                    }

                    if (hasPermission)
                    {
                        list.add(reg.getReference());
                    }
                }

                if (list.size() > 0)
                {
                    return (ServiceReference[])
                        list.toArray(new ServiceReference[list.size()]);
                }
            }
        }

        return null;
    }

    /**
     * Implementation for Bundle.getServicesInUse().
    **/
    protected ServiceReference[] getBundleServicesInUse(BundleImpl bundle)
    {
        synchronized (m_quickLock)
        {
            BundleInfo info = bundle.getInfo();
            Iterator iter = info.getServiceUsageCounters();
            if (iter.hasNext())
            {
                // Create a list of service references.
                ArrayList list = new ArrayList();
                while (iter.hasNext())
                {
                    // Get service reference.
                    ServiceReference ref = (ServiceReference) iter.next();

                    // Check that the current security context has permission
                    // to get at least one of the service interfaces; the
                    // objectClass property of the service stores its service
                    // interfaces.
                    boolean hasPermission = false;
                    if (System.getSecurityManager() != null)
                    {
                        String[] objectClass = (String[])
                            ref.getProperty(Constants.OBJECTCLASS);
                        if (objectClass == null)
                        {
                            return null;
                        }
                        for (int i = 0;
                            !hasPermission && (i < objectClass.length);
                            i++)
                        {
                            try
                            {
                                ServicePermission perm =
                                    new ServicePermission(
                                        objectClass[i], ServicePermission.GET);
                                AccessController.checkPermission(perm);
                                hasPermission = true;
                            }
                            catch (Exception ex)
                            {
                            }
                        }
                    }
                    else
                    {
                        hasPermission = true;
                    }

                    if (hasPermission)
                    {
                        list.add(ref);
                    }
                }

                if (list.size() > 0)
                {
                    return (ServiceReference[])
                        list.toArray(new ServiceReference[list.size()]);
                }
            }
        }

        return null;
    }

    /**
     * Implementation for Bundle.getState().
    **/
    protected int getBundleState(BundleImpl bundle)
    {
        return bundle.getInfo().getState();
    }

    /**
     * Implementation for Bundle.hasPermission().
    **/
    protected boolean bundleHasPermission(BundleImpl bundle, Object obj)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        return true;
    }

    /**
     * Implementation for Bundle.start().
    **/
    protected void startBundle(BundleImpl bundle)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        synchronized (m_adminLock)
        {
            // Set the bundle's persistent state to active.
            bundle.getInfo().setPersistentStateActive();

            try
            {
                // Try to start the bundle.
                startBundleWithStartLevel(bundle);
            }
            catch (Throwable th)
            {
                // Reset the bundle's persistent state to inactive if
                // there was an error.
                bundle.getInfo().setPersistentStateInactive();

th.printStackTrace();

                // The spec says to expect BundleException,
                // IllegalStateException, or SecurityException,
                // so rethrow these exceptions.
                if (th instanceof BundleException)
                {
                    throw (BundleException) th;
                }
                else if (th instanceof IllegalStateException)
                {
                    throw (IllegalStateException) th;
                }
                else if (th instanceof SecurityException)
                {
                    throw (SecurityException) th;
                }
                // Convert a privileged action exception to the
                // nested exception.
                else if (th instanceof PrivilegedActionException)
                {
                    th = ((PrivilegedActionException) th).getException();
                }

                // Rethrow all other exceptions as a BundleException.
                throw new BundleException("Activator start error.", th);
            }

            // Save the bundle's persistent state.
            try
            {
                m_cache.getArchive(bundle.getInfo().getBundleId())
                    .setPersistentState(bundle.getInfo().getPersistentState());
            }
            catch (Exception ex)
            {
                Oscar.error("Oscar: Error saving persistent bundle state.");
                Oscar.error("Oscar: " + ex);
            }
        }
    }

    /**
     * This method performs the actual task of starting a bundle and is
     * used by <tt>Oscar.startBundle()</tt> and <tt>Oscar.setStartLevel</tt>.
     * When <tt>Oscar.startBundle()</tt> is called, the bundle's persistent
     * state is set to active, while calling this method directly does
     * not affect the bundle's persistent state. This is necessary when
     * the framework's start level changes, because it may be necessary to
     * start bundles without affecting their persistent state. If the specified
     * bundle's start level is greater than the framework's start level, then
     * this method simply returns.
    **/
    private void startBundleWithStartLevel(BundleImpl bundle)
        throws Throwable
    {
        BundleInfo info = bundle.getInfo();

        // Ignore bundles whose persistent state is not active
        // or whose start level is greater than the framework's.
        if ((info.getPersistentState() != Bundle.ACTIVE)
            || (info.getStartLevel(getInitialBundleStartLevel()) > getStartLevel()))
        {
            return;
        }

        switch (info.getState())
        {
            case Bundle.UNINSTALLED:
                throw new IllegalStateException("Cannot start an uninstalled bundle.");
            case Bundle.STARTING:
            case Bundle.STOPPING:
                throw new BundleException("Starting a bundle that is starting or stopping is currently not supported.");
            case Bundle.ACTIVE:
                return;
            case Bundle.INSTALLED:
                resolveBundle(bundle);
            case Bundle.RESOLVED:
                info.setState(Bundle.STARTING);
        }

        try
        {
            // Set the bundle's activator.
            bundle.getInfo().setActivator(createBundleActivator(bundle.getInfo()));

            // Activate the bundle if it has an activator.
            if (bundle.getInfo().getActivator() != null)
            {
                if (info.getContext() == null)
                {
                    info.setContext(new BundleContextImpl(this, bundle));
                }

                if (System.getSecurityManager() != null)
                {
                    m_startStopPrivileged.setAction(StartStopPrivileged.START_ACTION);
                    m_startStopPrivileged.setBundle(bundle);
                    AccessController.doPrivileged(m_startStopPrivileged);
                }
                else
                {
                    info.getActivator().start(info.getContext());
                }
            }
        }
        catch (Throwable th)
        {
            // If there was an error starting the bundle,
            // then reset its state to RESOLVED.
            info.setState(Bundle.RESOLVED);

            // Unregister any services offered by this bundle.
            unregisterServices(bundle);

            // Release any services being used by this bundle.
            ungetServices(bundle);

            // Remove any listeners registered by this bundle.
            removeListeners(bundle);

            throw th;
        }

        info.setState(Bundle.ACTIVE);

        fireBundleEvent(BundleEvent.STARTED, bundle);
    }

    /**
     * Implementation for Bundle.update().
    **/
    protected void updateBundle(BundleImpl bundle, InputStream is)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        // We guarantee to close the input stream, so put it in a
        // finally clause.

        try
        {
            // Variable to indicate whether bundle is active or not.
            boolean activated = false;
            Exception rethrow = null;

            synchronized (m_adminLock)
            {
                BundleInfo info = bundle.getInfo();

                if (info.getState() == Bundle.UNINSTALLED)
                {
                    throw new IllegalStateException("The bundle is uninstalled.");
                }

                // First get the update-URL from our header.
                String updateLocation = (String)
                    info.getCurrentHeader().get(Constants.BUNDLE_UPDATELOCATION);

                // If no update location specified, use original location.
                if (updateLocation == null)
                {
                    updateLocation = info.getLocation();
                }

                // Remember if active.
                activated = (info.getState() == Bundle.ACTIVE);

                // If the bundle is active, stop it.
                if (activated)
                {
                    stopBundle(bundle);
                }

                try
                {
                    // Get the URL input stream if necessary.
                    if (is == null)
                    {
                        // Do it the manual way to have a chance to 
                        // set request properties such as proxy auth.
                        URL url = new URL(updateLocation);
                        URLConnection conn = url.openConnection(); 

                        // Support for http proxy authentication.
                        String auth = System.getProperty("http.proxyAuth");
                        if ((auth != null) && (auth.length() > 0))
                        {
                            if ("http".equals(url.getProtocol()) ||
                                "https".equals(url.getProtocol()))
                            {
                                String base64 = Util.base64Encode(auth);
                                conn.setRequestProperty(
                                    "Proxy-Authorization", "Basic " + base64);
                            }
                        }
                        is = conn.getInputStream();
                    }
                    // Get the bundle's archive.
                    BundleArchive archive = m_cache.getArchive(info.getBundleId());
                    // Update the bundle; this operation will increase
                    // the revision count for the bundle.
                    m_cache.update(archive, is);
                    // Create a module for the new revision; the revision is
                    // base zero, so subtract one from the revision count to
                    // get the revision of the new update.
                    Module module = createModule(
                        info.getBundleId(),
                        archive.getRevisionCount() - 1,
                        info.getCurrentHeader());
                    // Add module to bundle info.
                    info.addModule(module);
                }
                catch (Exception ex)
                {
                    Oscar.error("Unable to update the bundle.");
                    rethrow = ex;
                }
                finally
                {
                    try
                    {
                        if (is != null) is.close();
                    }
                    catch (Exception ex)
                    {
                        Oscar.error("Unable to close input stream: " + ex);
                    }
                }

                info.setState(Bundle.INSTALLED);

                // Mark as needing a refresh.
                info.setRemovalPending();
            }

            // Fire updated event if successful.
            if (rethrow == null)
            {
                // Fire bundle update event outside of synchronized block
                // if no error occured.
                fireBundleEvent(BundleEvent.UPDATED, bundle);
            }

            // Start if previously active.
            if (activated)
            {
                // Restart bundle if previously active.
                startBundle(bundle);
            }

            // If update failed, rethrow exception.
            if (rethrow != null)
            {
                throw new BundleException("Update failed.", rethrow);
            }
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                Oscar.error("Oscar: Could not close update stream.");
                // What else can we do?
            }
        }
    }

    /**
     * Implementation for Bundle.stop().
    **/
    protected void stopBundle(BundleImpl bundle)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        Throwable rethrow = null;

        synchronized (m_adminLock)
        {
            // Set the bundle's persistent state to inactive.
            bundle.getInfo().setPersistentStateInactive();

            try
            {
                // Stop the bundle.
                stopBundleWithStartLevel(bundle);
            }
            catch (Throwable th)
            {
                Oscar.error("Oscar: Error calling activator.", th);
                rethrow = th;
            }

            // Save the bundle's persistent state.
            try
            {
                m_cache.getArchive(bundle.getInfo().getBundleId())
                    .setPersistentState(bundle.getInfo().getPersistentState());
            }
            catch (Exception ex)
            {
                Oscar.error("Oscar: Error saving persistent bundle state.");
                Oscar.error("Oscar: " + ex);
            }
        }

        // Throw activator error if there was one.
        if (rethrow != null)
        {
            // The spec says to expect BundleException,
            // IllegalStateException, or SecurityException,
            // so rethrow these exceptions.
            if (rethrow instanceof BundleException)
            {
                throw (BundleException) rethrow;
            }
            else if (rethrow instanceof IllegalStateException)
            {
                throw (IllegalStateException) rethrow;
            }
            else if (rethrow instanceof SecurityException)
            {
                throw (SecurityException) rethrow;
            }
            else if (rethrow instanceof PrivilegedActionException)
            {
                rethrow = ((PrivilegedActionException) rethrow).getException();
            }

            // Rethrow all other exceptions as a BundleException.
            throw new BundleException("Activator stop error.", rethrow);
        }
    }

    /**
     * This method performs the actual task of stopping a bundle and is
     * used by <tt>Oscar.stopBundle()</tt> and <tt>Oscar.setStartLevel</tt>.
     * When <tt>Oscar.stopBundle()</tt> is called, the bundle's persistent
     * state is set to inactive, while calling this method directly does
     * not affect the bundle's persistent state. This is necessary when
     * the framework's start level changes, because it may be necessary to
     * stop bundles without affecting their persistent state.
    **/
    private void stopBundleWithStartLevel(BundleImpl bundle)
        throws Throwable
    {
        BundleInfo info = bundle.getInfo();

        switch (info.getState())
        {
            case Bundle.UNINSTALLED:
                throw new IllegalStateException("Cannot stop an uninstalled bundle.");
            case Bundle.STARTING:
            case Bundle.STOPPING:
                throw new BundleException("Stopping a bundle that is starting or stopping is currently not supported.");
            case Bundle.INSTALLED:
            case Bundle.RESOLVED:
                return;
            case Bundle.ACTIVE:
                // Set bundle state..
                info.setState(Bundle.STOPPING);
        }

        Throwable rethrow = null;

        try
        {
            if (bundle.getInfo().getActivator() != null)
            {
                if (System.getSecurityManager() != null)
                {
                    m_startStopPrivileged.setAction(StartStopPrivileged.STOP_ACTION);
                    m_startStopPrivileged.setBundle(bundle);
                    AccessController.doPrivileged(m_startStopPrivileged);
                }
                else
                {
                    info.getActivator().stop(info.getContext());
                }
            }

            // Try to save the activator in the cache.
            // NOTE: This is non-standard OSGi behavior and only
            // occurs if strictness is disabled.
            String strict = getConfigProperty(OscarConstants.STRICT_OSGI_PROP);
            boolean isStrict = (strict == null) ? true : strict.equals("true");
            if (!isStrict)
            {
                try
                {
                    m_cache.getArchive(info.getBundleId())
                        .setActivator(info.getActivator());
                }
                catch (Exception ex)
                {
                    // Problem saving activator, so ignore it.
                    // TODO: Perhaps we should handle this some other way?
                }
            }
        }
        catch (Throwable th)
        {
            // TODO: Make sure we clean up everything here.
th.printStackTrace();
            rethrow = th;
        }


        // Unregister any services offered by this bundle.
        unregisterServices(bundle);

        // Release any services being used by this bundle.
        ungetServices(bundle);

        // The spec says that we must remove all event
        // listeners for a bundle when it is stopped.
        removeListeners(bundle);

        info.setState(Bundle.RESOLVED);
        fireBundleEvent(BundleEvent.STOPPED, bundle);

        if (rethrow != null)
        {
            throw rethrow;
        }
    }

    /**
     * Implementation for Bundle.uninstall().
    **/
    protected void uninstallBundle(BundleImpl bundle)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        BundleException rethrow = null;

        synchronized (m_adminLock)
        {
            BundleInfo info = bundle.getInfo();
            if (info.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("The bundle is uninstalled.");
            }

            // The spec says that uninstall should always succeed, so
            // catch an exception here if stop() doesn't succeed and
            // rethrow it at the end.
            try
            {
                stopBundle(bundle);
            }
            catch (BundleException ex)
            {
                rethrow = ex;
            }

            // Get the quick lock to remove the bundle from the
            // installed map.
            BundleImpl target = null;
            synchronized (m_quickLock)
            {
                target = (BundleImpl) m_installedBundleMap.remove(
                    info.getLocation());
            }

            // Finally, put the uninstalled bundle into the
            // uninstalled list for subsequent refreshing.
            if (target != null)
            {
                // Set the bundle's persistent state to uninstalled.
                target.getInfo().setPersistentStateUninstalled();

                // Mark bundle for removal.
                target.getInfo().setRemovalPending();

                // Put bundle in uninstalled bundle array.
                BundleImpl[] bundles = null;
                if (m_uninstalledBundles == null)
                {
                    bundles = new BundleImpl[1];
                }
                else
                {
                    bundles = new BundleImpl[m_uninstalledBundles.length + 1];
                    System.arraycopy(
                        m_uninstalledBundles, 0, bundles, 0, bundles.length - 1);
                }
                bundles[bundles.length - 1] = target;
                m_uninstalledBundles = bundles;
            }
            else
            {
                Oscar.error("Unable to remove bundle from installed map!");
            }

            // Set state to uninstalled.
            info.setState(Bundle.UNINSTALLED);
        }

        // Fire bundle event outside synchronized block.
        fireBundleEvent(BundleEvent.UNINSTALLED, bundle);

        if (rethrow != null)
        {
            throw rethrow;
        }
    }

    //
    // Implementations for BundleContext interface methods.
    //

    /**
     * Implementation for BundleContext.getProperty(). Returns
     * environment property associated with the container.
     *
     * @param key the name of the property to retrieve.
     * @return the value of the specified property or null.
    **/
    protected String getProperty(String key)
    {
        // Property names are case insensitive.
        if (m_frameworkPropMap == null)
        {
            return null;
        }
        String val = (String) m_frameworkPropMap.get(key);
        return substVars(val);
    }

    /**
     * Set the environment property associated with the container.
     *
     * @param key the name of the property to set.
     * @param value the value of the specified property.
    **/
    protected void setProperty(String key, String value)
    {
        // Property names are case insensitive.
        if (m_frameworkPropMap != null)
        {
            m_frameworkPropMap.put(key, value);
        }
    }

    /**
     * Implementation for BundleContext.installBundle(). Installs the bundle
     * associated with the location string, using the specified input stream
     * if is it not null; the input stream will be closed at the end of this
     * method.
     *
     * @param location the location string (URL) for the bundle.
     * @param is input stream from which to read the bundle, can be null.
     * @return a reference to the installed bundle.
     * @throws BundleException if any problems are encountered during installation.
    **/
    protected Bundle installBundle(String location, InputStream is)
        throws BundleException
    {
        return installBundle(-1, location, is);
    }

    private Bundle installBundle(long id, String location, InputStream is)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        BundleImpl bundle = null;

        synchronized (m_adminLock)
        {
            try
            {
                // Check to see if the framework is still running;
                if ((getFrameworkStatus() == Oscar.STOPPING_STATUS) ||
                    (getFrameworkStatus() == Oscar.UNKNOWN_STATUS))
                {
                    throw new BundleException("The framework has been shutdown.");
                }

                // If bundle location is already installed, then
                // return it as required by the OSGi specification.
                bundle = (BundleImpl) getBundle(location);
                if (bundle != null)
                {
                    return bundle;
                }

                // Determine if this is a new or existing bundle.
                boolean isNew = (id < 0);

                // If the bundle is new we must cache its JAR file.
                if (isNew)
                {
                    // First generate an identifier for it.
                    id = getNextId();

                    try
                    {
                        // Get the URL input stream if necessary.
                        if (is == null)
                        {
                            // Do it the manual way to have a chance to 
                            // set request properties such as proxy auth.
                            URL url = new URL(location);
                            URLConnection conn = url.openConnection(); 

                            // Support for http proxy authentication.
                            String auth = System.getProperty("http.proxyAuth");
                            if ((auth != null) && (auth.length() > 0))
                            {
                                if ("http".equals(url.getProtocol()) ||
                                    "https".equals(url.getProtocol()))
                                {
                                    String base64 = Util.base64Encode(auth);
                                    conn.setRequestProperty(
                                        "Proxy-Authorization", "Basic " + base64);
                                }
                            }
                            is = conn.getInputStream();
                        }
                        // Add the bundle to the cache.
                        m_cache.create(id, location, is);
                    }
                    catch (Exception ex)
                    {
                        throw new BundleException("Unable to cache bundle: " + location, ex);
                    }
                    finally
                    {
                        try
                        {
                            if (is != null) is.close();
                        }
                        catch (IOException ex)
                        {
                            Oscar.error("Unable to close input stream: " + ex);
                        }
                    }
                }
                else
                {
                    // If the bundle we are installing is not new,
                    // then try to purge old revisions before installing
                    // it; this is done just in case a "refresh"
                    // didn't occur last session...this would only be
                    // due to an error or system crash.
                    try
                    {
                        if (m_cache.getArchive(id).getRevisionCount() > 1)
                        {
                            m_cache.purge(m_cache.getArchive(id));
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        Oscar.error("Oscar: Could not purge bundle.", ex);
                    }
                }

                try
                {
                    BundleArchive archive = m_cache.getArchive(id);
                    bundle = new BundleImpl(this, createBundleInfo(archive));
                }
                catch (Exception ex)
                {
                    // If the bundle is new, then remove it from the cache.
                    // TODO: Perhaps it should be removed if it is not new
                    // too.
                    if (isNew)
                    {
                        try
                        {
                            m_cache.remove(m_cache.getArchive(id));
                        }
                        catch (Exception ex1)
                        {
                            Oscar.error("Could not remove from cache.", ex1);
                        }
                    }
                    throw new BundleException("Could not create bundle object: " + ex);
                }

                // If the bundle is new, then persistently set its
                // start level; existing bundles already have their
                // start level set.
                if (isNew)
                {
                    setBundleStartLevel(bundle, getInitialBundleStartLevel());
                }

                synchronized (m_quickLock)
                {
                    m_installedBundleMap.put(location, bundle);
                }
            }
            finally
            {
                // Always try to close the input stream.
                try
                {
                    if (is != null) is.close();
                }
                catch (IOException ex)
                {
                    Oscar.error("Oscar: Unable to close input stream.");
                    // Not much else we can do.
                }
            }
        }

        // Fire bundle event outside synchronized block.
        fireBundleEvent(BundleEvent.INSTALLED, bundle);

        // Return new bundle.
        return bundle;
    }

    /**
     * Implementation for BundleContext.getBundle(). Retrieves a
     * bundle from its identifier.
     *
     * @param id the identifier of the bundle to retrieve.
     * @return the bundle associated with the identifier or null if there
     *         is no bundle associated with the identifier.
    **/
    protected Bundle getBundle(long id)
    {
        synchronized (m_quickLock)
        {
            BundleImpl bundle = null;

            for (Iterator i = m_installedBundleMap.values().iterator(); i.hasNext(); )
            {
                bundle = (BundleImpl) i.next();
                if (bundle.getInfo().getBundleId() == id)
                {
                    return bundle;
                }
            }
        }

        return null;
    }

    // Private member for method below.
    private Comparator m_comparator = null;

    /**
     * Implementation for BundleContext.getBundles(). Retrieves
     * all installed bundles.
     *
     * @return an array containing all installed bundles or null if
     *         there are no installed bundles.
    **/
    protected Bundle[] getBundles()
    {
        if (m_comparator == null)
        {
            m_comparator = new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    Bundle b1 = (Bundle) o1;
                    Bundle b2 = (Bundle) o2;
                    if (b1.getBundleId() > b2.getBundleId())
                        return 1;
                    else if (b1.getBundleId() < b2.getBundleId())
                        return -1;
                    return 0;
                }
            };
        }

        Bundle[] bundles = null;

        synchronized (m_quickLock)
        {
            if (m_installedBundleMap.size() == 0)
            {
                return null;
            }

            bundles = new Bundle[m_installedBundleMap.size()];
            int counter = 0;
            for (Iterator i = m_installedBundleMap.values().iterator(); i.hasNext(); )
            {
                bundles[counter++] = (Bundle) i.next();
            }
        }

        Arrays.sort(bundles, m_comparator);

        return bundles;
    }

    /**
     * Implementation for BundleContext.addBundleListener().
     * Adds bundle listener to the listener list so that it
     * can listen for <code>BundleEvent</code>s.
     *
     * @param bundle the bundle that registered the listener.
     * @param l the bundle listener to add to the listener list.
    **/
    protected void addBundleListener(Bundle bundle, BundleListener l)
    {
        // The spec says do nothing if the listener is
        // already registered.
        BundleListenerWrapper old = (BundleListenerWrapper)
            m_dispatchQueue.getListener(BundleListener.class, l);
        if (old == null)
        {
            l = new BundleListenerWrapper(bundle, l);
            m_dispatchQueue.addListener(BundleListener.class, l);
        }
    }

    /**
     * Implementation for BundleContext.removeBundleListener().
     * Removes bundle listeners from the listener list.
     *
     * @param l the bundle listener to remove from the listener list.
    **/
    protected void removeBundleListener(BundleListener l)
    {
        m_dispatchQueue.removeListener(BundleListener.class, l);
    }

    /**
     * Implementation for BundleContext.addServiceListener().
     * Adds service listener to the listener list so that is
     * can listen for <code>ServiceEvent</code>s.
     *
     * @param bundle the bundle that registered the listener.
     * @param l the service listener to add to the listener list.
     * @param f the filter for the listener; may be null.
    **/
    protected void addServiceListener(Bundle bundle, ServiceListener l, String f)
        throws InvalidSyntaxException
    {
        // The spec says if the listener is already registered,
        // then replace filter.
        ServiceListenerWrapper old = (ServiceListenerWrapper)
            m_dispatchQueue.getListener(ServiceListener.class, l);
        if (old != null)
        {
            old.setFilter((f == null) ? null : new FilterImpl(f));
        }
        else
        {
            l = new ServiceListenerWrapper(
                bundle, l, (f == null) ? null : new FilterImpl(f));
            m_dispatchQueue.addListener(ServiceListener.class, l);
        }
    }

    /**
     * Implementation for BundleContext.removeServiceListener().
     * Removes service listeners from the listener list.
     *
     * @param l the service listener to remove from the listener list.
    **/
    protected void removeServiceListener(ServiceListener l)
    {
        m_dispatchQueue.removeListener(ServiceListener.class, l);
    }

    /**
     * Implementation for BundleContext.addFrameworkListener().
     * Adds framework listener to the listener list so that it
     * can listen for <code>FrameworkEvent</code>s.
     *
     * @param bundle the bundle that registered the listener.
     * @param l the framework listener to add to the listener list.
    **/
    protected void addFrameworkListener(Bundle bundle, FrameworkListener l)
    {
        // The spec says do nothing if the listener is
        // already registered.
        FrameworkListenerWrapper old = (FrameworkListenerWrapper)
            m_dispatchQueue.getListener(FrameworkListener.class, l);
        if (old == null)
        {
            l = new FrameworkListenerWrapper(bundle, l);
            m_dispatchQueue.addListener(FrameworkListener.class, l);
        }
    }

    /**
     * Implementation for BundleContext.removeFrameworkListener().
     * Removes framework listeners from the listener list.
     *
     * @param l the framework listener to remove from the listener list.
    **/
    protected void removeFrameworkListener(FrameworkListener l)
    {
        m_dispatchQueue.removeListener(FrameworkListener.class, l);
    }

    /**
     * Implementation for BundleContext.registerService(). Registers
     * a service for the specified bundle bundle.
     *
     * @param clazzes a string array containing the names of the classes
     *                under which the new service is available.
     * @param svcObj the service object or <code>ServiceFactory</code>.
     * @param dict a dictionary of properties that further describe the
     *             service or null.
     * @return a <code>ServiceRegistration</code> object or null.
    **/
    protected ServiceRegistration registerService(
        BundleImpl bundle, String[] classNames, Object svcObj, Dictionary dict)
    {
        if (classNames == null)
        {
            throw new NullPointerException("Service class names cannot be null.");
        }
        else if (svcObj == null)
        {
            throw new IllegalArgumentException("Service object cannot be null.");
        }

        // Check for permission to register all passed in interface names.
        if (System.getSecurityManager() != null)
        {
            for (int i = 0; i < classNames.length; i++)
            {
                ServicePermission perm = new ServicePermission(
                    classNames[i], ServicePermission.REGISTER);
                AccessController.checkPermission(perm);
            }
        }

        ServiceRegistrationImpl reg = null;

        synchronized (m_quickLock)
        {
            BundleInfo info = bundle.getInfo();

            // Can only register services if starting or active.
            if ((info.getState() & (Bundle.STARTING | Bundle.ACTIVE)) == 0)
            {
                throw new IllegalStateException(
                    "Can only register services while bundle is active or activating.");
            }

            // Check to make sure that the service object is
            // an instance of all service classes; ignore if
            // service object is a service factory.
            if (!(svcObj instanceof ServiceFactory))
            {
                String pkgName = null;
                Class clazz = null;
                // Get the class loader from the service object.
                ClassLoader loader = svcObj.getClass().getClassLoader();
                // A null class loader represents the system class loader.
                loader = (loader == null)
                    ? ClassLoader.getSystemClassLoader() : loader;
                for (int i = 0; i < classNames.length; i++)
                {
                    try
                    {
                        clazz = loader.loadClass(classNames[i]);
                    }
                    catch (ClassNotFoundException ex)
                    {
                        throw new IllegalArgumentException("Class not found: " + ex);
                    }
                    if (!clazz.isAssignableFrom(svcObj.getClass()))
                    {
                        throw new IllegalArgumentException(
                            "Service object is not an instance of \""
                            + classNames[i] + "\".");
                    }
                }
            }

            reg = new ServiceRegistrationImpl(
                this, bundle, classNames,
                new Long(getNextServiceId()), svcObj, dict);

            info.addServiceRegistration(reg);
        }

        // Fire event outside synchronized block.
        fireServiceEvent(ServiceEvent.REGISTERED, reg.getReference());

        return reg;
    }

    /**
     * Implementation for BundleContext.getServiceReferences().
    **/
    protected ServiceReference[] getServiceReferences(String className, String expr)
        throws InvalidSyntaxException
    {
        Oscar.debug("Oscar.getServiceReferences(" + className + ", " + expr + ")");

        // If the specified class name is not null, then check for
        // permission to get the associated service interface.
        if ((className != null) && (System.getSecurityManager() != null))
        {
            try
            {
                ServicePermission perm =
                    new ServicePermission(className, ServicePermission.GET);
                AccessController.checkPermission(perm);
            }
            catch (Exception ex)
            {
                // We do not throw this exception since the bundle
                // is not supposed to know about the service at all
                // if it does not have permission.
                Oscar.error(ex.getMessage());
                return null;
            }
        }

        // Define filter if expression is not null.
        Filter filter = null;
        if (expr != null)
        {
            filter = new FilterImpl(expr);
        }

        synchronized (m_quickLock)
        {
            // Create a filtered list of service references.
            ArrayList list = new ArrayList();
            // Iterator over all bundles.
            for (Iterator i = m_installedBundleMap.values().iterator(); i.hasNext(); )
            {
                BundleImpl bundle = (BundleImpl) i.next();

                // Do not look at bundles that are stopping.
                if (bundle.getInfo().getState() != Bundle.STOPPING)
                {
                    // Loop through all registered services for each bundle.
                    ServiceReference[] refs = getBundleRegisteredServices(bundle);
                    for (int refIdx = 0;
                        (refs != null) && (refIdx < refs.length);
                        refIdx++)
                    {
                        // Determine if the registered services matches
                        // the search criteria.
                        boolean matched = false;

                        // If className is null, then look at filter only.
                        if ((className == null) &&
                            ((filter == null) || filter.match(refs[refIdx])))
                        {
                            boolean hasPermission = true;
                            // Since the class name is null, we need to check
                            // here for permission to get each service interface.
                            if (System.getSecurityManager() != null)
                            {
                                String[] objectClass = (String[])
                                    refs[refIdx].getProperty(OscarConstants.OBJECTCLASS);
                                for (int classIdx = 0;
                                    classIdx < objectClass.length;
                                    classIdx++)
                                {
                                    try
                                    {
                                        ServicePermission perm = new ServicePermission(
                                            objectClass[classIdx], ServicePermission.GET);
                                        AccessController.checkPermission(perm);
                                        // The bundle only needs permission for one
                                        // of the service interfaces, so break out
                                        // of the loop when permission is granted.
                                        break;
                                    }
                                    catch (Exception ex)
                                    {
                                        // We do not throw this exception since the bundle
                                        // is not supposed to know about the service at all
                                        // if it does not have permission.
                                        Oscar.error(ex.getMessage());
                                        hasPermission = false;
                                        break;
                                    }
                                }
                            }
                            matched = hasPermission;
                        }
                        // If className is not null, then first match the
                        // objectClass property before looking at the
                        // filter.
                        else if (className != null)
                        {
                            String[] objectClass = (String[])
                                refs[refIdx].getProperty(OscarConstants.OBJECTCLASS);
                            for (int classIdx = 0;
                                classIdx < objectClass.length;
                                classIdx++)
                            {
                                if (objectClass[classIdx].equals(className) &&
                                    ((filter == null) || filter.match(refs[refIdx])))
                                {
                                    matched = true;
                                    break;
                                }
                            }
                        }

                        // Add reference if it was a match.
                        if (matched)
                        {
                            list.add(refs[refIdx]);
                        }
                    }
                }
            }

            if (list.size() > 0)
            {
                ServiceReference[] refs = new ServiceReference[list.size()];
                for (int i = 0; i < list.size(); i++)
                {
                    refs[i] = (ServiceReference) list.get(i);
                }
                return refs;
            }
        }

        return null;
    }

    /**
     * Implementation for BundleContext.getService().
    **/
    protected Object getService(BundleImpl bundle, ServiceReference ref)
    {
        synchronized (m_quickLock)
        {
            // Check that the bundle has permission to get at least
            // one of the service interfaces; the objectClass property
            // of the service stores its service interfaces.
            String[] objectClass = (String[])
                ref.getProperty(Constants.OBJECTCLASS);
            if (objectClass == null)
            {
                return null;
            }

            boolean hasPermission = false;
            if (System.getSecurityManager() != null)
            {
                for (int i = 0;
                    !hasPermission && (i < objectClass.length);
                    i++)
                {
                    try
                    {
                        ServicePermission perm =
                            new ServicePermission(
                                objectClass[i], ServicePermission.GET);
                        AccessController.checkPermission(perm);
                        hasPermission = true;
                    }
                    catch (Exception ex)
                    {
                    }
                }
            }
            else
            {
                hasPermission = true;
            }

            // If the bundle does not permission to access the service,
            // then return null.
            if (!hasPermission)
            {
                return null;
            }

            // Get the service registration if it is still valid.
            ServiceRegistrationImpl reg =
                ((ServiceReferenceImpl) ref).getServiceRegistration().isValid()
                ? ((ServiceReferenceImpl) ref).getServiceRegistration()
                : null;

            BundleInfo clientInfo = bundle.getInfo();

            // If the service registration is not valid, then this means
            // that the service provider unregistered the service. The spec
            // says that calls to get an unregistered service should always
            // return null (assumption: even if it is currently cached
            // by the bundle). So in this case, flush the service reference
            // from the cache and return null.
            if (reg == null)
            {
                // Remove service reference from usage cache.
                clientInfo.removeServiceUsageCounter(ref);

                // It is not necessary to unget the service object from
                // the providing bundle, since the associated service is
                // unregistered and hence not in the list of registered services
                // of the providing bundle. This is precisely why the service
                // registration was not found above in the first place.

                return null;
            }

            // Get the usage count, if any.
            BundleInfo.UsageCounter usage = clientInfo.getServiceUsageCounter(ref);

            // If the service object is cached, then increase the usage
            // count and return the cached service object.
            Object svcObj = null;
            if (usage != null)
            {
                usage.m_count++;
                svcObj = usage.m_svcObj;
            }
            else
            {
                // Get service object from service registration.
                svcObj = reg.getService(bundle);

                // Cache the service object.
                if (svcObj != null)
                {
                    usage = new BundleInfo.UsageCounter();
                    usage.m_svcObj = svcObj;
                    usage.m_count++;
                    clientInfo.putServiceUsageCounter(ref, usage);
                }
            }

            return svcObj;
        }
    }

    /**
     * Implementation for BundleContext.ungetService().
    **/
    protected Object ungetService(BundleImpl bundle, ServiceReference ref)
    {
        synchronized (m_quickLock)
        {
            BundleInfo clientInfo = bundle.getInfo();
            BundleInfo.UsageCounter usage = null;

            // Get current usage count.
            usage = clientInfo.getServiceUsageCounter(ref);

            // If no usage count, then return.
            if (usage == null)
            {
                return null;
            }

            // Decrement usage count.
            usage.m_count--;

            // Remove reference when usage count goes to zero
            // and unget the service object from the exporting
            // bundle.
            if (usage.m_count == 0)
            {
                clientInfo.removeServiceUsageCounter(ref);
                ServiceRegistrationImpl reg =
                    ((ServiceReferenceImpl) ref).getServiceRegistration();
                reg.ungetService(bundle, usage.m_svcObj);
                usage.m_svcObj = null;
            }

            // Always return the service object.
            return usage.m_svcObj;
        }
    }

    /**
     * Implementation for BundleContext.getDataFile().
    **/
    protected File getBundleDataFile(BundleImpl bundle, String name)
        throws IllegalStateException
    {
        // The spec says to throw an error if the bundle
        // is stopped, which I assume means not active,
        // starting, or stopping.
        if ((bundle.getInfo().getState() != Bundle.ACTIVE) &&
            (bundle.getInfo().getState() != Bundle.STARTING) &&
            (bundle.getInfo().getState() != Bundle.STOPPING))
        {
            throw new IllegalStateException("Only active bundles can create files.");
        }
        try
        {
            return m_cache.getArchive(
                bundle.getInfo().getBundleId()).getDataFile(name);
        }
        catch (Exception ex)
        {
            Oscar.error(ex.getMessage());
            return null;
        }
    }

    //
    // Miscellaneous management methods.
    //

    private BundleInfo createBundleInfo(BundleArchive archive)
        throws Exception
    {
        // Get the bundle manifest.
        Map headerMap = null;
        try
        {
            // Although there should only ever be one revision at this
            // point, get the header for the current revision to be safe.
            headerMap = archive.getManifestHeader(archive.getRevisionCount() - 1);
        }
        catch (Exception ex)
        {
            throw new BundleException("Unable to read JAR manifest.", ex);
        }

        // We can't do anything without the manifest header.
        if (headerMap == null)
        {
            throw new BundleException("Unable to read JAR manifest header.");
        }

        // Create the module for the bundle; although there should only
        // ever be one revision at this point, create the module for
        // the current revision to be safe.
        Module module = createModule(
            archive.getId(), archive.getRevisionCount() - 1, headerMap);

        // Finally, create an return the bundle info.
        return new BundleInfo(archive, module);
    }

    /**
     * Creates a module for a given bundle by reading the bundle's
     * manifest meta-data and converting it to work with the underlying
     * import/export search policy of the module loader.
     * @param id the identifier of the bundle for which the module should
     *        be created.
     * @param headers the headers map associated with the bundle.
     * @return the initialized and/or newly created module.
    **/
    private Module createModule(long id, int revision, Map headerMap)
        throws Exception
    {
        // Create the resource sources for the bundle. The resource sources
        // are comprised of the bundle's class path values (as JarResourceSources).
        ResourceSource[] resSources = null;
        try
        {
            // Get bundle class path for the specified revision from cache.
            String[] classPath = m_cache.getArchive(id).getClassPath(revision);

            // Create resource sources for everything.
            resSources = new ResourceSource[classPath.length];
            for (int i = 0; i < classPath.length; i++)
            {
                resSources[i] = new JarResourceSource(classPath[i]);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new BundleException("Error in class path: " + ex);
        }

        // Get import packages from bundle manifest
        // for use by ImportSearchPolicy.
        Object[][] imports = TextUtil.parseImportExportHeader(
            (String) headerMap.get(Constants.IMPORT_PACKAGE));

        // Get export packages from bundle manifest
        // for use by ImportSearchPolicy.
        Object[][] exports = TextUtil.parseImportExportHeader(
            (String) headerMap.get(Constants.EXPORT_PACKAGE));

        // Get dynamic imports from bundle manifest
        // for use by OSGiImportSearchPolicy.
        String[] dynamics = TextUtil.parseCommaDelimitedString(
            (String) headerMap.get(Constants.DYNAMICIMPORT_PACKAGE));
        dynamics = (dynamics == null) ? new String[0] : dynamics;

        Object[][] attributes = {
            new Object[] { ImportSearchPolicy.EXPORTS_ATTR, exports },
            new Object[] { ImportSearchPolicy.IMPORTS_ATTR, imports },
            new Object[] { OSGiImportSearchPolicy.DYNAMIC_IMPORTS_ATTR, dynamics},
            new Object[] { ImportSearchPolicy.PROPAGATES_ATTR, new Object[0] }
        };

        // Get native library entry names for module library sources.
        LibraryInfo[] libraries =
            TextUtil.parseLibraryStrings(TextUtil.parseCommaDelimitedString(
                (String) headerMap.get(Constants.BUNDLE_NATIVECODE)));
        LibrarySource[] libSources = {
            new OSGiLibrarySource(
                m_cache, id, revision,
                getProperty(Constants.FRAMEWORK_OS_NAME),
                getProperty(Constants.FRAMEWORK_PROCESSOR),
                libraries)
        };

        Module module =
            m_mgr.addModule(
                Long.toString(id) + "." + Integer.toString(revision),
                attributes, resSources, libSources);

        return module;
    }

    //
    // Miscellaneous bundle management methods.
    //

    /**
     * Retrieves a bundle from its location.
     *
     * @param location the location of the bundle to retrieve.
     * @return the bundle associated with the location or null if there
     *         is no bundle associated with the location.
    **/
    private Bundle getBundle(String location)
    {
        synchronized (m_quickLock)
        {
            return (Bundle) m_installedBundleMap.get(location);
        }
    }

    /**
     * This method removes all old revisions of the specified bundle from
     * the bundle cache, except the current revision and removes all modules
     * associated with the specified bundle from the module manager.
     * This method is only called during a refresh.
     * @param bundle the bundle whose revisions should be purged.
     * @throws java.lang.Exception if any error occurs.
    **/
    private void purgeBundle(BundleImpl bundle) throws Exception
    {
        // CONCURRENCY NOTE:
        // This method is called during a refresh operation, which
        // already holds the admin lock.

        // Get the bundle info.
        BundleInfo info = bundle.getInfo();

        // In case of a refresh, then we want to physically
        // remove the bundle's modules from the module manager.
        // This is necessary for two reasons: 1) because
        // under Windows we won't be able to delete the bundle
        // because files might be left open in the resource
        // sources of its modules and 2) we want to make sure
        // that no references to old modules exist since they
        // will all be stale after the refresh. The only other
        // way to do this is to remove the bundle, but that
        // would be incorrect, because this is a refresh operation
        // and should not trigger bundle REMOVE events.
        synchronized (m_quickLock)
        {
            Module[] modules = info.getModules();
            for (int i = 0; i < modules.length; i++)
            {
                m_mgr.removeModule(modules[i]);
            }
        }

        // Purge all bundle revisions, but the current one.
        m_cache.purge(m_cache.getArchive(info.getBundleId()));
    }

    /**
     * This method removes a bundle from the installed bundled map
     * and from the bundle cache. It also removes the bundle's associated
     * module from the module manager. This method is only used internally/
     * @param bundle the bundle to remove.
     * @throws java.lang.Exception if any error occurs.
    **/
    private void removeBundle(BundleImpl bundle) throws Exception
    {
        synchronized (m_adminLock)
        {
            synchronized (m_quickLock)
            {
                // Remove the bundle from the installed bundle map.
                m_installedBundleMap.remove(bundle.getInfo().getLocation());
            }

            // Remove the bundle's associated modules from
            // the module manager.
            Module[] modules = bundle.getInfo().getModules();
            for (int i = 0; i < modules.length; i++)
            {
                m_mgr.removeModule(modules[i]);
            }

            // Remove the bundle from the cache.
            m_cache.remove(m_cache.getArchive(bundle.getInfo().getBundleId()));
        }
    }

    private BundleActivator createBundleActivator(BundleInfo info)
        throws Exception
    {
        // CONCURRENCY NOTE:
        // This method is only called from startBundleWithStartLevel(),
        // which has the exclusion lock, so there is no need to do any
        // locking here.

        BundleActivator activator = null;

        String strict = getConfigProperty(OscarConstants.STRICT_OSGI_PROP);
        boolean isStrict = (strict == null) ? true : strict.equals("true");
        if (!isStrict)
        {
            try
            {
                activator =
                    m_cache.getArchive(info.getBundleId())
                        .getActivator(info.getCurrentModule().getClassLoader());
            }
            catch (Exception ex)
            {
                activator = null;
            }
        }

        // If there was no cached activator, then get the activator
        // class from the bundle manifest.
        if (activator == null)
        {
            // Get the associated bundle archive.
            BundleArchive ba = m_cache.getArchive(info.getBundleId());
            // Get the manifest from the current revision; revision is
            // base zero so subtract one from the count to get the
            // current revision.
            Map headerMap = ba.getManifestHeader(ba.getRevisionCount() - 1);
            // Get the activator class attribute.
            String className = (String) headerMap.get(Constants.BUNDLE_ACTIVATOR);
            // Try to instantiate activator class if present.
            if (className != null)
            {
                className = className.trim();
                Class clazz = info.getCurrentModule().getClassLoader().loadClass(className);
                if (clazz == null)
                {
                    throw new BundleException("Not found: "
                        + className);
                }
                activator = (BundleActivator) clazz.newInstance();
            }
        }

        return activator;
    }

    protected void resolveBundle(BundleImpl bundle)
        throws BundleException
    {
        synchronized (m_adminLock)
        {
            // If a security manager is installed, then check for permission
            // to import the necessary packages.
            if (System.getSecurityManager() != null)
            {
                URL url = null;
                try
                {
                    url = new URL(bundle.getInfo().getLocation());
                }
                catch (MalformedURLException ex)
                {
                    throw new BundleException("Cannot resolve, bad URL "
                        + bundle.getInfo().getLocation());
                }

                try
                {
                    AccessController.doPrivileged(new CheckImportsPrivileged(url, bundle));
                }
                catch (PrivilegedActionException ex)
                {
                    Exception thrown = ((PrivilegedActionException) ex).getException();
                    if (thrown instanceof AccessControlException)
                    {
                        throw (AccessControlException) thrown;
                    }
                    else
                    {
                        throw new BundleException("Problem resolving: " + ex);
                    }
                }
            }

            // Get the import search policy and try to validate
            // the module.
            ImportSearchPolicy search =
                (ImportSearchPolicy) m_mgr.getSearchPolicy();
            Module module = bundle.getInfo().getCurrentModule();
            try
            {
                search.validate(module);
            }
            catch (ValidationException ex)
            {
                int[] v = (int[]) ex.getVersion();
                throw new BundleException("Unresolved package: "
                    + ex.getIdentifier() + "; specification-version=\""
                    + v[0] + "." + v[1] + "." + v[2] + "\"");
            }

            bundle.getInfo().setState(Bundle.RESOLVED);
        }
    }

    //
    // Miscellaneous service management methods.
    //

    /**
     * Sets the properties associated with the given service registration.
    **/
    protected void servicePropertiesModified(ServiceRegistration reg)
    {
        fireServiceEvent(ServiceEvent.MODIFIED, reg.getReference());
    }

    /**
     * Returns the bundles that are using the service referenced by the
     * ServiceReference object. Specifically, this method returns the bundles
     * whose usage count for that service is greater than zero.
     * @return An array of bundles whose usage count for the service referenced
     * by this ServiceReference object is greater than zero; null if no bundles
     * are currently using that service.
    **/
    protected Bundle[] getUsingBundles(ServiceReference ref)
    {
        // Since this needs to look at all bundles, first
        // get lock the write lock object, then get the
        // exclusive lock on the read lock.
        synchronized (m_quickLock)
        {
            List list = null;

            // Access the installed bundle map directly, since
            // the read lock it not recursive, we cannot call
            // Oscar.getBundles() or else we would deadlock.
            Iterator iter = m_installedBundleMap.values().iterator();
            while (iter.hasNext())
            {
                BundleImpl bundle = (BundleImpl) iter.next();
                BundleInfo info = bundle.getInfo();
                if (info.getServiceUsageCounter(ref) != null)
                {
                    if (list == null)
                    {
                        list = new ArrayList();
                    }
                    list.add(bundle);
                }
            }

            if (list != null)
            {
                Bundle[] bundles = new Bundle[list.size()];
                return (Bundle[]) list.toArray(bundles);
            }
        }

        return null;
    }

    /**
     * Unregisters a service for the specified bundle.
     *
     * @param reg the service registration for the service to unregister.
    **/
    protected void unregisterService(BundleImpl bundle, ServiceRegistrationImpl reg)
    {
        synchronized (m_quickLock)
        {
            BundleInfo info = bundle.getInfo();
            info.removeServiceRegistration(reg);
        }

        // Fire event outside synchronized block.
        fireServiceEvent(ServiceEvent.UNREGISTERING, reg.getReference());
    }

    /**
     * Unregisters all services for the specified bundle.
    **/
    protected void unregisterServices(BundleImpl bundle)
    {
        synchronized (m_quickLock)
        {
            BundleInfo info = bundle.getInfo();
            while (info.getServiceRegistrationCount() != 0)
            {
                ServiceRegistrationImpl reg = info.getServiceRegistration(0);
                unregisterService(bundle, reg);
            }
        }
    }

    /**
     * This is a utility method to release all services being
     * used by the specified bundle.
     * @param bundle the bundle whose services are to be released.
    **/
    protected void ungetServices(BundleImpl bundle)
    {
        synchronized (m_quickLock)
        {
            BundleInfo info = bundle.getInfo();
            if (info.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("The bundle is uninstalled.");
            }

            // We don't directly call getServicesInUse() here
            // because of security checks.
            Iterator iter = info.getServiceUsageCounters();
            if (iter.hasNext())
            {
                // Create a list of services so we don't have
                // to worry about concurrent modification to
                // the iterator's collection.
                ArrayList list = new ArrayList();
                while (iter.hasNext())
                {
                    list.add(iter.next());
                }

                // Remove each service object from the
                // service cache.
                for (int i = 0; i < list.size(); i++)
                {
                    ServiceReference ref = (ServiceReference) list.get(i);
                    // Keep ungetting until all usage count is zero.
                    while (info.getServiceUsageCounter(ref) != null)
                    {
                        ungetService(bundle, ref);
                    }
                }
            }
        }
    }

    //
    // Event firing methods.
    //

    /**
     * Miscellaneous events methods.
    **/
    private void fireFrameworkEvent(
        int type, Bundle bundle, Throwable throwable)
    {
        if (m_frameworkDispatcher == null)
        {
            m_frameworkDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj)
                {
                    ((FrameworkListener) l)
                        .frameworkEvent((FrameworkEvent) eventObj);
                }
            };
        }
        FrameworkEvent event = new FrameworkEvent(type, bundle, throwable);
        m_dispatchQueue.dispatch(
            m_frameworkDispatcher, FrameworkListener.class, event);
    }

    /**
     * Fires bundle events.
     *
     * @param type the type of bundle event to fire.
     * @param bundle the bundle associated with the event.
    **/
    private void fireBundleEvent(int type, Bundle bundle)
    {
        if (m_bundleDispatcher == null)
        {
            m_bundleDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj)
                {
                    ((BundleListener) l)
                        .bundleChanged((BundleEvent) eventObj);
                }
            };
        }
        BundleEvent event = null;
        event = new BundleEvent(type, bundle);
        m_dispatchQueue.dispatch(m_bundleDispatcher,
            BundleListener.class, event);
    }

    /**
     * Fires service events.
     *
     * @param type the type of service event to fire.
     * @param ref the service reference associated with the event.
    **/
    private void fireServiceEvent(int type, ServiceReference ref)
    {
        if (m_serviceDispatcher == null)
        {
            m_serviceDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj)
                {
                    ((ServiceListener) l)
                        .serviceChanged((ServiceEvent) eventObj);
                }
            };
        }
        ServiceEvent event = null;
        event = new ServiceEvent(type, ref);
        m_dispatchQueue.dispatch(m_serviceDispatcher,
            ServiceListener.class, event);
    }

    /**
     * Remove all of the specified bundle's event listeners from
     * the framework.
     * @param bundle the bundle whose listeners are to be removed.
    **/
    private void removeListeners(BundleImpl bundle)
    {
        Oscar.debug("Removing all listeners for bundle "
            + bundle.getInfo().getBundleId());
        if (bundle == null)
        {
            return;
        }

        // Remove all listeners associated with the supplied bundle;
        // it is only possible to know the bundle associated with a
        // listener if the listener was wrapper by a ListenerWrapper,
        // so look for those.
        Object[] listeners = m_dispatchQueue.getListeners();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            // Check for listener wrappers and then compare the bundle.
            if (listeners[i + 1] instanceof ListenerWrapper)
            {
                ListenerWrapper lw = (ListenerWrapper) listeners[i + 1];
                if ((lw.getBundle() != null) && (lw.getBundle().equals(bundle)))
                {
                    m_dispatchQueue.removeListener(
                        (Class) listeners[i], (EventListener) listeners[i+1]);
                }
            }
        }

        Oscar.debug("Removed all listeners for bundle "
            + bundle.getInfo().getBundleId());
    }

    //
    // Property related methods.
    //

    private static boolean s_initialized = false;

    /**
     * Installs all system properties specified in the system property
     * file associated with the Oscar installation; these properties
     * will be accessible through <tt>System.getProperty()</tt> at run
     * time. By default, the system property file is located in the
     * same directory as the <tt>oscar.jar</tt> file and is called
     * "<tt>system.properties</tt>". This may be changed by setting the
     * "<tt>oscar.system.properties</tt>" system property to an
     * arbitrary absolute path. The properties in this file will
     * overwrite any existing system properties.
    **/
    public static void initializeSystemProperties()
    {
        // In theory, this should be synchronized, but it
        // is not that critical.
        if (!s_initialized)
        {
            // Set initialized flag.
            s_initialized = true;

            // The system properties file is either specified by a system
            // property or it is in the same directory as the Oscar JAR file.
            // Try to load it from one of these places.

            // See if the property file was specified as a property.
            File propFile = null;
            String custom = System.getProperty(OscarConstants.SYSTEM_PROPERTIES_PROP);
            if (custom != null)
            {
                propFile = new File(custom);
            }
            else
            {
                // Determine where oscar.jar is located by looking at the
                // system class path.
                String jarLoc = null;
                String classpath = System.getProperty("java.class.path");
                int index = classpath.toLowerCase().indexOf("oscar.jar");
                int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
                if (index > start)
                {
                    jarLoc = classpath.substring(start, index);
                    if (jarLoc.length() == 0)
                    {
                        jarLoc = ".";
                    }
                }
                else
                {
                    // Can't figure it out so use the current directory as default.
                    jarLoc = System.getProperty("user.dir");
                }

                propFile = new File(jarLoc, OscarConstants.SYSTEM_PROPERTY_FILE_VALUE);
            }

            // Try to load the global properties file.
            Properties props = new Properties();
            try
            {
                // See if the default property file has been overwritten.
                FileInputStream fis = new FileInputStream(propFile);
                props.load(fis);
                fis.close();
            }
            catch (FileNotFoundException ex)
            {
                // Ignore file not found.
            }
            catch (Exception ex)
            {
                Oscar.error("Oscar: Error loading system properties from "
                    + OscarConstants.SYSTEM_PROPERTY_FILE_VALUE + ": " + ex);
            }

            // Push all loaded properties into System.
            for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
            {
                String name = (String) e.nextElement();
                System.setProperty(name, substVars((String) props.getProperty(name)));
            }
        }
    }

    private void initializeOsgiProperties()
    {
        setProperty(OscarConstants.FRAMEWORK_VERSION,
            OscarConstants.FRAMEWORK_VERSION_VALUE);
        setProperty(OscarConstants.FRAMEWORK_VENDOR,
            OscarConstants.FRAMEWORK_VENDOR_VALUE);
        setProperty(OscarConstants.FRAMEWORK_LANGUAGE,
            System.getProperty("user.language"));
        setProperty(OscarConstants.FRAMEWORK_OS_NAME,
            System.getProperty("os.name"));
        setProperty(OscarConstants.FRAMEWORK_OS_VERSION,
            System.getProperty("os.version"));
        setProperty(OscarConstants.FRAMEWORK_PROCESSOR,
            System.getProperty("os.arch"));
    }

    private void initializeBundleProperties()
    {
        //
        // First set various default global property values.
        //

        // The Oscar version property.
        setProperty(OscarConstants.OSCAR_VERSION_PROPERTY,
            OscarConstants.OSCAR_VERSION_VALUE);

        // Oscar OSGi strictness property.
        String strict = getConfigProperty(OscarConstants.STRICT_OSGI_PROP);
        boolean isStrict = (strict == null) ? true : strict.equals("true");
        setProperty(OscarConstants.STRICT_OSGI_PROP, (isStrict) ? "true" : "false");

        // The Java user directory property.
        String val = System.getProperty("user.home");
        if (val != null)
            setProperty("user.home", val);

        // The Java user directory property.
        val = System.getProperty("user.dir");
        if (val != null)
            setProperty("user.dir", val);

        // Push all properties from the bundle property file into
        // the Oscar global properties.
        Properties props = readBundlePropertiesFile();
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();
            setProperty(name, (String) props.getProperty(name));
        }
    }

    /**
     * Retrieves the bundle property file associated with the Oscar
     * installation; these properties will be accessible through
     * <tt>BundleContext.getProperty()</tt> at run time. By default, the
     * Bundle property file is located in the same directory as the
     * <tt>oscar.jar</tt> file and is called "<tt>bundle.properties</tt>".
     * This may be changed by setting the
     * "<tt>oscar.bundle.properties</tt>" system property to an
     * arbitrary absolute path.
     *
     * @return a list of properties or <tt>null</tt> if there was an error.
    **/
    private Properties readBundlePropertiesFile()
    {
        // The global properties file is either specified by a system
        // property or it is in the same directory as the Oscar JAR file.
        // Try to load it from one of these places.

        // See if the property file was specified as a property.
        File propFile = null;
        String custom = getConfigProperty(OscarConstants.BUNDLE_PROPERTIES_PROP);
        if (custom != null)
        {
            propFile = new File(custom);
        }
        else
        {
            // Determine where oscar.jar is located by looking at the
            // system class path.
            String jarLoc = null;
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("oscar.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index > start)
            {
                jarLoc = classpath.substring(start, index);
                if (jarLoc.length() == 0)
                {
                    jarLoc = ".";
                }
            }
            else
            {
                // Can't figure it out so use the current directory as default.
                jarLoc = System.getProperty("user.dir");
            }

            propFile = new File(jarLoc, OscarConstants.BUNDLE_PROPERTY_FILE_VALUE);
        }

        // Try to load the global properties file.
        Properties props = new Properties();
        try
        {
            // See if the default property file has been overwritten.
            FileInputStream fis = new FileInputStream(propFile);
            props.load(fis);
            fis.close();
        }
        catch (FileNotFoundException ex)
        {
            // Ignore file not found.
        }
        catch (Exception ex)
        {
            Oscar.error("Oscar: Error loading bundle properties from "
                + OscarConstants.BUNDLE_PROPERTY_FILE_VALUE + ": " + ex);
            return null;
        }

        return props;
    }

    private static final String DELIM_START = "${";
    private static final char DELIM_STOP  = '}';
    private static final int DELIM_START_LEN = 2;
    private static final int DELIM_STOP_LEN  = 1;

    private static String substVars(String val)
        throws IllegalArgumentException
    {
        StringBuffer sbuf = new StringBuffer();

        if (val == null)
        {
            return val;
        }

        int i = 0;
        int j, k;

        while (true)
        {
            j = val.indexOf(DELIM_START, i);
            if (j == -1)
            {
                if (i == 0)
                {
                    return val;
                }
                else
                {
                    sbuf.append(val.substring(i, val.length()));
                    return sbuf.toString();
                }
            }
            else
            {
                sbuf.append(val.substring(i, j));
                k = val.indexOf(DELIM_STOP, j);
                if (k == -1)
                {
                    throw new IllegalArgumentException(
                    '"' + val +
                    "\" has no closing brace. Opening brace at position "
                    + j + '.');
                }
                else
                {
                    j += DELIM_START_LEN;
                    String key = val.substring(j, k);
                    // Try system properties.
                    String replacement = System.getProperty(key, null);
                    if (replacement != null)
                    {
                        sbuf.append(replacement);
                    }
                    i = k + DELIM_STOP_LEN;
                }
            }
        }
    }

    private void processAutoProperties()
    {
        // The auto-install property specifies bundles to be automatically
        // installed in each new profile; it is possible to specify a
        // start level for these bundles by appending a ".n" to the
        // auto-install property name, where "n" is the desired start
        // level for the bundle. This code will search for the auto-install
        // property and for subsequent variants with a specified start level
        // up until it finds a break in the sequence of start levels.

        // NOTE: The following is a little bit of a hack. For legacy reasons
        // we still have to allow for the possibility of an auto-install
        // property with no start level specified. To do this, we first
        // try get the property with no start level and try to process it,
        // then we retrieve the next property at the end of the loop and
        // then process it, and so on.
        String prop = getConfigProperty(OscarConstants.AUTO_INSTALL_PROP);
        int level = 0;
        do
        {
            if (prop != null)
            {
                StringTokenizer st = new StringTokenizer(prop, "\" ",true);
                if (st.countTokens() > 0)
                {
                    String location = null;
                    do
                    {
                        location = nextLocation(st);
                        if (location != null)
                        {
                            try
                            {
                                BundleImpl b = (BundleImpl) installBundle(location, null);
                                // The legacy case defaults to level 1.
                                b.getInfo().setStartLevel((level == 0) ? 1 : level);
                            }
                            catch (Exception ex)
                            {
                                System.err.println("Oscar: Auto-properties install.");
                                ex.printStackTrace();
                            }
                        }
                    }
                    while (location != null);
                }
            }

            level++;
            prop = getConfigProperty(OscarConstants.AUTO_INSTALL_PROP + "." + level);
        }
        while (prop != null);

        // The auto-start property specifies bundles to be automatically
        // installed and started in each new profile; it is possible to
        // specify a start level for these bundles by appending a ".n" to
        // the auto-start property name, where "n" is the desired start
        // level for the bundle. This code will search for the auto-start
        // property and for subsequent variants with a specified start level
        // up until it finds a break in the sequence of start levels.

        // NOTE: The following is a little bit of a hack. For legacy reasons
        // we still have to allow for the possibility of an auto-start
        // property with no start level specified. To do this, we first
        // try get the property with no start level and try to process it,
        // then we retrieve the next property at the end of the loop and
        // then process it, and so on.
        prop = getConfigProperty(OscarConstants.AUTO_START_PROP);
        level = 0;
        do
        {
            if (prop != null)
            {
                // First install all autostart bundles to avoid
                // start-up ordering problems.
                StringTokenizer st = new StringTokenizer(prop, "\" ",true);
                if (st.countTokens() > 0)
                {
                    String location = null;
                    do
                    {
                        location = nextLocation(st);
                        if (location != null)
                        {
                            try
                            {
                                BundleImpl b = (BundleImpl) installBundle(location, null);
                                // The legacy case defaults to level 1.
                                b.getInfo().setStartLevel((level == 0) ? 1 : level);
                            }
                            catch (Exception ex)
                            {
                                System.err.println("Oscar: Auto-properties install.");
                                ex.printStackTrace();
                            }
                        }
                    }
                    while (location != null);
                }

                // Now loop through and start the installed bundles.
                st = new StringTokenizer(prop, "\" ",true);
                if (st.countTokens() > 0)
                {
                    String location = null;
                    do
                    {
                        location = nextLocation(st);
                        if (location != null)
                        {
                            // Installing twice just returns the same bundle.
                            try
                            {
                                BundleImpl bundle = (BundleImpl) installBundle(location, null);
                                startBundle(bundle);
                            }
                            catch (Exception ex)
                            {
                                System.err.println("Oscar: Auto-properties start.");
                                ex.printStackTrace();
                            }
                        }
                    }
                    while (location != null);
                }
            }

            level++;
            prop = getConfigProperty(OscarConstants.AUTO_START_PROP + "." + level);
        }
        while (prop != null);
    }

    private String nextLocation(StringTokenizer st)
    {
        String retVal = null;

        if (st.countTokens() > 0)
        {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok = null;
            String location = null;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit))
            {
                tok = st.nextToken(tokenList);
                if (tok.equals("\""))
                {
                    inQuote = ! inQuote;
                    if (inQuote)
                    {
                        tokenList = "\"";
                    }
                    else
                    {
                        tokenList = "\" ";
                    }

                }
                else if (tok.equals(" "))
                {
                    if (tokStarted)
                    {
                        retVal = tokBuf.toString();
                        tokStarted=false;
                        tokBuf = new StringBuffer(10);
                        exit = true;
                    }
                }
                else
                {
                    tokStarted = true;
                    tokBuf.append(tok.trim());
                }
            }

            // Handle case where end of token stream and
            // still got data
            if ((!exit) && (tokStarted))
            {
                retVal = tokBuf.toString();
            }
        }

        return retVal;
    }

    //
    // Public static utility methods.
    //

    public static void setDebug(PrintStream ps)
    {
        synchronized (m_outputLockObj)
        {
            m_debugOut = ps;
        }
    }

    public static void debug(String s)
    {
        synchronized (m_outputLockObj)
        {
            if (m_debugOut != null)
            {
                m_debugOut.println(s);
            }
        }
    }

    public static void error(String s)
    {
        synchronized (m_outputLockObj)
        {
            if (m_errorOut != null)
            {
                m_errorOut.println(s);
            }
        }
    }

    public static void error(String s, Throwable th)
    {
        synchronized (m_outputLockObj)
        {
            if (m_errorOut != null)
            {
                m_errorOut.println(s);
                th.printStackTrace(m_errorOut);
            }
        }
    }

    //
    // Private utility methods.
    //

    /**
     * Generated the next valid bundle identifier.
    **/
    private synchronized long getNextId()
    {
        return m_nextId++;
    }

    /**
     * Generated the next valid service identifier.
    **/
    private synchronized long getNextServiceId()
    {
        return m_nextServiceId++;
    }

    //
    // Private utility classes.
    //

    /**
     * Simple class that is used in <tt>refreshPackages()</tt> to embody
     * the refresh logic in order to keep the code clean. This class is
     * not static because it needs access to framework event firing methods.
    **/
    private class RefreshHelper
    {
        private BundleImpl m_bundle = null;
        private boolean m_active = false;

        public RefreshHelper(Bundle bundle)
        {
            m_bundle = (BundleImpl) bundle;
        }

        public void stop()
        {
            if (m_bundle.getInfo().getPersistentState() == Bundle.ACTIVE)
            {
                m_active = true;
                try
                {
                    stopBundle(m_bundle);
                }
                catch (BundleException ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }

        public void purgeOrRemove()
        {
            try
            {
                BundleInfo info = m_bundle.getInfo();

                // Remove or purge the bundle depending on its
                // current state.
                if (info.getState() == Bundle.UNINSTALLED)
                {
                    // This physically removes the bundle from memory
                    // as well as the bundle cache.
                    removeBundle(m_bundle);
                    m_bundle = null;
                }
                else
                {
                    // This physically removes all old revisions of the
                    // bundle from memory and only maintains the newest
                    // version in the bundle cache.
                    purgeBundle(m_bundle);
                }
            }
            catch (Exception ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
            }
        }

        public void reinitialize()
        {
            if (m_bundle != null)
            {
                try
                {
                    synchronized (m_quickLock)
                    {
                        BundleInfo info = m_bundle.getInfo();
                        BundleInfo newInfo = createBundleInfo(info.getArchive());
                        m_bundle.setInfo(newInfo);
                    }
                }
                catch (Exception ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }

        public void restart()
        {
            if (m_bundle != null)
            {
                if (m_active)
                {
                    try
                    {
                        startBundle(m_bundle);
                    }
                    catch (BundleException ex)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                    }
                }
            }
        }

        public String toString()
        {
            return m_bundle.toString();
        }
    }

    /**
     * This simple class is used to perform the privileged action of
     * checking if a bundle has permission to import its packages.
    **/
    private static class CheckImportsPrivileged implements PrivilegedExceptionAction
    {
        private URL m_url = null;
        private BundleImpl m_bundle = null;

        public CheckImportsPrivileged(URL url, BundleImpl bundle)
        {
            m_url = url;
            m_bundle = bundle;
        }

        public Object run() throws Exception
        {
            // Get permission collection for code source; we cannot
            // call AccessController.checkPermission() directly since
            // the bundle's code is not on the access context yet because
            // it has not started yet...we are simply resolving it to see
            // if we can start it. We must check for import permission
            // on the exports as well, since export implies import.
            CodeSource cs = new CodeSource(m_url, (Certificate[]) null);
            PermissionCollection pc = Policy.getPolicy().getPermissions(cs);

            // Check import permission for all imports of the current module.
            Object[][] imports =
                ImportSearchPolicy.getImportsAttribute(m_bundle.getInfo().getCurrentModule());
            for (int i = 0; i < imports.length; i++)
            {
                PackagePermission perm = new PackagePermission(
                    (String) imports[i][ImportSearchPolicy.IDENTIFIER_IDX],
                    PackagePermission.IMPORT);
                if (!pc.implies(perm))
                {
                    throw new AccessControlException("access denied " + perm);
                }
            }

            // Check export permission for all exports of the current module.
            imports =
                ImportSearchPolicy.getExportsAttribute(m_bundle.getInfo().getCurrentModule());
            for (int i = 0; i < imports.length; i++)
            {
                PackagePermission perm = new PackagePermission(
                    (String) imports[i][ImportSearchPolicy.IDENTIFIER_IDX],
                    PackagePermission.EXPORT);
                if (!pc.implies(perm))
                {
                    throw new AccessControlException("access denied " + perm);
                }
            }

            return null;
        }
    }

    private static class StartStopPrivileged implements PrivilegedExceptionAction
    {
        private Oscar m_oscar = null;
        private int m_action = 0;
        private BundleImpl m_bundle = null;

        public static final int START_ACTION = 0;
        public static final int STOP_ACTION = 1;

        public StartStopPrivileged(Oscar oscar)
        {
            m_oscar = oscar;
        }

        public void setAction(int i)
        {
            m_action = i;
        }

        public void setBundle(BundleImpl bundle)
        {
            m_bundle = bundle;
        }

        public Object run() throws Exception
        {
            if (m_action == START_ACTION)
            {
                m_bundle.getInfo().getActivator().start(m_bundle.getInfo().getContext());
            }
            else
            {
                m_bundle.getInfo().getActivator().stop(m_bundle.getInfo().getContext());
            }
            return null;
        }
    }
}
