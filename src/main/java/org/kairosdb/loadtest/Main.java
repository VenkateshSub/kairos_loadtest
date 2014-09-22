package org.kairosdb.loadtest;


import org.kairosdb.client.Client;
import org.kairosdb.client.HttpClient;
import org.kairosdb.client.TelnetClient;
import org.kairosdb.client.builder.MetricBuilder;
import org.kairosdb.client.builder.QueryBuilder;
import org.kairosdb.client.builder.TimeUnit;
import org.kairosdb.client.response.GetResponse;
import org.kairosdb.client.response.QueryResponse;
import org.kairosdb.client.response.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URISyntaxException;

public class Main
	{
	private final HttpClient m_httpClient;
	private TelnetClient m_telnetClient;

	public static void main(String[] args) throws IOException, URISyntaxException
		{
		System.out.println("Hello");

		Main main = new Main("kairos-mini", 4242);

		//Start 1 million load test
		for (int rowCount = 1; rowCount < 16; rowCount ++)
			{
			String metric = "load_1million_"+rowCount+"_rows";
			main.loadTelnet(metric, rowCount, (1000000 / rowCount));
			main.queryMetric(metric);
			}

		for (int rowCount = 16; rowCount <= 1024; rowCount *= 2)
			{
			String metric = "load_1million_"+rowCount+"_rows";
			main.loadTelnet(metric, rowCount, (1000000 / rowCount));

			main.queryMetric(metric);
			}

		//main.loadTelnet("query_test_60k_big_tags", 60000, 10);

		main.close();
		}

	public Main(String host, int port) throws IOException
		{
		m_telnetClient = new TelnetClient(host, port);
		m_httpClient = new HttpClient("http://"+host+":"+port+"/api/v1");
		}

	public void close() throws IOException
		{
		m_telnetClient.shutdown();
		}

	public void queryMetric(String metricName) throws IOException, URISyntaxException
		{
		QueryBuilder qb = QueryBuilder.getInstance();

		qb.addMetric(metricName);
		qb.setStart(5, TimeUnit.MINUTES);

		long start = System.currentTimeMillis();
		m_httpClient.query(qb);
		long end = System.currentTimeMillis();

		System.out.println("Query: "+metricName+", "+(end - start));
		}

	public void loadTelnet(String metricName, long rows, long width) throws IOException
		{
		long start = System.currentTimeMillis();
		loadTelnetInternal(metricName, rows, width);
		long end = System.currentTimeMillis();

		System.out.println(metricName+", "+(end - start));
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