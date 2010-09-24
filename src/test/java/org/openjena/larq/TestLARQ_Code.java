/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.larq;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.lucene.index.CorruptIndexException;
import org.junit.Test;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;

import org.openjena.larq.IndexBuilderNode;

public class TestLARQ_Code extends TestCase
{

    // Called every test.
    public void setUp() {}
    public void tearDown() {}

    static final String datafile = "src/test/resources/LARQ/data-1.ttl" ;
    
    public void test_ext_1()
    {
        IndexBuilderNode b = new IndexBuilderNode() ;
        Model model = ModelFactory.createDefaultModel() ;
        Resource r = model.createResource("http://example/r") ;
        b.index(r, "foo") ;
        b.closeWriter() ;
        
        IndexLARQ index = b.getIndex() ;
        NodeIterator nIter = index.searchModelByIndex("foo") ;
        assertEquals(1, TestLARQUtils.count(nIter)) ;
        nIter = index.searchModelByIndex("foo") ;
        Resource r2 = (Resource)nIter.nextNode() ;
        assertEquals(r, r2) ;
    }
    
    @Test public void test_ext_2()
    {
        IndexBuilderNode b = new IndexBuilderNode() ;
        Model model = ModelFactory.createDefaultModel() ;
        Literal lit = model.createLiteral("example") ;
        b.index(lit, "foo") ;
        b.closeWriter() ;
        
        IndexLARQ index = b.getIndex() ;
        NodeIterator nIter = index.searchModelByIndex("foo") ;
        assertEquals(1, TestLARQUtils.count(nIter)) ;
        nIter = index.searchModelByIndex("foo") ;
        Literal lit2 = (Literal)nIter.nextNode() ;
        assertEquals(lit, lit2) ;
    }

    @Test public void test_ext_3()
    {
        IndexBuilderNode b = new IndexBuilderNode() ;
        Model model = ModelFactory.createDefaultModel() ;
        Resource bnode = model.createResource() ;
        b.index(bnode, "foo") ;
        b.closeWriter() ;
        
        IndexLARQ index = b.getIndex() ;
        NodeIterator nIter = index.searchModelByIndex("foo") ;
        assertEquals(1, TestLARQUtils.count(nIter)) ;
        nIter = index.searchModelByIndex("foo") ;
        Resource bnode2 = (Resource)nIter.nextNode() ;
        assertEquals(bnode, bnode2) ;
        assertTrue(bnode2.isAnon()) ;
    }

    @Test public void test_ext_4()
    {
        IndexBuilderNode b = new IndexBuilderNode() ;
        Model model = ModelFactory.createDefaultModel() ;
        Resource r = model.createResource("http://example/r") ;
        b.index(r, "foo") ;
        b.closeWriter() ;
        
        IndexLARQ index = b.getIndex() ;
        NodeIterator nIter = index.searchModelByIndex("bah") ;
        assertFalse(nIter.hasNext()) ;
    }
    
    @Test public void test_ext_5()
    {
        IndexBuilderNode b = new IndexBuilderNode() ;
        Model model = ModelFactory.createDefaultModel() ;
        Resource r = model.createResource("http://example/r") ;
        StringReader sr = new StringReader("foo") ;
        b.index(r, sr) ;
        b.closeWriter() ;
        
        IndexLARQ index = b.getIndex() ;
        NodeIterator nIter = index.searchModelByIndex("foo") ;
        assertEquals(1, TestLARQUtils.count(nIter)) ;
        nIter = index.searchModelByIndex("foo") ;
        Resource r2 = (Resource)nIter.nextNode() ;
        assertEquals(r, r2) ;
    }
    
    // Test what happens when the index is updated after a reader index (LARQIndex) is created
    @Test public void test_ext_6()
    {
        IndexBuilderNode b = new IndexBuilderNode() ;
        Model model = ModelFactory.createDefaultModel() ;
        Resource r1 = model.createResource("http://example/r1") ;
        Resource r2 = model.createResource("http://example/r2") ;
        
        StringReader sr = new StringReader("R1") ;
        b.index(r1, sr) ;
        IndexLARQ index = b.getIndex() ;
        NodeIterator nIter = index.searchModelByIndex("R1") ;
        assertEquals(1, TestLARQUtils.count(nIter)) ;
        nIter = index.searchModelByIndex("R2") ;
        assertEquals(0, TestLARQUtils.count(nIter)) ;
        
        // Add r2.
        b.index(r2, new StringReader("R2")) ;
        // Old index - can't see R2
        nIter = index.searchModelByIndex("R2") ;
        assertEquals(0, TestLARQUtils.count(nIter)) ;
        
        // New index - can see R2
        index = b.getIndex() ;
        nIter = index.searchModelByIndex("R2") ;
        assertEquals(1, TestLARQUtils.count(nIter)) ;
    }
    
