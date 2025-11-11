package com.logwise.orchestrator.service;

import com.google.inject.Inject;
import com.logwise.orchestrator.config.ApplicationConfig.DelayMetricsConfig;
import com.logwise.orchestrator.config.ApplicationConfig.TenantConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.response.LogSyncDelayResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Single;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class MetricsService {

  /** Compute application log sync delay in minutes and return as DTO. */
  public Single<LogSyncDelayResponse> computeLogSyncDelay(Tenant tenant) {
    return computeApplicationLogSyncDelayForAws(tenant)
        .map(
            appDelay ->
                LogSyncDelayResponse.builder()
                    .tenant(tenant.getValue())
                    .appLogsDelayMinutes(appDelay)
                    .build());
  }

  private Single<Integer> computeApplicationLogSyncDelayForAws(Tenant tenant) {
    TenantConfig config = ApplicationConfigUtil.getTenantConfig(tenant);
    DelayMetricsConfig delayMetricsConfig = config.getDelayMetrics();
    LocalDateTime nowTime = LocalDateTime.now(ZoneOffset.UTC);
    List<String> prefixList =
        getPrefixList(
            nowTime,
            config.getSpark().getLogsDir(),
            delayMetricsConfig.getApp().getSampleEnv(),
            delayMetricsConfig.getApp().getSampleServiceName(),
            delayMetricsConfig.getApp().getSampleComponentName());

    return ApplicationUtils.executeBlockingCallable(
            () -> {
              Integer computed = null;
              for (String prefix : prefixList) {
                List<String> objNames =
                    ObjectStoreFactory.getClient(tenant).listObjects(prefix).blockingGet();
                if (!objNames.isEmpty()) {
                  objNames.sort(Collections.reverseOrder());
                  Matcher matcher =
                      Pattern.compile("hour=(\\d{2})/minute=(\\d{2})").matcher(objNames.get(0));
                  if (matcher.find()) {
                    int objHour = Integer.parseInt(matcher.group(1));
                    int objMinute = Integer.parseInt(matcher.group(2));
                    int nowHour = nowTime.getHour();
                    int nowMinute = nowTime.getMinute();
                    int timeDiff = (nowHour * 60 + nowMinute) - (objHour * 60 + objMinute);
                    computed = Math.max(1, timeDiff);
                    break;
                  }
                }
              }
              if (computed == null) {
                computed = ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS * 60;
              }
              return computed;
            })
        .toSingle();
  }

  private static List<String> getPrefixList(
      LocalDateTime nowTime, String dir, String env, String service, String component) {
    List<String> dirPrefixList = new ArrayList<>();
    for (int deltaHours = 0;
        deltaHours <= ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS;
        deltaHours++) {
      LocalDateTime time = nowTime.minusHours(deltaHours);
      String month = String.format("%02d", time.getMonthValue());
      String day = String.format("%02d", time.getDayOfMonth());
      String hour = String.format("%02d", time.getHour());
      String dirPrefix =
          String.format(
              "%s/env=%s/service_name=%s/component_name=%s/year=%d/month=%s/day=%s/hour=%s/minute=",
              dir, env, service, component, time.getYear(), month, day, hour);
      if (!dirPrefixList.contains(dirPrefix)) {
        dirPrefixList.add(dirPrefix);
      }
    }
    log.info("dirPrefixList: {}", dirPrefixList);
    return dirPrefixList;
  }
}
