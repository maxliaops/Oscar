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
package org.ungoverned.oscar.installer;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import org.ungoverned.oscar.installer.artifact.*;
import org.ungoverned.oscar.installer.editor.*;
import org.ungoverned.oscar.installer.property.*;
import org.ungoverned.oscar.util.OscarConstants;

public class Install extends JFrame
{
    private static transient final String PROPERTY_FILE = "property.xml";
    private static transient final String ARTIFACT_FILE = "artifact.xml";

    public static transient final String JAVA_DIR = "Java directory";
    public static transient final String INSTALL_DIR = "Install directory";

    private PropertyPanel m_propPanel = null;
    private JButton m_okayButton = null;
    private JButton m_cancelButton = null;
    private JLabel m_statusLabel = null;

    private java.util.List m_propList = null;
    private java.util.List m_artifactList = null;

    public Install()
        throws Exception
    {
        super("Install");

        // Load properties before resources, because resources
        // refer to properties.
        m_propList = loadPropertyList();
        m_artifactList = loadArtifactList();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(
            m_propPanel = new PropertyPanel(m_propList), BorderLayout.CENTER);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(true);
        centerWindow(this);

        // Make window closeable.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event)
            {
                doCancel();
            }
        });
    }

    public java.util.List loadPropertyList()
    {
        String installDir = System.getProperty("user.home");
        if (!installDir.endsWith(File.separator))
        {
            installDir = installDir + File.separator;
        }

        Property prop = null;

        // Eventually these should be read from a file.
        java.util.List list = new ArrayList();

        // Add the shell choice property.
        prop = new BooleanPropertyImpl("Shell", true);
        prop.setEditor(new BooleanEditor((BooleanProperty) prop, "Text", "GUI"));
        list.add(prop);

        // Add the java directory property.
        prop = new StringPropertyImpl(JAVA_DIR, System.getProperty("java.home"));
        prop.setEditor(new FileEditor((StringProperty) prop, true));
        list.add(prop);

        // Add the installation directory property.
        prop = new StringPropertyImpl(INSTALL_DIR, installDir + "Oscar");
        prop.setEditor(new FileEditor((StringProperty) prop, true));
        list.add(prop);

        // Add the documentation URL property.
        prop = new BooleanStringPropertyImpl(
            "User documentation",
            true,
            "http://download.forge.objectweb.org/oscar/oscar-doc-"
            + OscarConstants.OSCAR_VERSION_VALUE + ".jar");
        list.add(prop);

        // Add the documentation URL property.
        prop = new BooleanStringPropertyImpl(
            "API documentation",
            true,
            "http://download.forge.objectweb.org/oscar/oscar-api-"
            + OscarConstants.OSCAR_VERSION_VALUE + ".jar");
        list.add(prop);

        return list;
    }

    public java.util.List loadArtifactList() throws Exception
    {
        // Eventually I will changed these to be read from a file.
        java.util.List list = new ArrayList();
        list.add(
            new ArtifactHolder(
                (BooleanProperty) getProperty("User documentation"),
                new URLJarArtifact(
                    (StringProperty) getProperty("User documentation"))));
        list.add(
            new ArtifactHolder(
                (BooleanProperty) getProperty("API documentation"),
                new URLJarArtifact(
                    (StringProperty) getProperty("API documentation"))));
        list.add(
            new ArtifactHolder(
                new ResourceJarArtifact(
                    new StringPropertyImpl("sourceName", "package.jar"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "src.jar"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "LICENSE.txt"))));
        list.add(
            new ArtifactHolder(
                (BooleanProperty) getProperty("Shell"),
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "system.properties.text"),
                    new StringPropertyImpl("destName", "system.properties"),
                    new StringPropertyImpl("destDir", "lib"))));
        list.add(
            new ArtifactHolder(
                new NotBooleanPropertyImpl((BooleanProperty) getProperty("Shell")),
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "system.properties.gui"),
                    new StringPropertyImpl("destName", "system.properties"),
                    new StringPropertyImpl("destDir", "lib"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "example.policy"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "oscar.bat"),
                    new StringPropertyImpl("destName" , "oscar.bat"),
                    new StringPropertyImpl("destDir", ""),
                    true)));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "oscar.sh"),
                    new StringPropertyImpl("destName" , "oscar.sh"),
                    new StringPropertyImpl("destDir", ""),
                    true)));

        return list;
    }

    private Property getProperty(String name)
    {
        for (int i = 0; i < m_propList.size(); i++)
        {
            Property prop = (Property) m_propList.get(i);
            if (prop.getName().equals(name))
            {
                return prop;
            }
        }
        return null;
    }

    protected void doOkay()
    {
        m_propPanel.setEnabled(false);
        m_okayButton.setEnabled(false);
        m_cancelButton.setEnabled(false);
        new Thread(new InstallRunnable()).start();
    }

    protected void doCancel()
    {
        System.exit(0);
    }

    protected JPanel createButtonPanel()
    {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Create and set layout.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        buttonPanel.setLayout(grid);

        // Create labels and fields.
        c.insets = new Insets(2, 2, 2, 2);

        // Okay button.
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        grid.setConstraints(m_okayButton = new JButton("OK"), c);
        buttonPanel.add(m_okayButton);
        m_okayButton.setDefaultCapable(true);
        getRootPane().setDefaultButton(m_okayButton);

        // Cancel button.
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        grid.setConstraints(m_cancelButton = new JButton("Cancel"), c);
        buttonPanel.add(m_cancelButton);

        // Add action listeners.
        m_okayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                doOkay();
            }
        });

        m_cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                doCancel();
            }
        });

        // Status label.
        m_statusLabel = new JLabel("Oscar installation");
        m_statusLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        // Complete panel.
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(m_statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    public static void centerWindow(Component window)
    {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dim = toolkit.getScreenSize();
        int screenWidth = dim.width;
        int screenHeight = dim.height;
        int x = (screenWidth - window.getSize().width) / 2;
        int y = (screenHeight - window.getSize().height) / 2;
        window.setLocation(x, y);
    }

    public static void main(String[] argv) throws Exception
    {
        String msg = "<html>"
            + "<center><h1>Oscar " + OscarConstants.OSCAR_VERSION_VALUE + "</h1></center>"
            + "You can download example bundles at the Oscar shell prompt by<br>"
            + "using the <b><tt>obr</tt></b> command to access the Oscar Bundle Repository;<br>"
            + "type <b><tt>obr help</tt></b> at the Oscar shell prompt for details."
            + "</html>";
        JLabel label = new JLabel(msg);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        final JDialog dlg = new JDialog((Frame) null, "Oscar Install", true);
        dlg.getContentPane().setLayout(new BorderLayout(10, 10));
        dlg.getContentPane().add(label, BorderLayout.CENTER);
        JPanel panel = new JPanel();
        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                dlg.hide();
            }
        });
        panel.add(button);
        dlg.getContentPane().add(panel, BorderLayout.SOUTH);
        // For spacing purposes...
        dlg.getContentPane().add(new JPanel(), BorderLayout.NORTH);
        dlg.getContentPane().add(new JPanel(), BorderLayout.EAST);
        dlg.getContentPane().add(new JPanel(), BorderLayout.WEST);
        dlg.pack();
        centerWindow(dlg);
        dlg.show();

        Install obj = new Install();
        obj.setVisible(true);
    }

    class InstallRunnable implements Runnable
    {
        public void run()
        {
            Map propMap = new HashMap();
            for (int i = 0; i < m_propList.size(); i++)
            {
                Property prop = (Property) m_propList.get(i);
                propMap.put(prop.getName(), prop);
            }

            String installDir = ((StringProperty) propMap.get(INSTALL_DIR)).getStringValue();

            // Make sure the install directory ends with separator char.
            if (!installDir.endsWith(File.separator))
            {
                installDir = installDir + File.separator;
            }

            // Make sure the install directory exists and
            // that is actually a directory.
            File file = new File(installDir);
            if (!file.exists())
            {
                if (!file.mkdirs())
                {
                    JOptionPane.showMessageDialog(Install.this,
                        "Unable to create install directory.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(-1);
                }
            }
            else if (!file.isDirectory())
            {
                JOptionPane.showMessageDialog(Install.this,
                    "The selected install location is not a directory.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }

            // Status updater runnable.
            StatusRunnable sr = new StatusRunnable();

            // Loop through and process resources.
            for (int i = 0; i < m_artifactList.size(); i++)
            {
                ArtifactHolder ah = (ArtifactHolder) m_artifactList.get(i);
                if (ah.isIncluded())
                {
                    if (!ah.getArtifact().process(sr, propMap))
                    {
                        JOptionPane.showMessageDialog(Install.this,
                            "An error occurred while processing the resources.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(-1);
                    }
                }
            }

            System.exit(0);
        }
    }

    class StatusRunnable implements Status, Runnable
    {
        private String text = null;

        public void setText(String s)
        {
            text = s;
            try {
                SwingUtilities.invokeAndWait(this);
            } catch (Exception ex) {
                // Ignore.
            }
        }

        public void run()
        {
            m_statusLabel.setText(text);
        }
    }

    // Re-usable static member for ResourceHolder inner class.
    private static BooleanProperty m_trueProp =
        new BooleanPropertyImpl("mandatory", true);

    class ArtifactHolder
    {
        private BooleanProperty m_isIncluded = null;
        private Artifact m_artifact = null;
        
        public ArtifactHolder(Artifact artifact)
        {
            this(m_trueProp, artifact);
        }
        
        public ArtifactHolder(BooleanProperty isIncluded, Artifact artifact)
        {
            m_isIncluded = isIncluded;
            m_artifact = artifact;
        }
        
        public boolean isIncluded()
        {
            return m_isIncluded.getBooleanValue();
        }
        
        public Artifact getArtifact()
        {
            return m_artifact;
        }
    }
}
