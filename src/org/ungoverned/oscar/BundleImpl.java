package org.ungoverned.oscar;

import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class BundleImpl implements Bundle {
    private Oscar m_oscar = null;
    private BundleInfo m_info = null;

    protected BundleImpl(Oscar oscar, BundleInfo info)
    {
        m_oscar = oscar;
        m_info = info;
    }

    Oscar getOscar() // package protected
    {
        return m_oscar;
    }

    BundleInfo getInfo() // package protected
    {
        return m_info;
    }

    void setInfo(BundleInfo info) // package protected
    {
        m_info = info;
    }

	@Override
	public int getState() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void start() throws BundleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() throws BundleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void update() throws BundleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(InputStream in) throws BundleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void uninstall() throws BundleException {
		// TODO Auto-generated method stub

	}

	@Override
	public Dictionary getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getBundleId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference[] getRegisteredServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceReference[] getServicesInUse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasPermission(Object permission) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public URL getResource(String name) {
		// TODO Auto-generated method stub
		return null;
	}

}
