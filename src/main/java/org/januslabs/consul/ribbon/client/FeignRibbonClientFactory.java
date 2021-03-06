package org.januslabs.consul.ribbon.client;

import org.januslabs.consul.ribbon.ConsulRibbonLoadBalancer;
import org.januslabs.consul.ribbon.ConsulServerListBuilder;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

import feign.Logger;
import feign.Retryer;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.slf4j.Slf4jLogger;


public class FeignRibbonClientFactory {

  private ZoneAwareLoadBalancer<Server> loadbalancer;

  public FeignRibbonClientFactory(String openWeatherServiceId) {
    loadbalancer =
        new ConsulRibbonLoadBalancer(new ConsulServerListBuilder()).build(openWeatherServiceId);
  }

  public <T> T target(Class<T> type, String servicename) {

    return HystrixFeign.builder().logger(new Slf4jLogger()).retryer(new Retryer.Default())
        .logLevel(Logger.Level.FULL).decoder(new JacksonDecoder())
        .target(type, loadbalance(servicename).getHostPort());
  }

  public static <T> T loadbalanceTarget(Class<T> type, String servicename) {

    return HystrixFeign.builder().logger(new Slf4jLogger()).retryer(new Retryer.Default())
        .logLevel(Logger.Level.FULL).decoder(new JacksonDecoder())
        .target(ZoneAwareBalancingTarget.create(type, servicename));
  }

  private Server loadbalance(String key) {
    final Server server = loadbalancer.chooseServer(key);
    if (server == null) {
      throw new IllegalStateException("No available servers for " + loadbalancer.getName());
    }
    return server;
  }

}
