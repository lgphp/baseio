/*
 * Copyright 2015-2017 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.generallycloud.baseio.component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.generallycloud.baseio.common.Logger;
import com.generallycloud.baseio.common.LoggerFactory;
import com.generallycloud.baseio.common.LoggerUtil;
import com.generallycloud.baseio.configuration.ServerConfiguration;
import com.generallycloud.baseio.live.AbstractLifeCycleListener;
import com.generallycloud.baseio.live.LifeCycle;
import com.generallycloud.baseio.live.LifeCycleListener;

public class ChannelContextListener extends AbstractLifeCycleListener implements LifeCycleListener {

	private Logger		logger		= LoggerFactory.getLogger(ChannelContextListener.class);

	@Override
	public int lifeCycleListenerSortIndex() {
		return 999;
	}

	@Override
	public void lifeCycleStarted(LifeCycle lifeCycle) {
//		LoggerUtil.prettyNIOServerLog(logger, "CONTEXT加载完成");
	}

	@Override
	public void lifeCycleFailure(LifeCycle lifeCycle, Exception exception) {
		// NIOConnector connector = (NIOConnector) lifeCycle;
		logger.error(exception.getMessage(), exception);
	}

	@Override
	public void lifeCycleStopped(LifeCycle lifeCycle) {
		LoggerUtil.prettyNIOServerLog(logger, "service stoped");
	}

	@Override
	public void lifeCycleStopping(LifeCycle lifeCycle) {
		ChannelContext context = (ChannelContext) lifeCycle;
		
		if (context == null) {
			LoggerUtil.prettyNIOServerLog(logger, "service start failed, prepare to stop ...");
			return;
		}
		
		ServerConfiguration configuration = context.getServerConfiguration();
		
		BigDecimal time = new BigDecimal(System.currentTimeMillis() - context.getStartupTime());
		BigDecimal anHour = new BigDecimal(60 * 60 * 1000);
		BigDecimal hour = time.divide(anHour, 3, RoundingMode.HALF_UP);
		String[] params = { String.valueOf(configuration.getSERVER_PORT()), String.valueOf(hour) };
		LoggerUtil.prettyNIOServerLog(logger, "service running @127.0.0.1:{} for {} hours", params);
		LoggerUtil.prettyNIOServerLog(logger, "begin to stop service, please wait ...");
	}

}
