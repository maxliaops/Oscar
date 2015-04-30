package org.ungoverned.oscar;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.ungoverned.oscar.util.*;

public class Oscar {
    // Debug output.
    private static PrintStream m_debugOut = null;
    // Error output.
    private static PrintStream m_errorOut = null;
    // Output lock.
    private static String m_outputLockObj = new String("output lock");

    // List of event listeners.
    private transient OscarDispatchQueue m_dispatchQueue = null;
    // Re-usable event dispatchers.
    private Dispatcher m_frameworkDispatcher = null;
    private Dispatcher m_bundleDispatcher = null;
    private Dispatcher m_serviceDispatcher = null;

    // Status flag for Oscar.
    public static final int UNKNOWN_STATUS = -1;
    public static final int RUNNING_STATUS = 0;
    public static final int STARTING_STATUS = 1;
    public static final int STOPPING_STATUS = 2;
    private transient int m_oscarStatus = UNKNOWN_STATUS;

    public Oscar() {
        this(null, null);
    }

    public Oscar(Properties props) {
        this(props, null);
    }

    public Oscar(List activatorList) {
        this(null, activatorList);
    }

    public Oscar(Properties props, List activatorList) {
        // Initialize.
        initialize(activatorList);
    }

    private void initialize(List activatorList) {
        // Oscar is now in its startup sequence.
        m_oscarStatus = STARTING_STATUS;

        // Turn on error information...
        m_errorOut = System.err;

        setDebug(System.out);

        // Initialize private members.
        m_dispatchQueue = new OscarDispatchQueue();

        SystemBundle systembundle = null;

        try {
            // Create a simple bundle info for the system bundle.
            BundleInfo info = new BundleInfo();
            systembundle = new SystemBundle(this, info, activatorList);
        } catch (BundleException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Oscar is now running.
        m_oscarStatus = RUNNING_STATUS;

        // Fire started event for system bundle.
        fireBundleEvent(BundleEvent.STARTED, systembundle);

        // Send a framework event to indicate Oscar has started.
        fireFrameworkEvent(FrameworkEvent.STARTED, systembundle, null);
    }

    //
    // Oscar framework attribute methods.
    //

    public int getFrameworkStatus() {
        return m_oscarStatus;
    }

    //
    // Event firing methods.
    //

    private void fireFrameworkEvent(int type, Bundle bundle, Throwable throwable) {
        Oscar.debug("Oscar-fireFrameworkEvent:  type=" + type + " throwable="
                + throwable);
        if (m_frameworkDispatcher == null) {
            m_frameworkDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj) {
                    ((FrameworkListener) l)
                            .frameworkEvent((FrameworkEvent) eventObj);
                }
            };
        }
        FrameworkEvent event = new FrameworkEvent(type, bundle, throwable);
        m_dispatchQueue.dispatch(m_frameworkDispatcher,
                FrameworkListener.class, event);
    }

    private void fireBundleEvent(int type, Bundle bundle) {
        Oscar.debug("Oscar-fireBundleEvent:  type=" + type + " bundle="
                + bundle);
        if (m_bundleDispatcher == null) {
            m_bundleDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj) {
                    ((BundleListener) l).bundleChanged((BundleEvent) eventObj);
                }
            };
        }
        BundleEvent event = null;
        event = new BundleEvent(type, bundle);
        m_dispatchQueue.dispatch(m_bundleDispatcher, BundleListener.class,
                event);
    }

    private void fireServiceEvent(int type, ServiceReference ref) {
        Oscar.debug("Oscar-fireServiceEvent:  type=" + type + " ref=" + ref);
        if (m_serviceDispatcher == null) {
            m_serviceDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj) {
                    ((ServiceListener) l)
                            .serviceChanged((ServiceEvent) eventObj);
                }
            };
        }
        ServiceEvent event = null;
        event = new ServiceEvent(type, ref);
        m_dispatchQueue.dispatch(m_serviceDispatcher, ServiceListener.class,
                event);
    }

    private void removeListeners(BundleImpl bundle) {
        Oscar.debug("Removing all listeners for bundle "
                + bundle.getInfo().getBundleId());
        if (bundle == null) {
            return;
        }

        // Remove all listeners associated with the supplied bundle;
        // it is only possible to know the bundle associated with a
        // listener if the listener was wrapper by a ListenerWrapper,
        // so look for those.
        Object[] listeners = m_dispatchQueue.getListeners();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            // Check for listener wrappers and then compare the bundle.
            if (listeners[i + 1] instanceof ListenerWrapper) {
                ListenerWrapper lw = (ListenerWrapper) listeners[i + 1];
                if ((lw.getBundle() != null) && (lw.getBundle().equals(bundle))) {
                    m_dispatchQueue.removeListener((Class) listeners[i],
                            (EventListener) listeners[i + 1]);
                }
            }
        }

        Oscar.debug("Removed all listeners for bundle "
                + bundle.getInfo().getBundleId());
    }

    //
    // Public static utility methods.
    //

    public static void setDebug(PrintStream ps) {
        synchronized (m_outputLockObj) {
            m_debugOut = ps;
        }
    }

    public static void debug(String s) {
        synchronized (m_outputLockObj) {
            if (m_debugOut != null) {
                m_debugOut.println(s);
            }
        }
    }

    public static void error(String s) {
        synchronized (m_outputLockObj) {
            if (m_errorOut != null) {
                m_errorOut.println(s);
            }
        }
    }

    public static void error(String s, Throwable th) {
        synchronized (m_outputLockObj) {
            if (m_errorOut != null) {
                m_errorOut.println(s);
                th.printStackTrace(m_errorOut);
            }
        }
    }

}
