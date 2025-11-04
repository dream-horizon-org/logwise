package com.logwise.spark.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class SparkMasterJsonResponse {
  private List<App> activeapps;
  private int memory;
  private Object resources;
  private List<Driver> completeddrivers;
  private String url;
  private int cores;
  private List<Driver> activedrivers;
  private int coresused;
  private List<App> completedapps;
  private int aliveworkers;
  private Object resourcesused;
  private List<Worker> workers;
  private int memoryused;
  private String status;

  @Data
  public static class App {
    private long duration;
    private int memoryperexecutor;
    private int cores;
    private List<Object> resourcesperslave;
    private String name;
    private int memoryperslave;
    private String id;
    private long starttime;
    private String submitdate;
    private String state;
    private String user;
    private List<Object> resourcesperexecutor;
  }

  @Data
  public static class Driver {
    private String mainclass;
    private int cores;
    private int memory;
    private Object resources;
    private String id;
    private String starttime;
    private String state;
    private String submitdate;
    private String worker;
  }

  @Data
  public static class Worker {
    private int memory;
    private Object resourcesfree;
    private String webuiaddress;
    private Object resources;
    private int cores;
    private int port;
    private int coresused;
    private long lastheartbeat;
    private int memoryfree;
    private String host;
    private String id;
    private String state;
    private Object resourcesused;
    private int coresfree;
    private int memoryused;
  }
}
