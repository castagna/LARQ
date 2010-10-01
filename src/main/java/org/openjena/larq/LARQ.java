/*
 * (c) Copyright 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.larq;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.openjena.larq.pfunction.search;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Blank;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.sparql.ARQConstants;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.NodeFactory;
import com.hp.hpl.jena.sparql.util.Symbol;

public class LARQ
{
    /** The ARQ property function library URI space */
    public static final String LARQPropertyFunctionLibraryURI = "http://openjena.org/LARQ/property#" ;
    
    static {
    	PropertyFunctionRegistry.get().put(LARQPropertyFunctionLibraryURI + "search", search.class);
    }

    // The field that is the index
    public static final String fIndex               = "index" ;

    public static final String fIndexHash           = "hash" ;
    
    // Object literals
    public static final String fLex                 = "lex" ;
    public static final String fLang                = "lang" ;
    public static final String fDataType            = "datatype" ;
    // Object URI
    public static final String fURI                 = "uri" ;
    // Object bnode
    public static final String fBNodeID             = "bnode" ;

    // The symbol used to register the index in the query context
    public static final Symbol indexKey     = ARQConstants.allocSymbol("lucene") ;

    public static void setDefaultIndex(IndexLARQ index)
    { setDefaultIndex(ARQ.getContext(), index) ; }
    
    public static void setDefaultIndex(Context context, IndexLARQ index)
    { context.set(LARQ.indexKey, index) ; }
    
    public static IndexLARQ getDefaultIndex()
    { return getDefaultIndex(ARQ.getContext()) ; }
    
    public static IndexLARQ getDefaultIndex(Context context)
    { return (IndexLARQ)context.get(LARQ.indexKey) ; }
    
    public static void removeDefaultIndex()
    { removeDefaultIndex(ARQ.getContext()) ; }
    
    public static void removeDefaultIndex(Context context)
    { context.unset(LARQ.indexKey) ; }

    public static void index(Document doc, Node indexNode)
    {
        if ( ! indexNode.isLiteral() )
            throw new LARQException("Not a literal: "+indexNode) ;
        index(doc, indexNode.getLiteralLexicalForm()) ;
    }        
     
    public static void index(Document doc, String indexContent)
    {
        Field indexField = new Field(LARQ.fIndex, indexContent, Field.Store.NO, Field.Index.ANALYZED) ;
        doc.add(indexField) ;

        Field indexHashField = new Field(LARQ.fIndexHash, hash(indexContent), Field.Store.NO, Field.Index.NOT_ANALYZED) ;
        doc.add(indexHashField) ;
    }        
     
    public static void index(Document doc, Reader indexContent)
    {
       	Field indexField = new Field(LARQ.fIndex, indexContent) ;
        doc.add(indexField) ;

        Field indexHashField = new Field(LARQ.fIndexHash, hash(indexContent), Field.Store.NO, Field.Index.NOT_ANALYZED) ;
       	doc.add(indexHashField) ;
    }
    
	public static Query unindex(Node node, String indexStr) throws ParseException 
	{
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(LARQ.fIndexHash, hash(indexStr))) , Occur.MUST);

        if ( node.isLiteral() ) {
        	queryLiteral (query, (Node_Literal)node);  	
        } else if ( node.isURI() ) {
        	queryURI (query, (Node_URI)node) ;
        } else if ( node.isBlank() ) {
        	queryBNode (query, (Node_Blank)node) ;
        } else {
            throw new LARQException("Can't unindex: "+node) ;
        }
        
		return query;
	}

	public static Query unindex(Node node, Reader indexContent) throws ParseException 
	{
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(LARQ.fIndexHash, hash(indexContent))) , Occur.MUST);

        if ( node.isLiteral() ) {
        	queryLiteral (query, (Node_Literal)node);  	
        } else if ( node.isURI() ) {
        	queryURI (query, (Node_URI)node) ;
        } else if ( node.isBlank() ) {
        	queryBNode (query, (Node_Blank)node) ;
        } else {
            throw new LARQException("Can't unindex: "+node) ;
        }
        
		return query;
	}
	
    public static void store(Document doc, Node node)
    {
        // Store.
        if ( node.isLiteral() )
            storeLiteral(doc, (Node_Literal)node) ;
        else if ( node.isURI() )
            storeURI(doc, (Node_URI)node) ;
        else if ( node.isBlank() )
            storeBNode(doc, (Node_Blank)node) ;
        else
            throw new LARQException("Can't store: "+node) ;
    }

    public static Node build(Document doc)
    {
        String lex = doc.get(LARQ.fLex) ;
        if ( lex != null )
            return buildLiteral(doc) ;
        String uri = doc.get(LARQ.fURI) ;
        if ( uri != null )
            return Node.createURI(uri) ;
        String bnode = doc.get(LARQ.fBNodeID) ;
        if ( bnode != null )
            return Node.createAnon(new AnonId(bnode)) ;
        throw new LARQException("Can't build: "+doc) ;
    }

    public static boolean isString(Literal literal)
    {
        RDFDatatype dtype = literal.getDatatype() ;
        if ( dtype == null )
            return true ;
        if ( dtype.equals(XSDDatatype.XSDstring) )
            return true ;
        return false ;
    }
    
    private static void storeURI(Document doc, Node_URI node)
    { 
        String x = node.getURI() ;
        Field f = new Field(LARQ.fIndex, x, Field.Store.NO, Field.Index.ANALYZED) ;
        doc.add(f) ;
        f = new Field(LARQ.fURI, x, Field.Store.YES, Field.Index.NOT_ANALYZED) ;
        doc.add(f) ;
    }

    private static void storeBNode(Document doc, Node_Blank node)
    { 
        String x = node.getBlankNodeLabel() ;
        Field f = new Field(LARQ.fIndex, x, Field.Store.NO, Field.Index.ANALYZED) ;
        doc.add(f) ;
        f = new Field(LARQ.fBNodeID, x, Field.Store.YES, Field.Index.NOT_ANALYZED) ;
        doc.add(f) ;
    }
    
    private static void storeLiteral(Document doc, Node_Literal node)
    {
        String lex = node.getLiteralLexicalForm() ;
        String datatype = node.getLiteralDatatypeURI() ;
        String lang = node.getLiteralLanguage() ;

        Field f = new Field(LARQ.fLex, lex, Field.Store.YES, Field.Index.NOT_ANALYZED) ;
        doc.add(f) ;
        
        if ( lang != null )
        {
            f = new Field(LARQ.fLang, lang, Field.Store.YES, Field.Index.NOT_ANALYZED) ;
            doc.add(f) ;
        }

        if ( datatype != null )
        {       
            f = new Field(LARQ.fDataType, datatype, Field.Store.YES, Field.Index.NOT_ANALYZED) ;
            doc.add(f) ;
        }
    }
    
    private static void queryURI(BooleanQuery query, Node_URI node)
    { 
        query.add(new TermQuery(new Term(LARQ.fURI, node.getURI())) , Occur.MUST);
    }

    private static void queryBNode(BooleanQuery query, Node_Blank node)
    { 
        query.add(new TermQuery(new Term(LARQ.fBNodeID, node.getBlankNodeLabel())) , Occur.MUST);
    }
    
    private static void queryLiteral(BooleanQuery query, Node_Literal node)
    {
        String lex = node.getLiteralLexicalForm() ;
        String datatype = node.getLiteralDatatypeURI() ;
        String lang = node.getLiteralLanguage() ;

        query.add(new TermQuery(new Term(LARQ.fLex, lex)) , Occur.MUST);
        
        if ( lang != null )
        {
            query.add(new TermQuery(new Term(LARQ.fLang, lang)) , Occur.MUST);
        }

        if ( datatype != null )
        {       
            query.add(new TermQuery(new Term(LARQ.fDataType, datatype)) , Occur.MUST);
        }
    }
    
    private static Node buildLiteral(Document doc)
    {
        String lex = doc.get(LARQ.fLex) ;
        if ( lex == null )
            return null ;
        String datatype = doc.get(LARQ.fDataType) ;
        String lang = doc.get(LARQ.fLang) ;
        return NodeFactory.createLiteralNode(lex, lang, datatype) ;
    }

    private static String hash (Node node) 
    {
        String lexForm = null ; 
        String datatypeStr = "" ;
        String langStr = "" ;
        
        if ( node.isURI() ) {
        	lexForm = node.getURI() ;
        } else if ( node.isLiteral() ) {
        	lexForm = node.getLiteralLexicalForm() ;
            datatypeStr = node.getLiteralDatatypeURI() ;
            langStr = node.getLiteralLanguage() ;
        } else if ( node.isBlank() ) {
        	lexForm = node.getBlankNodeLabel() ;
        } else {
        	throw new LARQException("Unable to hash node:"+node) ;
        }

        return hash (lexForm + "|" + langStr + "|" + datatypeStr);
    }
    
    private static String hash (Reader reader)
    {
    	StringBuffer sb = new StringBuffer();
		try {
	        int charsRead;
			do {
		    	char[] buffer = new char[1024];
		        int offset = 0;
		        int length = buffer.length;
		        charsRead = 0;
				while (offset < buffer.length) {
					charsRead = reader.read(buffer, offset, length);
					if (charsRead == -1)
						break;
					offset += charsRead;
					length -= charsRead;
				}
				sb.append(buffer);
			} while (charsRead != -1);
			reader.reset();
		} catch (IOException e) {
			new LARQException("hash", e);
		}
		
		return hash(sb.toString());
    }
    
    private static String hash (String str) 
    {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(str.getBytes("UTF8"));
            byte[] hash = digest.digest();
            BigInteger bigInt = new BigInteger(hash);
            return bigInt.toString();
        } catch (NoSuchAlgorithmException e) {
        	new LARQException("hash", e);
        } catch (UnsupportedEncodingException e) {
        	new LARQException("hash", e);
        }

        return null;
    }
    
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