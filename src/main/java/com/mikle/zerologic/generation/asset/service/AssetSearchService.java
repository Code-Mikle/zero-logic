package com.mikle.zerologic.generation.asset.service;

import com.mikle.zerologic.generation.asset.model.AssetResource;
import com.mikle.zerologic.generation.asset.model.AssetSearchRequest;

import java.util.List;

public interface AssetSearchService {

    List<AssetResource> search(AssetSearchRequest request);
}
