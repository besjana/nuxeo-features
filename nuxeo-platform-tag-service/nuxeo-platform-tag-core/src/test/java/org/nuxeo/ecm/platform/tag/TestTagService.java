/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Radu Darlea
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseOracle;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.DocumentSnapshot;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.runtime.api.Framework;

public class TestTagService extends SQLRepositoryTestCase {

    protected static final Log log = LogFactory.getLog(TestTagService.class);

    protected TagService tagService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.comment");
        deployBundle("org.nuxeo.ecm.platform.tag");
        deployBundle("org.nuxeo.ecm.platform.ws");
        deployTestContrib("org.nuxeo.ecm.platform.tag",
                getClass().getResource("/login-config.xml"));
        openSession();
        tagService = Framework.getLocalService(TagService.class);
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    // Oracle fails if we do too many connections in a short time, sleep
    // here to prevent this.
    public void maybeSleep() throws Exception {
        if (DatabaseHelper.DATABASE instanceof DatabaseOracle) {
            Thread.sleep(5 * 1000);
        }
    }

    @Test
    public void testTags() throws Exception {
        DocumentModel fold = session.createDocumentModel("/", "fold", "Folder");
        fold = session.createDocument(fold);
        DocumentModel file1 = session.createDocumentModel("/", "foo", "File");
        file1.setPropertyValue("dc:title", "File1");
        file1 = session.createDocument(file1);
        DocumentModel file2 = session.createDocumentModel("/fold", "bar",
                "File");
        file2.setPropertyValue("dc:title", "File2");
        file2 = session.createDocument(file2);
        session.save();
        String file1Id = file1.getId();
        String file2Id = file2.getId();

        Set<String> file1set = new HashSet<String>(Arrays.asList(file1Id));
        Set<String> twofiles = new HashSet<String>(Arrays.asList(file1Id,
                file2Id));

        // add tag
        tagService.tag(session, file1Id, "mytag", "Administrator");
        tagService.tag(session, file1Id, "othertag", "Administrator");
        tagService.tag(session, file2Id, "mytag", "Administrator");

        Set<String> mytag = new HashSet<String>(Arrays.asList("mytag"));
        Set<String> twotags = new HashSet<String>(Arrays.asList("mytag",
                "othertag"));

        // find tags for doc
        List<Tag> tags;
        // file 1
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(twotags, labels(tags));
        tags = tagService.getDocumentTags(session, file1Id, "Administrator");
        assertEquals(twotags, labels(tags));
        tags = tagService.getDocumentTags(session, file1Id, "bob");
        assertTrue(tags.isEmpty());
        // file 2
        tags = tagService.getDocumentTags(session, file2Id, null);
        assertEquals(mytag, labels(tags));
        tags = tagService.getDocumentTags(session, file2Id, "Administrator");
        assertEquals(mytag, labels(tags));
        tags = tagService.getDocumentTags(session, file2Id, "bob");
        assertTrue(tags.isEmpty());

        maybeSleep();

        // find docs for tag
        List<String> docIds;
        // tag 1
        docIds = tagService.getTagDocumentIds(session, "mytag", null);
        assertEquals(twofiles, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "mytag", "Administrator");
        assertEquals(twofiles, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "mytag", "bob");
        assertTrue(docIds.isEmpty());
        // tag 2
        docIds = tagService.getTagDocumentIds(session, "othertag", null);
        assertEquals(file1set, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "othertag",
                "Administrator");
        assertEquals(file1set, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "othertag", "bob");
        assertTrue(docIds.isEmpty());

        maybeSleep();

        // global cloud
        List<Tag> cloud = tagService.getTagCloud(session, null, null, null);
        assertEquals(2, cloud.size());
        Collections.sort(cloud, Tag.LABEL_COMPARATOR);
        Tag tag1 = cloud.get(0);
        assertEquals("mytag", tag1.getLabel());
        assertEquals(2, tag1.getWeight());
        Tag tag2 = cloud.get(1);
        assertEquals("othertag", tag2.getLabel());
        assertEquals(1, tag2.getWeight());
        // specific tagging user
        cloud = tagService.getTagCloud(session, null, "bob", null);
        assertEquals(0, cloud.size());

        // cloud per folder
        cloud = tagService.getTagCloud(session, fold.getId(), null, null);
        assertEquals(1, cloud.size()); // only file2 under fold
        Collections.sort(cloud, Tag.LABEL_COMPARATOR);
        tag1 = cloud.get(0);
        assertEquals("mytag", tag1.getLabel());
        assertEquals(1, tag1.getWeight());

        // cloud under root folder
        cloud = tagService.getTagCloud(session,
                session.getRootDocument().getId(), null, null);
        assertEquals(2, cloud.size());
        Collections.sort(cloud, Tag.LABEL_COMPARATOR);
        tag1 = cloud.get(0);
        assertEquals("mytag", tag1.getLabel());
        assertEquals(2, tag1.getWeight());
        tag2 = cloud.get(1);
        assertEquals("othertag", tag2.getLabel());
        assertEquals(1, tag2.getWeight());

        // suggestions
        List<Tag> suggestions = tagService.getSuggestions(session, "my", null);
        assertEquals(mytag, labels(suggestions));
        suggestions = tagService.getSuggestions(session, "%tag", null);
        assertEquals(twotags, labels(suggestions));

        maybeSleep();

        // ws loader

        NuxeoRemotingBean remoting = new NuxeoRemotingBean();
        String sid = remoting.connect("Administrator", "Administrator");
        DocumentSnapshot snapshot = remoting.getDocumentSnapshot(sid, file1Id);
        DocumentProperty[] props = snapshot.getNoBlobProperties();
        Comparator<DocumentProperty> propsComparator = new Comparator<DocumentProperty>() {

            @Override
            public int compare(DocumentProperty o1, DocumentProperty o2) {
                return o1.getName().compareTo(o2.getName());
            }

        };
        Arrays.sort(props, propsComparator);
        int ti = Arrays.binarySearch(props, new DocumentProperty("tags", null),
                propsComparator);
        assertTrue(ti > 0);
        String expected = "tags:othertag,mytag";
        String prop = props[ti].toString();
        if (!expected.equals(prop)) {
            // order depends on database
            expected = "tags:mytag,othertag";
        }
        assertEquals(expected, prop);
        // remove explicit tagging
        tagService.untag(session, file2Id, "mytag", null);
        tags = tagService.getDocumentTags(session, file2Id, null);
        assertTrue(tags.isEmpty());
        docIds = tagService.getTagDocumentIds(session, "mytag", "Administrator");
        assertEquals(file1set, new HashSet<String>(docIds));
        // remove all taggings on doc
        tagService.untag(session, file1Id, null, null);
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertTrue(tags.isEmpty());

        // close remote session
        remoting.disconnect(sid);
    }

