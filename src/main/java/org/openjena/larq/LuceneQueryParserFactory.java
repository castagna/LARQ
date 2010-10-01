package org.openjena.larq;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;

public class LuceneQueryParserFactory {

	public static QueryParser create (Analyzer analyzer) {
		return new QueryParser(Version.LUCENE_29, LARQ.fIndex, analyzer);
	}

	public static QueryParser create () {
		return new QueryParser(Version.LUCENE_29, LARQ.fIndex, new StandardAnalyzer(Version.LUCENE_29));
	}
	
}
