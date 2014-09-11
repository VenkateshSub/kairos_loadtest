package org.kairosdb.loadtest;


import org.kairosdb.client.Client;
import org.kairosdb.client.TelnetClient;
import org.kairosdb.client.builder.MetricBuilder;
import org.kairosdb.client.builder.QueryBuilder;
import org.kairosdb.client.response.GetResponse;
import org.kairosdb.client.response.QueryResponse;
import org.kairosdb.client.response.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URISyntaxException;

public class Main
	{
	public static void main(String[] args) throws IOException
		{
		System.out.println("Hello");

		Main main = new Main();
		main.loadTelnet("query_test_60k_big_tags", 60000, 10);
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
		TelnetClient client = new TelnetClient("kairos-mini", 4242);

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

				client.pushMetrics(mb);
				//os.println("put " + testName + " " + String.valueOf(i + start) + " 42 row=" + rowCount + " host=abc.123.ethernet.com customer_id=thompsonrouters");
				}

			//if (i % 10 == 0)
			}

		//os.close();
		//sock.close();
		client.shutdown();
		}
	}