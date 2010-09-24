package dev;

import java.io.File;

import org.openjena.larq.IndexBuilderModel;
import org.openjena.larq.IndexBuilderString;
import org.openjena.larq.IndexLARQ;
import org.openjena.larq.LARQ;
import org.openjena.larq.assembler.AssemblerLARQ;
import org.openjena.larq.assembler.LARQAssemblerVocab;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.assembler.AssemblerUtils;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.assembler.VocabTDB;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Run {

	public static File PATH_LUCENE_INDEX = new File ("target/data/lucene");
	public static String TDB_ASSEMBLER_FILENAME = "src/test/resources/tdb.ttl";
	
	public static void main(String[] args) {
		PATH_LUCENE_INDEX.mkdirs();
		
		// Load data into TDB
		Dataset dataset = TDBFactory.assembleDataset(TDB_ASSEMBLER_FILENAME);
		Model model = dataset.getDefaultModel();
		model.add(RDF.first, RDF.first, "text");
		
		// Build Lucene index
        IndexBuilderModel larqBuilder = new IndexBuilderString(PATH_LUCENE_INDEX);
        larqBuilder.indexStatements(model.listStatements());
        larqBuilder.closeWriter();
        
        // Register the index globally to ARQ
		VocabTDB.assemblerClass(null, LARQAssemblerVocab.tTextIndex, new AssemblerLARQ());
		IndexLARQ index = (IndexLARQ)AssemblerUtils.build(TDB_ASSEMBLER_FILENAME, LARQAssemblerVocab.tTextIndex);
        LARQ.setDefaultIndex(index);
        
        // Register larqBuilder as listener for updates
        larqBuilder = new IndexBuilderString(PATH_LUCENE_INDEX);
        model.register(larqBuilder);
        model.add(ResourceFactory.createResource("foo"), RDFS.label, "text");

        // Perform a query
        Query q = QueryFactory.create(
                "PREFIX larq:     <http://openjena.org/LARQ/property#>" +
                "" +
                "select * where {" +
                "?doc ?p ?lit ." +
                "(?lit ?score ) larq:search '+text' ." +
                "}"
                );
        
        Op op = Algebra.compile(q) ;
        op = Algebra.optimize(op) ;
        System.out.println(op) ;
        
        QueryExecution qe = QueryExecutionFactory.create(q, dataset);
        ResultSet res = qe.execSelect();
        ResultSetFormatter.out(res);
        qe.close();

        larqBuilder.closeWriter();

	}

}
