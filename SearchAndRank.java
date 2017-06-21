// lucene-6.4.1

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchAndRank {
	
	public void index(List<DocumentInCollection> docs) throws IOException {
		// implement the Lucene indexing here	
		//StandardAnalyzer includes stop-words, EnglishAnalyzer includes stop-words and PorterStem
		//Create analyzer: http://stackoverflow.com/questions/21945600/porterstemmer-in-lucene
		Analyzer analyzer = new EnglishAnalyzer();
		// file-based search index
		Path save_path = Paths.get("/Users/shichangtai/Desktop/Information_Retrieval/assignment2/index");
		Directory directory = FSDirectory.open(save_path);
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		// index files would be updated rather than construct new files
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		// Remember to use the same Similarity when search
		// Implementation of Similarity with the Vector Space Model, TF-IDF and Cosine similarity
		ClassicSimilarity VSM = new ClassicSimilarity();
		config.setSimilarity(VSM);
	        IndexWriter iwriter = new IndexWriter(directory, config);   
		for(int i=0;i<docs.size();i++){
			// Extract data from each document
			DocumentInCollection temp = docs.get(i);
			String title = temp.getTitle();
			String abstractText = temp.getAbstractText();
			String query = temp.getQuery();
			String relevance = Boolean.toString(temp.isRelevant());
			String taskNum = Integer.toString(temp.getSearchTaskNumber()); 
			// Write data into index
			Document doc = new Document();
			// StrFields cannot have any analysis or filters applied, and will only give results for exact matches
			// TextFields usually have a tokenizer and text analysis attached
		        doc.add(new Field("title", title, TextField.TYPE_STORED));
		        doc.add(new Field("abstractText", abstractText, TextField.TYPE_STORED));
		    	doc.add(new Field("query", query, TextField.TYPE_STORED));
		    	// Focus on items that are marked with search task number
		    	doc.add(new Field("taskNum", taskNum, TextField.TYPE_STORED));
		    	// relevance is just saved
		    	doc.add(new StoredField("relevance", relevance, TextField.TYPE_STORED));
		    	iwriter.addDocument(doc);
		}
	    	iwriter.close();
	    	directory.close();
	}
	
	public List<Document> search(List<String> queryWords, int taskNum) throws IOException, ParseException {
		
		printQuery(queryWords, taskNum);

		List<Document> results = new LinkedList<Document>();

		// implement the Lucene search here
		Path save_path=Paths.get("/Users/shichangtai/Desktop/Information_Retrieval/assignment2/index");
		Directory directory = FSDirectory.open(save_path);
	    	DirectoryReader ireader = DirectoryReader.open(directory);
	    	IndexSearcher isearcher = new IndexSearcher(ireader);
	   	// use the same similarity with index process
	    	ClassicSimilarity VSM = new ClassicSimilarity();
	   	isearcher.setSimilarity(VSM);
	    
		// use the same analyzer with index process
		Analyzer analyzer = new EnglishAnalyzer();    
	    	BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
	    	QueryParser titleParser = new QueryParser("title", analyzer);
	    	QueryParser textParser = new QueryParser("abstractText", analyzer);
	    	QueryParser numParser = new QueryParser("taskNum", analyzer);
	    	if (queryWords!=null && taskNum!=0){    	
	    	for(String query_item:queryWords){
	    		booleanQuery.add(titleParser.parse(query_item), BooleanClause.Occur.SHOULD);
	    		booleanQuery.add(textParser.parse(query_item), BooleanClause.Occur.SHOULD);
			}
	    	// query task number
	    	booleanQuery.add(numParser.parse(Integer.toString(taskNum)), BooleanClause.Occur.MUST);
	    } 
	    
	    ScoreDoc[] hits = isearcher.search(booleanQuery.build(),10000).scoreDocs;
	    for (int i = 0; i < hits.length; i++) {
		      Document hitDoc = isearcher.doc(hits[i].doc);
		      results.add(hitDoc);
		      // explain the calculation of scores
		      //System.out.print(isearcher.explain(booleanQuery.build(), hits[i].doc));
		}
	    
	    ireader.close();
	    directory.close();
		
		return results;
	}
	
	public void printQuery(List<String> queryWords, int taskNum) {
		if (queryWords != null) {
			System.out.println("Query words: "+queryWords);
			System.out.println("Task number: "+taskNum);
		}
	}
	
	public void printResults(List<Document> results) {
		if (results.size() > 0) {
			// Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				// print rank, task number and relevance
				System.out.println(" " + (i+1) + ". " + results.get(i).get("taskNum") + "  " + results.get(i).get("relevance"));
		}
		else
			System.out.println(" no results");
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		if (args.length > 0) {
			SearchAndRank engine = new SearchAndRank();		
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();
			// implement index
			engine.index(docs);
			
			// just one query example, for task number 5
			List<String> queryWords;
			queryWords = new LinkedList<String>();
			queryWords.add("languages");
			queryWords.add("data");
			queryWords.add("extraction");
			
			// search results
			List<Document> results = engine.search(queryWords,5);
			engine.printResults(results);
			
			// test the data in source xml file
			/*
			for (int i=0; i<10; ++i){
				System.out.println(docs.get(i).toString());
			}
			*/
		}
		else
			System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
	}
}

