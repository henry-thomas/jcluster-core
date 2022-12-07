/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster.hzUtils;

import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;

/**
 *
 * @author henry
 */
public class HzLifeCycleListener implements LifecycleListener{

    @Override
    public void stateChanged(LifecycleEvent event) {
        LifecycleEvent.LifecycleState state = event.getState();
        switch (state) {
            case CLIENT_CONNECTED:
                System.out.println("CLIENT CONNECTED EVENT");
                break;
            case CLIENT_DISCONNECTED:
                System.out.println("CLIENT DISCONNECTED EVENT");
                break;
            case CLIENT_CHANGED_CLUSTER:
                System.out.println("CLIENT CHANGED CLUSTER EVENT");
                break;
            case STARTED:
                System.out.println("STARTED MEMBER EVENT");
                break;
            case STARTING:
                System.out.println("STARTING MEMBER EVENT");
                break;
            case SHUTTING_DOWN:
                System.out.println("SHUTTING DOWN MEMBER EVENT");
                break;
            case SHUTDOWN:
                System.out.println("SHUT DOWN MEMBER EVENT");
                break;
            case MERGING:
                System.out.println("MERGING MEMBER EVENT");
                break;
            case MERGED:
                System.out.println("MERGED MEMBER EVENT");
                break;
            case MERGE_FAILED:
                System.out.println("MERGED MEMBER EVENT");
                break;
            default:
                throw new AssertionError();
        }
    }
    
}
