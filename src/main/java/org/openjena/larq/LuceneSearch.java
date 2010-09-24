/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.larq;

import java.util.Iterator ;

import org.openjena.atlas.iterator.IteratorTruncate ;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype ;
import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.query.Query ;
import com.hp.hpl.jena.query.QueryBuildException ;
import com.hp.hpl.jena.query.QueryExecException ;
import com.hp.hpl.jena.sparql.core.Var ;
import com.hp.hpl.jena.sparql.engine.ExecutionContext ;
import com.hp.hpl.jena.sparql.engine.QueryIterator ;
import com.hp.hpl.jena.sparql.engine.binding.Binding ;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap ;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator ;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper ;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterSingleton ;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterSlice ;
import com.hp.hpl.jena.sparql.expr.ExprEvalException ;
import com.hp.hpl.jena.sparql.expr.NodeValue ;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg ;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType ;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionEval ;
import org.openjena.atlas.logging.Log ;
import com.hp.hpl.jena.sparql.util.IterLib ;
import com.hp.hpl.jena.sparql.util.NodeFactory ;
import com.hp.hpl.jena.util.iterator.Map1 ;
import com.hp.hpl.jena.util.iterator.Map1Iterator ;

/** Base class for searching a IndexLARQ */

public abstract class LuceneSearch extends PropertyFunctionEval
{
    protected LuceneSearch()
    {
        super(PropFuncArgType.PF_ARG_EITHER,
              PropFuncArgType.PF_ARG_EITHER) ;
    }

    protected abstract IndexLARQ getIndex(ExecutionContext execCxt) ;
    
    @Override
    public void build(PropFuncArg argSubject, Node predicate, PropFuncArg argObject, ExecutionContext execCxt)
    {
        super.build(argSubject, predicate, argObject, execCxt) ;
        if ( getIndex(execCxt) == null )
            throw new QueryBuildException("Index not found") ;

        if ( argSubject.isList() && argSubject.getArgListSize() != 2 )
                throw new QueryBuildException("Subject has "+argSubject.getArgList().size()+" elements, not 2: "+argSubject) ;
        
        if ( argObject.isList() && (argObject.getArgListSize() != 2 && argObject.getArgListSize() != 3) )
                throw new QueryBuildException("Object has "+argObject.getArgList().size()+" elements, not 2 or 3: "+argObject) ;
    }
    
    @Override
    public QueryIterator execEvaluated(Binding binding, PropFuncArg argSubject, Node predicate, PropFuncArg argObject, ExecutionContext execCxt)
    {
    	try { 
    		return execEvaluatedProtected(binding, argSubject, predicate,  argObject,  execCxt) ;
    	} catch (RuntimeException ex)
    	{
    		Log.fatal(this, "Exception from Lucene search", ex) ;
    		throw ex ;
    	}
    }

    private QueryIterator execEvaluatedProtected(Binding binding, PropFuncArg argSubject, Node predicate, PropFuncArg argObject, ExecutionContext execCxt)
    {	
        Node match = null ;
        Node score = null ;
        
        Node searchString = null ;
        long limit = Query.NOLIMIT ;
        float scoreLimit = -1.0f ;
        
        if ( argSubject.isList() )
        {
            // Length checked in build
            match = argSubject.getArg(0) ;
            score = argSubject.getArg(1) ;
            
            if ( ! score.isVariable() )
                throw new QueryExecException("Hit score is not a variable: "+argSubject) ;
        }
        else
        {
            match = argSubject.getArg() ;
            //score = null ;
        }
        
        if ( argObject.isList() )
        {
            // Length checked in build
            searchString = argObject.getArg(0) ;
            
            for ( int i = 1 ; i < argObject.getArgListSize() ; i++ )
            {
                Node n = argObject.getArg(i) ;
                int nInt = asInteger(n) ;
                if ( isInteger(nInt) )
                {
                    if ( limit > 0 )
                        throw new ExprEvalException("2 potential limits to Lucene search: "+argObject) ;
                    limit = nInt ;
                    if ( limit < 0 )
                        limit = Query.NOLIMIT ;
                    continue ;
                }
                
                float nFloat = asFloat(n) ;
                if ( isFloat(nFloat) )
                {
                    if ( scoreLimit > 0 )
                        throw new ExprEvalException("2 potential score limits to Lucene search: "+argObject) ;
                    if ( nFloat < 0 )
                        throw new ExprEvalException("Negative score limit to Lucene search: "+argObject) ;
                    scoreLimit = nFloat ;
                    continue ;
                }
                throw new ExprEvalException("Bad argument to Lucene search: "+argObject) ;
            }
            
            if ( scoreLimit < 0 )
                scoreLimit = 0.0f ;

            if ( ! isValidSearchString(searchString) )
                return new QueryIterNullIterator(execCxt) ;
        }
        else
        {
            searchString = argObject.getArg() ;
            limit = Query.NOLIMIT ;
            scoreLimit = 0.0f ;
        }
        
        if ( !isValidSearchString(searchString) )
            return IterLib.noResults(execCxt) ;

        String qs = asString(searchString) ;
        
        if ( qs == null )
        {
            Log.warn(this, "Not a string (it was a moment ago!): "+searchString) ;
            return new QueryIterNullIterator(execCxt) ;
        }
        
        Var scoreVar = (score==null)?null:Var.alloc(score) ;
        
        if ( match.isVariable() )
            return varSubject(binding, 
                              Var.alloc(match), scoreVar,
                              qs, limit, scoreLimit,
                              execCxt) ;
        else
            return boundSubject(binding, 
                                match, scoreVar, 
                                qs, limit, scoreLimit,
                                execCxt) ;
    }
    
