package com.logwise.orchestrator.dto.response;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
public class SparkMasterJsonResponse {
  @NonFinal List<App> activeapps;
  @NonFinal int memory;
  @NonFinal Object resources;
  @NonFinal List<Driver> completeddrivers;
  @NonFinal String url;
  @NonFinal Integer cores;
  @NonFinal List<Driver> activedrivers;
  @NonFinal int coresused;
  @NonFinal List<App> completedapps;
  @NonFinal int aliveworkers;
  @NonFinal Object resourcesused;
  @NonFinal List<Worker> workers;
  @NonFinal int memoryused;
  @NonFinal String status;

  @Data
  @NoArgsConstructor
  public static class App {
    @NonFinal long duration;
    @NonFinal int memoryperexecutor;
    @NonFinal int cores;
    @NonFinal List<Object> resourcesperslave;
    @NonFinal String name;
    @NonFinal int memoryperslave;
    @NonFinal String id;
    @NonFinal long starttime;
    @NonFinal String submitdate;
    @NonFinal String state;
    @NonFinal String user;
    @NonFinal List<Object> resourcesperexecutor;
  }

  @Data
  public static class Driver {
    @NonFinal String mainclass;
    @NonFinal int cores;
    @NonFinal int memory;
    @NonFinal Object resources;
    @NonFinal String id;
    @NonFinal String starttime;
    @NonFinal String state;
    @NonFinal String submitdate;
    @NonFinal String worker;
  }

  @Data
  @NoArgsConstructor
  public static class Worker {
    @NonFinal int memory;
    @NonFinal Object resourcesfree;
    @NonFinal String webuiaddress;
    @NonFinal Object resources;
    @NonFinal int cores;
    @NonFinal int port;
    @NonFinal int coresused;
    @NonFinal long lastheartbeat;
    @NonFinal int memoryfree;
    @NonFinal String host;
    @NonFinal String id;
    @NonFinal String state;
    @NonFinal Object resourcesused;
    @NonFinal int coresfree;
    @NonFinal int memoryused;
  }
}
