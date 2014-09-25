package org.kairosdb.loadtest;

/**
 Created by bhawkins on 9/25/14.
 */
public class RunData
	{
	private long m_rowCount;
	private long m_sampleSize;
	private long m_loadTime;
	private long m_clientQueryTime;
	private long m_kairosKeyQueryTime;
	private long m_kairosDatastoreQueryTime;
	private long m_kairosQueryTime;
	private long m_kairosRequestTime;

	public RunData(long rowCount)
		{
		m_rowCount = rowCount;
		}

	public void setSampleSize(long sampleSize)
		{
		m_sampleSize = sampleSize;
		}

	public void setLoadTime(long loadTime)
		{
		m_loadTime = loadTime;
		}

	public void setClientQueryTime(long clientQueryTime)
		{
		m_clientQueryTime = clientQueryTime;
		}

	public void setKairosKeyQueryTime(long kairosKeyQueryTime)
		{
		m_kairosKeyQueryTime = kairosKeyQueryTime;
		}

	public void setKairosDatastoreQueryTime(long kairosDatastoreQueryTime)
		{
		m_kairosDatastoreQueryTime = kairosDatastoreQueryTime;
		}

	public void setKairosQueryTime(long kairosQueryTime)
		{
		m_kairosQueryTime = kairosQueryTime;
		}

	public void setKairosRequestTime(long kairosRequestTime)
		{
		m_kairosRequestTime = kairosRequestTime;
		}

	public static String printHeader()
		{
		return
				"rowCount"+
				", sampleSize"+
				", loadTime" +
				", clientQueryTime" +
				", kairosKeyQueryTime" +
				", kairosDatastoreQueryTime" +
				", kairosQueryTime" +
				", kairosRequestTime";
		}

	@Override
	public String toString()
		{
		return m_rowCount +
				", " + m_sampleSize +
				", " + m_loadTime +
				", " + m_clientQueryTime +
				", " + m_kairosKeyQueryTime +
				", " + m_kairosDatastoreQueryTime +
				", " + m_kairosQueryTime +
				", " + m_kairosRequestTime;
		}
	}
