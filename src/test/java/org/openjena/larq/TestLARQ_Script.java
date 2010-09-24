/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.larq;

import junit.framework.JUnit4TestAdapter ;
import junit.framework.TestCase ;
import org.junit.Test ;

import com.hp.hpl.jena.query.Query ;
import com.hp.hpl.jena.query.QueryExecution ;
import com.hp.hpl.jena.query.QueryExecutionFactory ;
import com.hp.hpl.jena.query.QueryFactory ;
import com.hp.hpl.jena.query.ResultSetFactory ;
import com.hp.hpl.jena.query.ResultSetFormatter ;
import com.hp.hpl.jena.query.larq.IndexBuilderModel ;
import com.hp.hpl.jena.query.larq.IndexBuilderString ;
import com.hp.hpl.jena.query.larq.IndexBuilderSubject ;
import com.hp.hpl.jena.query.larq.IndexLARQ ;
import com.hp.hpl.jena.query.larq.LARQ ;
import com.hp.hpl.jena.rdf.model.Model ;
import com.hp.hpl.jena.rdf.model.ModelFactory ;
import com.hp.hpl.jena.sparql.junit.QueryTest ;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable ;
import com.hp.hpl.jena.util.FileManager ;
import com.hp.hpl.jena.vocabulary.DC ;

public class TestLARQ_Script extends TestCase
{
    public static junit.framework.Test suite()
    {
        return new JUnit4TestAdapter(TestLARQ_Script.class) ;
    }
    
//    public static TestSuite suite()
//    {
//        TestSuite ts = new TestSuite(TestLARQ2.class) ;
//        ts.setName("LARQ-Scripts") ;
//        return ts ;
//    }
    
    static final String root = "LARQ/" ;
//    static final String datafile = "LARQ/data-1.ttl" ;
//    static final String results1 = "LARQ/results-1.srj" ;
//    static final String results2 = "LARQ/results-2.srj" ;
//    static final String results3 = "LARQ/results-3.srj" ;
    
    public TestLARQ_Script(String name)
    { 
        super(name) ;
    }
    
    // See TestLARQ.
    
    static void runTestScript(String queryFile, String dataFile, String resultsFile, IndexBuilderModel builder)
    {
        Query query = QueryFactory.read(root+queryFile) ;
        IndexBuilderString larqBuilder = new IndexBuilderString() ;
        Model model = ModelFactory.createDefaultModel() ; 
        model.register(builder) ;    
        FileManager.get().readModel(model, root+dataFile) ;
        model.unregister(builder) ;
        
        IndexLARQ index = builder.getIndex() ;
        LARQ.setDefaultIndex(index) ;
        
        QueryExecution qe = QueryExecutionFactory.create(query, model) ;
        ResultSetRewindable rsExpected = 
            ResultSetFactory.makeRewindable(ResultSetFactory.load(root+resultsFile)) ;
        
        ResultSetRewindable rsActual = 
            ResultSetFactory.makeRewindable(qe.execSelect()) ;
        boolean b = QueryTest.resultSetEquivalent(query, rsActual, rsExpected) ;
        if ( ! b ) 
        {
            rsActual.reset() ;
            rsExpected.reset() ;
            System.out.println("==== Different (LARQ)") ;
            System.out.println("== Actual") ;
            ResultSetFormatter.out(rsActual) ;
            System.out.println("== Expected") ;
            ResultSetFormatter.out(rsExpected) ;
        }
        
        assertTrue(b) ;
        qe.close() ; 
        LARQ.removeDefaultIndex() ;
    }
    
    @Test public void test_larq_1()
    { runTestScript("larq-q-1.rq", "data-1.ttl", "results-1.srj", new IndexBuilderString()) ; }

    @Test public void test_larq_2()
    { runTestScript("larq-q-2.rq", "data-1.ttl", "results-2.srj", new IndexBuilderString(DC.title)) ; }

    @Test public void test_larq_3()
    { runTestScript("larq-q-3.rq", "data-1.ttl", "results-3.srj", new IndexBuilderSubject(DC.title)) ; }
    
    @Test public void test_larq_4()
    { runTestScript("larq-q-4.rq", "data-1.ttl", "results-4.srj", new IndexBuilderString()) ; }
    
    @Test public void test_larq_5()
    { runTestScript("larq-q-5.rq", "data-1.ttl", "results-5.srj", new IndexBuilderString()) ; }

    @Test public void test_larq_6()
    { runTestScript("larq-q-6.rq", "data-1.ttl", "results-6.srj", new IndexBuilderString()) ; }

    @Test public void test_larq_7()
    { runTestScript("larq-q-7.rq", "data-1.ttl", "results-7.srj", new IndexBuilderString()) ; }
}

/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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