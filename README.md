# Information-Retrieval
*Index, Search and Rank*

Project is implemented with Lucene: [https://lucene.apache.org/core/6_6_0/index.html]

Data: A series of documents with title, text, topic number and relevance

Task: Building a search engine

Evaluation: averaged 11-step precision recall curves 

Index documents with EnglishAnalyzer (include stop-words and PorterStemmer), make the same deal with query, then implement comparison with the Vector Space Model, TF-IDF and Cosine similarity. Return the results after rank.

For documents which have the topic "multiple language information retrieval", here gives two query results.

![query2](query2.png)

![query1](query1.png)

To evaluate the second query result, plot the averaged 11-step P-R curve.

![pr-curve](curves.png)