    @Test public void test_index_literal_1()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        // Search both DC title and RDFS label
        assertEquals(3,TestLARQUtils.count(nIter)) ;
        index.close() ;
    }

    @Test public void test_index_literal_2()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString(DC.title)) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        // Search just DC title
        assertEquals(2,TestLARQUtils.count(nIter)) ;
    } 
    
    @Test public void test_index_literal_3()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        // Search both DC title and RDFS label
        for ( ; nIter.hasNext(); )
        {
            RDFNode n = nIter.nextNode() ;
            assertTrue(n instanceof Literal) ;
            assertTrue(model.contains(null, null, n)) ;
            boolean b = model.contains(null, DC.title, n) ||
                        model.contains(null, RDFS.label, n) ;
            assertTrue("DC.title or RDFS.label", b) ;
        }
    }

    @Test public void test_index_literal_4()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString(DC.title)) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        // Search both DC title and RDFS label
        for ( ; nIter.hasNext(); )
        {
            RDFNode n = nIter.nextNode() ;
            assertTrue(n instanceof Literal) ;
            assertTrue(model.contains(null, DC.title, n)) ;
            assertFalse(model.contains(null, RDFS.label, n)) ;
        }
    }
    
    
    @Test public void test_index_subject_1()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderSubject()) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        // Search both DC title and RDFS label
        assertEquals(3,TestLARQUtils.count(nIter)) ;
    }
    
    @Test public void test_index_subject_2()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderSubject(DC.title)) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        // Search both DC title and RDFS label
        assertEquals(2,TestLARQUtils.count(nIter)) ;
    }

    @Test public void test_index_subject_3()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderSubject()) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        // Search both DC title and RDFS label
        for ( ; nIter.hasNext(); )
        {
            RDFNode n = nIter.nextNode() ;
            assertTrue(n instanceof Resource) ;
            assertTrue(model.contains((Resource)n, null, (RDFNode)null)) ;
            boolean b = model.contains((Resource)n, DC.title, (RDFNode)null) ||
                        model.contains((Resource)n, RDFS.label, (RDFNode)null) ;
            assertTrue("subject with DC.title or RDFS.label", b) ;
        }
    }

    @Test public void test_index_subject_4()
    { 
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderSubject(DC.title)) ; 
        NodeIterator nIter = index.searchModelByIndex(model, "+document") ;
        for ( ; nIter.hasNext(); )
        {
            RDFNode n = nIter.nextNode() ;
            assertTrue(n instanceof Resource) ;
            assertTrue(model.contains((Resource)n, null, (RDFNode)null)) ;
            assertTrue(model.contains((Resource)n, DC.title, (RDFNode)null)) ;
        }
    }

    // Negative searches
    @Test public void test_negative_1()
    {
        IndexLARQ index = TestLARQUtils.createIndex(datafile, new IndexBuilderString()) ;
        assertFalse(index.hasMatch("+iceberg")) ;
    }

    @Test public void test_negative_2()
    {
        IndexLARQ index = TestLARQUtils.createIndex(datafile, new IndexBuilderString(DC.title)) ;
        assertFalse(index.hasMatch("+iceberg")) ;
    }

    @Test public void test_negative_3()
    {
        IndexLARQ index = TestLARQUtils.createIndex(datafile, new IndexBuilderSubject()) ;
        assertFalse(index.hasMatch("+iceberg")) ;
    }
    
    @Test public void test_negative_4()
    {
        IndexLARQ index = TestLARQUtils.createIndex(datafile, new IndexBuilderSubject(DC.title)) ;
        assertFalse(index.hasMatch("+iceberg")) ;
    }
    
    @Test public void test_search_index_registration_1()
    {
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ;
        assertFalse(ARQ.getContext().isDefined(LARQ.indexKey)) ;
        try {
            LARQ.setDefaultIndex(index) ;
            assertTrue(ARQ.getContext().isDefined(LARQ.indexKey)) ;
            QueryExecution qExec = TestLARQUtils.query(model, "{ ?lit larq:search '+document' }") ;
            ResultSet rs = qExec.execSelect() ;
            assertEquals(3, TestLARQUtils.count(rs)) ;
            qExec.close() ;
            index.close() ;
            LARQ.removeDefaultIndex() ;
            assertFalse(ARQ.getContext().isDefined(LARQ.indexKey)) ;
        } finally { LARQ.removeDefaultIndex() ; }
    }
    
    @Test public void test_search_index_registration_2()
    {
        Model model = ModelFactory.createDefaultModel() ;
        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ;
        
        assertFalse(ARQ.getContext().isDefined(LARQ.indexKey)) ;
        QueryExecution qExec = TestLARQUtils.query(model, "{ ?lit larq:search '+document' }") ;
        
        try {
            LARQ.setDefaultIndex(qExec.getContext(), index) ;
            assertFalse(ARQ.getContext().isDefined(LARQ.indexKey)) ;
            assertTrue(qExec.getContext().isDefined(LARQ.indexKey)) ;
            
            ResultSet rs = qExec.execSelect() ;
            assertEquals(3, TestLARQUtils.count(rs)) ;
            qExec.close() ;
            index.close() ;
            LARQ.removeDefaultIndex(qExec.getContext()) ;
            assertFalse(qExec.getContext().isDefined(LARQ.indexKey)) ;
            assertFalse(ARQ.getContext().isDefined(LARQ.indexKey)) ;
        } finally { LARQ.removeDefaultIndex() ; }
    }
    
