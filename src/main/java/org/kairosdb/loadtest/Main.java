package org.kairosdb.loadtest;


import org.kairosdb.client.Client;
import org.kairosdb.client.HttpClient;
import org.kairosdb.client.TelnetClient;
import org.kairosdb.client.builder.*;
import org.kairosdb.client.response.*;
import org.kairosdb.client.response.grouping.DefaultGroupResult;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.List;

public class Main
	{
	public static final String DATASTORE_QUERY_TIME = "kairosdb.datastore.query_time";
	public static final String KEY_QUERY_TIME = "kairosdb.datastore.cassandra.key_query_time";
	public static final String HTTP_QUERY_TIME = "kairosdb.http.query_time";
	public static final String HTTP_REQUEST_TIME = "kairosdb.http.request_time";

	//private static final long LOAD_TEST_SIZE = 1000000L;
	private static final long LOAD_TEST_SIZE = 10000;
	private final HttpClient m_httpClient;
	private TelnetClient m_telnetClient;

	public static void main(String[] args) throws IOException, URISyntaxException, DataFormatException, InterruptedException
		{
		PrintStream printStream;

		if (args.length == 1)
			printStream = new PrintStream(new File(args[0]));
		else
			printStream = System.out;

		printStream.println(RunData.printHeader());

		//Main main = new Main("kairos-mini", 4242);
		Main main = new Main("localhost", 4242);

		//Start 1 million load test
		for (int rowCount = 1; rowCount < 16; rowCount ++)
			{
			RunData rd = new RunData(rowCount);
			String metricName = "load_1million_"+rowCount+"_rows";
			rd.setLoadTime(main.loadTelnet(metricName, rowCount, (LOAD_TEST_SIZE / rowCount)));
			Thread.sleep(2000);
			main.queryMetric(metricName, rd);
			printStream.println(rd.toString());
			}

		for (int rowCount = 16; rowCount <= 1024; rowCount *= 2)
			{
			RunData rd = new RunData(rowCount);
			String metricName = "load_1million_"+rowCount+"_rows";
			main.loadTelnet(metricName, rowCount, (LOAD_TEST_SIZE / rowCount));
			Thread.sleep(2000);
			main.queryMetric(metricName, rd);
			printStream.println(rd.toString());
			}

		//main.loadTelnet("query_test_60k_big_tags", 60000, 10);

		main.close();
		}

	public Main(String host, int port) throws IOException
		{
		m_telnetClient = new TelnetClient(host, port);
		m_httpClient = new HttpClient("http://"+host+":8080");
		}

	public void close() throws IOException
		{
		m_telnetClient.shutdown();
		}

	private long getDataPoint(Query query) throws DataFormatException
		{
		Result firstResultByGroup = query.getFirstResultByGroup(new DefaultGroupResult("type", "number"));
		if (firstResultByGroup != null)
			{
			List<DataPoint> dataPoints = firstResultByGroup.getDataPoints();
			if (dataPoints.size() != 0)
				return (dataPoints.get(0).longValue());
			}

		return -1;
		}

	public void queryMetric(String metricName, RunData runData) throws IOException, URISyntaxException, DataFormatException, InterruptedException
		{
		QueryBuilder qb = QueryBuilder.getInstance();

		qb.setStart(1, TimeUnit.MINUTES);
		qb.addMetric(metricName);

		long start = System.currentTimeMillis();
		QueryResponse queryResponse = m_httpClient.query(qb);
		long end = System.currentTimeMillis();
		if (queryResponse.getStatusCode() != 200)
			return;

		runData.setSampleSize(queryResponse.getQueries().get(0).getSampleSize());
		runData.setClientQueryTime(end - start);

		Thread.sleep(1000);

		//get stats of above query
		qb = QueryBuilder.getInstance();
		qb.setStart(1, TimeUnit.MINUTES);
		QueryMetric queryMetric = qb.addMetric(DATASTORE_QUERY_TIME);
		queryMetric.setOrder(QueryMetric.Order.DESCENDING);
		queryMetric.setLimit(1);

		queryMetric = qb.addMetric(KEY_QUERY_TIME);
		queryMetric.setOrder(QueryMetric.Order.DESCENDING);
		queryMetric.setLimit(1);

		queryMetric = qb.addMetric(HTTP_QUERY_TIME);
		queryMetric.setOrder(QueryMetric.Order.DESCENDING);
		queryMetric.setLimit(1);

		queryMetric = qb.addMetric(HTTP_REQUEST_TIME);
		queryMetric.setOrder(QueryMetric.Order.DESCENDING);
		queryMetric.setLimit(1);

		QueryResponse response = m_httpClient.query(qb);
		//System.out.println(response.getStatusCode());

		runData.setKairosDatastoreQueryTime(getDataPoint(response.getQueryResponse(DATASTORE_QUERY_TIME)));
		runData.setKairosKeyQueryTime(getDataPoint(response.getQueryResponse(KEY_QUERY_TIME)));
		runData.setKairosQueryTime(getDataPoint(response.getQueryResponse(HTTP_QUERY_TIME)));
		runData.setKairosRequestTime(getDataPoint(response.getQueryResponse(HTTP_REQUEST_TIME)));
		}

	public long loadTelnet(String metricName, long rows, long width) throws IOException
		{
		long start = System.currentTimeMillis();
		loadTelnetInternal(metricName, rows, width);
		long end = System.currentTimeMillis();

		return (end - start);
		}


	private void loadTelnetInternal(String metricName, long rows, long width) throws IOException
		{
		//Start time is the current time minus the width of each row.
		//So if the width is 10 then the data will be inserted in the last 10 milliseconds.
		long start = System.currentTimeMillis() - width;
		//PrintWriter os = new PrintWriter(sock.getOutputStream());

		long i = 0;
		for (; i < width; i++)
			{
			for (long rowCount = 0L; rowCount < rows; rowCount++)
				{
				//We add extra tags to make the data larger
				MetricBuilder mb = MetricBuilder.getInstance();

				mb.addMetric(metricName).addDataPoint(i+start, 42).addTag("row", String.valueOf(rowCount)).addTag("host", "abc.123.ethernet.com").addTag("customer_id", "thompsonrouters");

				m_telnetClient.pushMetrics(mb);
				//os.println("put " + testName + " " + String.valueOf(i + start) + " 42 row=" + rowCount + " host=abc.123.ethernet.com customer_id=thompsonrouters");
				}

			//if (i % 10 == 0)
			}
		}
	}