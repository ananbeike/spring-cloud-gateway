/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.route;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

/**
 *
 * @see CompositeRouteLocator
 * @see RouteDefinitionRouteLocator
 * @see CompositeRouteDefinitionLocator
 *
 * @author Spencer Gibb
 */
public class CachingRouteLocator implements Ordered,RouteLocator,ApplicationListener<RefreshRoutesEvent>{

    private static final String CACHE_KEY = "routes";

    /**
     * delegate 代理具体类型是 CachingRouteLocator -> CompositeRouteLocator ，
     * CompositeRouteLocator ，汇聚了所有的 RouteLocator 集合，主要包含两类：
     * 一类是 RouteDefinitionRouteLocator ，基于 RouteDefinition 获取 Route ；
     * 另一类是编程方式自定义创建 RouteLocator
     */
    private final RouteLocator delegate;

    private final Flux<Route> routes;

    private final Map<String, List> cache = new ConcurrentHashMap<>();

    public CachingRouteLocator(RouteLocator delegate){
        this.delegate = delegate;
		//构造方法中初始化 routes ，由于是异步的这时并没有真正的触发底层执行，只有在调用 locator.getRoutes() 真正使用到 routes 时才会触发底层调用。
		// 所以， WeightCalculatorWebFilter 中监听事件调用 locator.getRoutes() 就是触发执行。
        routes = CacheFlux.lookup(cache, CACHE_KEY, Route.class).onCacheMissResume(this::fetch);
    }

    private Flux<Route> fetch(){
        return this.delegate.getRoutes().sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    @Override
    public Flux<Route> getRoutes(){
        return this.routes;
    }

    /**
     * Clears the routes cache.
     * 
     * @return routes flux
     */
    public Flux<Route> refresh(){
        this.cache.clear();
        return this.routes;
    }

    @Override
    public void onApplicationEvent(RefreshRoutesEvent event){
        fetch().materialize().collect(Collectors.toList()).doOnNext(routes -> cache.put(CACHE_KEY, routes)).subscribe();
    }

    @Deprecated
    /* for testing */ void handleRefresh(){
        refresh();
    }

    @Override
    public int getOrder(){
        return 0;
    }

}
