package org.ungoverned.oscar;

import java.util.List;

import org.osgi.framework.BundleException;

public class SystemBundle extends BundleImpl {

    protected SystemBundle(Oscar oscar, BundleInfo info, List activatorList)
        throws BundleException
    {
        super(oscar, info);
    }
}
