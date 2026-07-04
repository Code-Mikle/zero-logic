package com.mikle.zerologic.service;

import com.mikle.zerologic.workflow.generation.asset.AssetResource;
import com.mikle.zerologic.workflow.generation.asset.AssetSearchRequest;

import java.util.List;

public interface AssetSearchService {

    List<AssetResource> search(AssetSearchRequest request);
}
