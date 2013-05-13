/*
 * Copyright 2013 Proofpoint Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.kairosdb.core.http;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricName;
import org.kairosdb.core.reporting.KairosMetricRegistry;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.kairosdb.core.reporting.KairosMetricRegistry.Tag;
import static org.kairosdb.util.Preconditions.checkNotNullOrEmpty;

public class MonitorFilter implements Filter
{
	private final KairosMetricRegistry metricsRegistry;
	private final Map<String, Counter> counterMap = new HashMap<String, Counter>();
	private final String hostname;

	@Inject
	public MonitorFilter(KairosMetricRegistry metricsRegistry, @Named("HOSTNAME")String hostname)
	{
		this.metricsRegistry = checkNotNull(metricsRegistry);
		this.hostname = checkNotNullOrEmpty(hostname);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		String path = ((HttpServletRequest)servletRequest).getRequestURI();
		int index = path.lastIndexOf('/');
		if (index > -1)
		{
			String resourceName = path.substring(index + 1);
			Counter counter = counterMap.get(resourceName);
			if (counter == null)
			{
				counter = metricsRegistry.newCounter(new MetricName("kairosdb", "protocol", "http_request_count", resourceName),
						new Tag("host", hostname),
						new Tag("method", resourceName));
				counterMap.put(resourceName, counter);
			}
			counter.inc();
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy()
	{
	}
}