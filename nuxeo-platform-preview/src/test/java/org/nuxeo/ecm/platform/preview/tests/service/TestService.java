/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.nuxeo.ecm.platform.preview.tests.service;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test Preview adapter management service registration
 *
 * @author tiry
 *
 */
public class TestService extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.platform.preview", "OSGI-INF/preview-adapter-framework.xml");
    }

    @Test
    public void testService() {
        PreviewAdapterManager pam = Framework.getLocalService(PreviewAdapterManager.class);
        assertNotNull(pam);
    }

    @Test
    public void testServiceContrib() throws Exception {
        PreviewAdapterManager pam = Framework.getLocalService(PreviewAdapterManager.class);
        assertNotNull(pam);

        MimeTypePreviewer previewer = pam.getPreviewer("text/html");
        assertNull(previewer);

        deployContrib("org.nuxeo.ecm.platform.preview", "OSGI-INF/preview-adapter-contrib.xml");
        previewer = pam.getPreviewer("text/html");
        assertNotNull(previewer);
    }

}
