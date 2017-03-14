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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.generallycloud.baseio.buffer.ByteBuf;
import com.generallycloud.baseio.buffer.UnpooledByteBufAllocator;
import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.common.Logger;
import com.generallycloud.baseio.common.LoggerFactory;
import com.generallycloud.baseio.common.ReleaseUtil;
import com.generallycloud.baseio.component.concurrent.BufferedArrayList;
import com.generallycloud.baseio.component.concurrent.ExecutorEventLoop;
import com.generallycloud.baseio.component.concurrent.LineEventLoop;
import com.generallycloud.baseio.live.LifeCycleUtil;

/**
 * @author wangkai
 *
 */
public class SocketSelectorEventLoop extends AbstractSelectorLoop implements SocketChannelThreadContext {

	private static final Logger						logger			= LoggerFactory
			.getLogger(SocketSelectorEventLoop.class);

	private ByteBuf								buf				= null;

	private ChannelByteBufReader						byteBufReader		= null;

	private NioSocketChannelContext					context			= null;

	private ExecutorEventLoop						executorEventLoop	= null;

	private boolean								isWaitForRegist	= false;

	private SessionManager							sessionManager		= null;

	private SocketSelectorEventLoopGroup				eventLoopGroup		= null;

	private SocketSelectorBuilder						selectorBuilder	= null;

	private SocketSelector							selector			= null;

	private ReentrantLock							runLock			= new ReentrantLock();

	private int									runTask			= 0;

	private boolean								hasTask			= false;

	private int									eventQueueSize		= 0;

	private BufferedArrayList<SelectorLoopEvent>	negativeEvents		= new BufferedArrayList<>();

	private BufferedArrayList<SelectorLoopEvent>	positiveEvents		= new BufferedArrayList<>();

	private AtomicBoolean							selecting			= new AtomicBoolean();

	private ReentrantLock							isWaitForRegistLock	= new ReentrantLock();

	private UnpooledByteBufAllocator					unpooledByteBufAllocator;

	public SocketSelectorEventLoop(SocketSelectorEventLoopGroup group, int eventQueueSize,
			int coreIndex) {

		super(group.getChannelContext(), coreIndex);

		this.eventLoopGroup = group;

		this.context = group.getChannelContext();
		
		this.selectorBuilder = ((NioChannelService) context.getChannelService()).getSelectorBuilder();

		this.executorEventLoop = context.getExecutorEventLoopGroup().getNext();

		this.byteBufReader = context.getChannelByteBufReader();

		this.sessionManager = context.getSessionManager();

		this.eventQueueSize = eventQueueSize;

		this.unpooledByteBufAllocator = new UnpooledByteBufAllocator(false);
	}

	public ReentrantLock getIsWaitForRegistLock() {
		return isWaitForRegistLock;
	}

	public boolean isWaitForRegist() {
		return isWaitForRegist;
	}

	public void setWaitForRegist(boolean isWaitForRegist) {
		this.isWaitForRegist = isWaitForRegist;
	}

	public SocketSelector getSelector() {
		return selector;
	}

	@Override
	public void rebuildSelector() throws IOException {
		this.selector = rebuildSelector0();
	}

	private void waitForRegist() {

		ReentrantLock lock = getIsWaitForRegistLock();

		lock.lock();

		lock.unlock();
	}

	public void accept(NioSocketChannel channel) {

		if (!channel.isOpened()) {
			return;
		}

		try {

			accept0(channel);

		} catch (Throwable e) {

			cancelSelectionKey(channel, e);
		}
	}

	public void accept0(NioSocketChannel channel) throws Exception {

		ByteBuf buf = this.buf;

		buf.clear();

		buf.nioBuffer();

		int length = channel.read(buf);

		if (length < 1) {

			if (length == -1) {
				CloseUtil.close(channel);
			}
			return;
		}

		channel.active();

		byteBufReader.accept(channel, buf.flip());
	}

	@Override
	public void doStartup() throws IOException {

		if (executorEventLoop instanceof LineEventLoop) {
			((LineEventLoop) executorEventLoop).setMonitor(this);
		}

		LifeCycleUtil.start(unpooledByteBufAllocator);

		int readBuffer = context.getServerConfiguration().getSERVER_CHANNEL_READ_BUFFER();

		// FIXME 使用direct
		this.buf = unpooledByteBufAllocator.allocate(readBuffer);

		super.doStartup();
	}

	@Override
	protected void doStop() {

		closeEvents(positiveEvents);

		closeEvents(negativeEvents);

		CloseUtil.close(selector);

		ReleaseUtil.release(buf);

		LifeCycleUtil.stop(unpooledByteBufAllocator);
	}

	private void closeEvents(BufferedArrayList<SelectorLoopEvent> bufferedList) {

		List<SelectorLoopEvent> events = getEventBuffer(bufferedList);

		for (SelectorLoopEvent event : events) {

			CloseUtil.close(event);
		}
	}

	@Override
	public NioSocketChannelContext getChannelContext() {
		return context;
	}

	public ExecutorEventLoop getExecutorEventLoop() {
		return executorEventLoop;
	}