    protected static Set<String> labels(List<Tag> tags) {
        Set<String> list = new HashSet<String>();
        for (Tag tag : tags) {
            list.add(tag.getLabel());
        }
        return list;
    }

    @Test
    public void testUntagOnDelete() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        session.save();
        String file1Id = file.getId();
        List<Tag> tags;

        tagService.tag(session, file1Id, "mytag", "Administrator");

        // check tag present
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.singleton("mytag"), labels(tags));

        // delete doc
        session.removeDocument(file.getRef());
        session.save();

        // wait for async tag removal
        Framework.getService(EventService.class).waitForAsyncCompletion();

        // check no more tag
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.emptySet(), labels(tags));
    }

    @Test
    public void testUntagOnTrash() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        session.save();
        String file1Id = file.getId();
        List<Tag> tags;

        tagService.tag(session, file1Id, "mytag", "Administrator");

        // check tag present
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.singleton("mytag"), labels(tags));

        // trash doc
        file.followTransition(LifeCycleConstants.DELETE_TRANSITION);
        session.save();

        // wait for async tag removal
        Framework.getService(EventService.class).waitForAsyncCompletion();

        // check no more tag
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.emptySet(), labels(tags));
    }

    @Test
    public void testCloudNormalization() throws Exception {
        List<Tag> cloud;
        cloud = new ArrayList<Tag>();
        TagServiceImpl.normalizeCloud(cloud, 0, 0, true);

        // linear
        cloud.add(new Tag("a", 3));
        TagServiceImpl.normalizeCloud(cloud, 3, 3, true);
        assertEquals(100, cloud.get(0).getWeight());

        // logarithmic
        cloud.add(new Tag("a", 3));
        TagServiceImpl.normalizeCloud(cloud, 3, 3, false);
        assertEquals(100, cloud.get(0).getWeight());

        // linear
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(100, cloud.get(1).getWeight());

        // logarithmic
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(100, cloud.get(1).getWeight());

        // linear
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(25, cloud.get(1).getWeight());
        assertEquals(100, cloud.get(2).getWeight());

        // logarithmic
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(43, cloud.get(1).getWeight());
        assertEquals(100, cloud.get(2).getWeight());

        // linear
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        cloud.add(new Tag("d", 12));
        TagServiceImpl.normalizeCloud(cloud, 1, 12, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(9, cloud.get(1).getWeight());
        assertEquals(36, cloud.get(2).getWeight());
        assertEquals(100, cloud.get(3).getWeight());

        // logarithmic
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        cloud.add(new Tag("d", 12));
        TagServiceImpl.normalizeCloud(cloud, 1, 12, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(28, cloud.get(1).getWeight());
        assertEquals(65, cloud.get(2).getWeight());
        assertEquals(100, cloud.get(3).getWeight());
    }
}