//    
//    @Test public void test_search_literal_1()
//    {
//        Model model = ModelFactory.createDefaultModel() ;
//        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ;
//        LARQ.setDefaultIndex(index) ;
//        QueryExecution qExec = query(model, "{ ?lit larq:search '+document' }") ;
//        ResultSet rs = qExec.execSelect() ;
//        assertEquals(3, TestLARQUtils.count(rs)) ;
//        qExec.close() ;
//        index.close() ;
//        LARQ.removeDefaultIndex() ;
//    }
//
//    @Test public void test_search_literal_2()
//    {
//        Model model = ModelFactory.createDefaultModel() ;
//        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString(DC.title)) ;
//        LARQ.setDefaultIndex(index) ;
//        QueryExecution qExec = query(model, "{ ?lit larq:search '+document' }") ;
//        ResultSet rs = qExec.execSelect() ;
//        assertEquals(2, TestLARQUtils.count(rs)) ;
//        qExec.close() ;
//        index.close() ;
//        LARQ.removeDefaultIndex() ;
//    }
//    
//    
//
//    @Test public void test_search_literal_3()
//    {
//        Model model = ModelFactory.createDefaultModel() ;
//        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ;
//        LARQ.setDefaultIndex(index) ;
//        QueryExecution qExec = query(model, 
//            "{ ?lit larq:search '+document' }") ;
//        ResultSetRewindable rs1 = ResultSetFactory.makeRewindable(qExec.execSelect()) ;
//        //ResultSetFormatter.outputAsJSON(rs1) ;
//        ResultSetRewindable rs2 = ResultSetFactory.makeRewindable(ResultSetFactory.load(results1)) ;
//        assertTrue(RSCompare.same(rs1, rs2)) ;
//        qExec.close() ;
//        index.close() ;
//        LARQ.removeDefaultIndex() ;
//    }
//
//    @Test public void test_search_literal_4()
//    {
//        Model model = ModelFactory.createDefaultModel() ;
//        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ;
//        LARQ.setDefaultIndex(index) ;
//        QueryExecution qExec = query(model, 
//            "{ ?lit larq:search '+document' . ?lit larq:search '+document'}") ;
//        ResultSetRewindable rs1 = ResultSetFactory.makeRewindable(qExec.execSelect()) ;
//        ResultSetRewindable rs2 = ResultSetFactory.makeRewindable(ResultSetFactory.load(results1)) ;
//        assertTrue(RSCompare.same(rs1, rs2)) ;
//        qExec.close() ;
//        index.close() ;
//        LARQ.removeDefaultIndex() ;
//    }
//
//    @Test public void test_search_literal_5()
//    {
//        Model model = ModelFactory.createDefaultModel() ;
//        IndexLARQ index = TestLARQUtils.createIndex(model, datafile, new IndexBuilderString()) ;
//        LARQ.setDefaultIndex(index) ;
//        QueryExecution qExec = query(model, 
//        "{ ?lit larq:search '+document' . ?doc ?p ?lit }") ;
//        ResultSetRewindable rs1 = ResultSetFactory.makeRewindable(qExec.execSelect()) ;
//        //ResultSetFormatter.outputAsJSON(rs1) ;
//        ResultSetRewindable rs2 = ResultSetFactory.makeRewindable(ResultSetFactory.load(results2)) ;
//        assertTrue(RSCompare.same(rs1, rs2)) ;
//        qExec.close() ;
//        index.close() ;
//        LARQ.removeDefaultIndex() ;
//    }
}

/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */