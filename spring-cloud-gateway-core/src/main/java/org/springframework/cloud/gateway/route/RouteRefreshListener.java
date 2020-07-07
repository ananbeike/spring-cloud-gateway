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

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;

/**
 * @see org.springframework.cloud.gateway.filter.WeightCalculatorWebFilter
 */
// see ZuulDiscoveryRefreshListener
// TODO: make abstract class in commons?
public class RouteRefreshListener implements ApplicationListener<ApplicationEvent>{

    private final ApplicationEventPublisher publisher;

    private HeartbeatMonitor monitor = new HeartbeatMonitor();

    public RouteRefreshListener(ApplicationEventPublisher publisher){
        Assert.notNull(publisher, "publisher may not be null");
        this.publisher = publisher;
    }

	/**
	 * ContextRefreshedEvent 是在 ApplicationContext.refresh() 执行完成后触发，即 Context 初始化全部完成。
	 *
	 * @param event
	 */
    @Override
    public void onApplicationEvent(ApplicationEvent event){
        if (event instanceof ContextRefreshedEvent || event instanceof RefreshScopeRefreshedEvent || event instanceof InstanceRegisteredEvent){
        	// RefreshRoutesEvent
            reset();
        }else if (event instanceof ParentHeartbeatEvent){
            ParentHeartbeatEvent e = (ParentHeartbeatEvent) event;
            resetIfNeeded(e.getValue());
        }else if (event instanceof HeartbeatEvent){
            HeartbeatEvent e = (HeartbeatEvent) event;
            resetIfNeeded(e.getValue());
        }
    }

    private void resetIfNeeded(Object value){
        if (this.monitor.update(value)){
            reset();
        }
    }

	/**
	 * WeightCalculatorWebFilter 类监听到 RefreshRoutesEvent 事件，触发调用 CachingRouteLocator#getRoutes() 获取 Route 集合
	 */
    private void reset(){
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

}
