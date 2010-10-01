package dev;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.openjena.larq.IndexBuilderString;
import org.openjena.larq.IndexLARQ;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.FileManager;

public class Remove {

	public static void main(String[] args) throws Exception {
		Directory directory = new RAMDirectory();
		IndexWriter indexWriter = new IndexWriter(directory, new StandardAnalyzer(Version.LUCENE_29), MaxFieldLength.UNLIMITED);
        IndexBuilderString indexBuilder = new IndexBuilderString(indexWriter);
		Model model = ModelFactory.createDefaultModel();
        model.register(indexBuilder) ;
        FileManager.get().readModel(model, "src/test/resources/LARQ/data-1.ttl") ;
        

        model.removeAll(ResourceFactory.createResource("http://example/doc3"), (Property)null, (RDFNode)null);
        indexBuilder.closeWriter() ;

        
        IndexLARQ index = indexBuilder.getIndex() ;
        System.out.println(index.searchModelByIndex("keyword").hasNext()) ;
        
	}

}