	@Override
	protected void doLoop() {

		try {

			SocketSelector selector = getSelector();

			int selected;

			// long last_select = System.currentTimeMillis();

			if (hasTask) {

				if (runTask-- > 0) {

					handlePositiveEvents(false);

					return;
				}

				selected = selector.selectNow();
			} else {

				if (selecting.compareAndSet(false, true)) {

					selected = selector.select(16);// FIXME try

					selecting.set(false);
				} else {

					selected = selector.selectNow();
				}
			}

			if (isWaitForRegist()) {

				waitForRegist();
			}

			if (selected < 1) {

				handleNegativeEvents();

				// selectEmpty(last_select);
			} else {

				List<NioSocketChannel> selectedChannels = selector.selectedChannels();

				for (NioSocketChannel channel : selectedChannels) {

					accept(channel);
				}

				selector.clearSelectedChannels();
			}

			handlePositiveEvents(true);

			if (isMainEventLoop()) {
				sessionManager.loop();
			}

		} catch (Throwable e) {

			logger.error(e.getMessage(), e);
		}
	}

	private SocketSelector rebuildSelector0() throws IOException {
		SocketSelector selector = selectorBuilder.build(this);

		//		Selector old = this.selector;
		//
		//		Set<SelectionKey> sks = old.keys();
		//
		//		if (sks.size() == 0) {
		//			logger.debug("sk size 0");
		//			CloseUtil.close(old);
		//			return selector;
		//		}
		//
		//		for (SelectionKey sk : sks) {
		//
		//			if (!sk.isValid() || sk.attachment() == null) {
		//				cancelSelectionKey(sk);
		//				continue;
		//			}
		//
		//			try {
		//				sk.channel().register(selector, SelectionKey.OP_READ);
		//			} catch (ClosedChannelException e) {
		//				cancelSelectionKey(sk, e);
		//			}
		//		}
		//
		//		CloseUtil.close(old);

		return selector;
	}

	@Override
	public SocketSelectorEventLoopGroup getEventLoopGroup() {
		return eventLoopGroup;
	}

	// FIXME 会不会出现这种情况，数据已经接收到本地，但是还没有被EventLoop处理完
	// 执行stop的时候如果确保不会再有数据进来
	@Override
	public void wakeup() {

		if (selecting.compareAndSet(false, true)) {
			selecting.set(false);
			return;
		}

		getSelector().wakeup();

		super.wakeup();
	}

	public void dispatch(SelectorLoopEvent event) throws RejectedExecutionException {

		//FIXME 找出这里出问题的原因
		//		if (inEventLoop()) {
		//
		//			if (!isRunning()) {
		//				CloseUtil.close(event);
		//				return;
		//			}
		//
		//			handleEvent(event);
		//
		//			return;
		//		}

		ReentrantLock lock = this.runLock;

		lock.lock();

		try {

			if (!isRunning()) {
				CloseUtil.close(event);
				return;
			}

			fireEvent(event);

		} finally {

			lock.unlock();
		}
	}

	private void fireEvent(SelectorLoopEvent event) {

		BufferedArrayList<SelectorLoopEvent> events = positiveEvents;

		if (events.getBufferSize() > eventQueueSize) {
			throw new RejectedExecutionException();
		}

		events.offer(event);

		wakeup();
	}

	private void handleEvent(SelectorLoopEvent event) {

		try {

			event.fireEvent(this);

			if (event.isComplete()) {
				return;
			}

			// FIXME xiaolv hui jiangdi
			if (event.isPositive()) {
				positiveEvents.offer(event);
				return;
			}

			negativeEvents.offer(event);

		} catch (IOException e) {

			CloseUtil.close(event);
		}
	}

	private void handleEvents(List<SelectorLoopEvent> eventBuffer) {

		for (SelectorLoopEvent event : eventBuffer) {

			handleEvent(event);
		}
	}

	private void handleNegativeEvents() {

		List<SelectorLoopEvent> eventBuffer = getEventBuffer(negativeEvents);

		if (eventBuffer.size() == 0) {
			return;
		}

		handleEvents(eventBuffer);
	}

	private void handlePositiveEvents(boolean refresh) {

		List<SelectorLoopEvent> eventBuffer = getEventBuffer(positiveEvents);

		if (eventBuffer.size() == 0) {

			hasTask = false;

			return;
		}

		handleEvents(eventBuffer);

		hasTask = positiveEvents.getBufferSize() > 0;

		if (hasTask && refresh) {
			runTask = 5;
		}
	}

	private List<SelectorLoopEvent> getEventBuffer(
			BufferedArrayList<SelectorLoopEvent> bufferedList) {
		ReentrantLock lock = this.runLock;
		lock.lock();
		try {
			return bufferedList.getBuffer();
		} finally {
			lock.unlock();
		}
	}

	protected void selectEmpty(SelectorEventLoop looper, long last_select) {

		long past = System.currentTimeMillis() - last_select;

		if (past < 1000) {

			if (!looper.isRunning() || past < 0) {
				return;
			}

			// JDK bug fired ?
			IOException e = new IOException("JDK bug fired ?");
			logger.error(e.getMessage(), e);
			logger.debug("last={},past={}", last_select, past);

			try {
				looper.rebuildSelector();
			} catch (IOException e1) {
				logger.error(e1.getMessage(), e1);
			}
		}
	}

	public interface SelectorLoopEvent extends Closeable {

		/**
		 * 返回该Event是否需要再次处理
		 * 
		 * @return true 需要再次处理，false处理结束后丢弃
		 */
		void fireEvent(SocketSelectorEventLoop selectLoop) throws IOException;
		
		boolean isComplete();

		boolean isPositive();
	}

}