    private static boolean isValidSearchString(Node searchString)
    {
        if ( !searchString.isLiteral() )
        {
            Log.warn(LuceneSearch.class, "Not a string: "+searchString) ;
            return false ;
        }

        if ( searchString.getLiteralDatatypeURI() != null )
        {
            Log.warn(LuceneSearch.class, "Not a plain string: "+searchString) ;
            return false ;
        }

        if ( searchString.getLiteralLanguage() != null && ! searchString.getLiteralLanguage().equals("") )
        {
            Log.warn(LuceneSearch.class, "Not a plain string (has lang tag): "+searchString) ;
            return false ;
        }
        return true ;
    }
    
    public QueryIterator varSubject(Binding binding, 
                                    Var match, Var score,
                                    String searchString, long limit, float scoreLimit,
                                    ExecutionContext execCxt)
    {
        Iterator<HitLARQ> iter = getIndex(execCxt).search(searchString) ;
        
        if ( scoreLimit > 0 )
            iter = new IteratorTruncate<HitLARQ>(new ScoreTest(scoreLimit), iter) ;
        
        HitConverter converter = new HitConverter(binding, match, score) ;
        
        Iterator<Binding> iter2 = new Map1Iterator<HitLARQ, Binding>(converter, iter) ;
        QueryIterator qIter = new QueryIterPlainWrapper(iter2, execCxt) ;

        if ( limit >= 0 )
            qIter = new QueryIterSlice(qIter, 0, limit, execCxt) ;
        return qIter ;
    }
    
    static class HitConverter implements Map1<HitLARQ, Binding>
    {
        private Binding binding ;
        private Var match ;
        private Var score ;
        
        HitConverter(Binding binding, Var matchVar, Var score)
        {
            this.binding = binding ;
            this.match = matchVar ;
            this.score = score ;
        }
        
        public Binding map1(HitLARQ hit)
        {
            Binding b = new BindingMap(binding) ;
            b.add(match, hit.getNode()) ;
            if ( score != null )
                b.add(score, NodeFactory.floatToNode(hit.getScore())) ;
            return b ;
        }
        
    }
    
    public QueryIterator boundSubject(Binding binding, 
                                      Node match, Var score,
                                      String searchString, long limit, float scoreLimit,
                                      ExecutionContext execCxt)
    {
        HitLARQ hit = getIndex(execCxt).contains(match, searchString) ;
        
        if ( hit == null )
            return new QueryIterNullIterator(execCxt) ;
        if ( score == null ) 
            return QueryIterSingleton.create(binding, execCxt) ;
        return IterLib.oneResult(binding, score, NodeFactory.floatToNode(hit.getScore()), execCxt) ;
    }

    static private String asString(Node node)
    {
        if ( node.getLiteralDatatype() != null
            && ! node.getLiteralDatatype().equals(XSDDatatype.XSDstring) )
            return null ;
        return node.getLiteralLexicalForm() ;
    }

    static private float asFloat(Node n)
    {
        if ( n == null ) return Float.MIN_VALUE ;
        NodeValue nv = NodeValue.makeNode(n) ;
        if ( nv.isFloat() )
            return nv.getFloat() ;
        return Float.MIN_VALUE ;
    }

    static private int asInteger(Node n)
    {
        if ( n == null ) return Integer.MIN_VALUE ;
        return NodeFactory.nodeToInt(n) ;
    }
    
    static private boolean isInteger(int i) { return i != Integer.MIN_VALUE ; }
    static private boolean isFloat(float f) { return f != Float.MIN_VALUE ; }
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