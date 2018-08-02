package io.sailrocket.core.client.netty;

import io.sailrocket.api.HttpClient;
import io.sailrocket.core.client.HttpClientPool;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.vertx.core.http.HttpVersion;

import java.util.concurrent.TimeUnit;

public class NettyHttpClientPool implements HttpClientPool {

  private volatile EventLoopGroup workerGroup;
  private volatile HttpVersion protocol;
  private volatile int size;
  private volatile boolean ssl;
  private volatile int port;
  private volatile String host;
  private volatile int concurrency;
  private final ThreadLocal<EventLoop> currentEventLoop = ThreadLocal.withInitial(() -> workerGroup.next());

  @Override
  public HttpClientPool threads(int count) {
    if (workerGroup != null) {
      throw new IllegalStateException();
    }
    workerGroup = new NioEventLoopGroup(count);
    return this;
  }

  @Override
  public HttpClientPool ssl(boolean ssl) {
    this.ssl = ssl;
    return this;
  }

  public HttpClientPool protocol(HttpVersion protocol) {
    this.protocol = protocol;
    return this;
  }

  public HttpClientPool size(int size) {
    this.size = size;
    return this;
  }

  public HttpClientPool port(int port) {
    this.port = port;
    return this;
  }

  public HttpClientPool host(String host) {
    this.host = host;
    return this;
  }

  public HttpClientPool concurrency(int maxConcurrency) {
    this.concurrency = maxConcurrency;
    return this;
  }

  @Override
  public HttpClient build() throws Exception {
    return HttpClientImpl.create(currentEventLoop.get(), protocol, ssl, size, port, host, concurrency);
  }

  @Override
  public void shutdown() {
    workerGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS);
  }
}