package com.logwise.orchestrator.client;

import static com.logwise.orchestrator.config.ApplicationConfig.ObjectStoreConfig;

import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;

public interface ObjectStoreClient {
  Completable rxConnect(ObjectStoreConfig config);

  Single<List<String>> listCommonPrefix(String prefix, String delimiter);

  Single<List<String>> listObjects(String prefix);

  Completable deleteFile(String objectKey);
}
