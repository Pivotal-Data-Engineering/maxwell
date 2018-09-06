package com.zendesk.maxwell.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.zendesk.maxwell.MaxwellContext;
import com.zendesk.maxwell.row.RowMap;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxwellElasticsearchProducer extends AbstractProducer {

	private static final Logger logger = LoggerFactory.getLogger(MaxwellElasticsearchProducer.class);
	private final String elasticUrl;
	private final String elasticUser;
	private final String elasticPassword;
	private final ObjectMapper mapper;
	private final CloseableHttpClient httpClient;

	public MaxwellElasticsearchProducer(MaxwellContext context) {
		super(context);
		elasticUrl = context.getConfig().elasticUrl;
		elasticUser = context.getConfig().elasticUser;
		elasticPassword = context.getConfig().elasticPassword;
		mapper = new ObjectMapper();
		try {
			httpClient = HttpClients.custom()
			.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (x509Certificates, s) -> true).build())
			.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		System.out.printf("ES URL: %s, ES user: %s, ES password: %s\n", elasticUrl, elasticUser, elasticPassword);
	}

	private Map<String, Object> mapRemoveNulls(Map<String, Object> map) {
		for (String key : map.keySet()) {
			if (null == map.get(key)) {
				map.remove(key);
			}
		}
		return map;
	}

	@Override
	public void push(RowMap r) throws Exception {
		if (!r.shouldOutput(outputConfig)) {
			context.setPosition(r.getNextPosition());
			return;
		}

		/*
		 * Avoid verification of server SSL certificate  Per the link below, it appears this must
		 * occur before every HTTP operation: https://github.com/Kong/unirest-java/issues/195
		 */
		Unirest.setHttpClient(httpClient);

		String pk = r.pkAsConcatString(); // Primary key
		String msg = r.toJSON(outputConfig);
		String esIndex = r.getDatabase();
		String esType = r.getTable();
		String op = r.getRowType();

		try {
			if ("INSERT".equalsIgnoreCase(op)) {
				String url = String.format("%s/%s/%s/%s", elasticUrl, esIndex, esType, pk);
				Map<String, Object> dataMap = mapRemoveNulls(r.getData());
				String dataMapJSON = mapper.writeValueAsString(dataMap);
				HttpResponse<JsonNode> jsonResponse = Unirest.put(url).basicAuth(elasticUser, elasticPassword)
						.header("accept", "application/json").header("Content-Type", "application/json")
						.body(dataMapJSON).asJson();
				System.out.printf("Response body: %s, status: %d\n", jsonResponse.getBody(), jsonResponse.getStatus());
			} else if ("UPDATE".equalsIgnoreCase(op)) {
				String url = String.format("%s/%s/%s/%s/_update", elasticUrl, esIndex, esType, pk);
				Map<String, Object> newDataMap = mapRemoveNulls(r.getData());
				Map<String, Object> oldDataMap = mapRemoveNulls(r.getOldData());
				Map<String, Object> changedDataMap = new HashMap<>();
				// oldDataMap will only contain values which were changed
				for (String key : oldDataMap.keySet()) {
					changedDataMap.put(key, newDataMap.get(key));
				}
				// For an UPDATE, Elasticsearch requires a data structure like:
				// { "doc": { ... }}
				Map<String, Map<String, Object>> docMap = new HashMap<>();
				docMap.put("doc", changedDataMap);
				String dataMapJSON = mapper.writeValueAsString(docMap);
				HttpResponse<JsonNode> jsonResponse = Unirest.post(url).basicAuth(elasticUser, elasticPassword)
						.header("accept", "application/json").header("Content-Type", "application/json")
						.body(dataMapJSON).asJson();
				System.out.printf("Response body: %s, status: %d\n", jsonResponse.getBody(), jsonResponse.getStatus());
			} else if ("DELETE".equalsIgnoreCase(op)) {
				String url = String.format("%s/%s/%s/%s", elasticUrl, esIndex, esType, pk);
				HttpResponse<JsonNode> jsonResponse = Unirest.delete(url).basicAuth(elasticUser, elasticPassword)
						.header("accept", "application/json").asJson();
				System.out.printf("Response body: %s, status: %d\n", jsonResponse.getBody(), jsonResponse.getStatus());
			}
			// TODO: Handle TRUNCATE? What about DROP TABLE?
			System.out.printf("PK: %s, ES index: %s, ES type: %s, OP: %s, msg: %s\n", pk, esIndex, esType, op, msg);
			this.succeededMessageCount.inc();
			this.succeededMessageMeter.mark();
		} catch (Exception e) {
			this.failedMessageCount.inc();
			this.failedMessageMeter.mark();
			logger.error("Exception during put", e);
			if (!context.getConfig().ignoreProducerError) {
				throw new RuntimeException(e);
			}
		}

		if (r.isTXCommit()) {
			context.setPosition(r.getNextPosition());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("message ...");
		}
	}
}
