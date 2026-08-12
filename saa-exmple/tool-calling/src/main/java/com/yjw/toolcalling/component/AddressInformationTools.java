package com.yjw.toolcalling.component;

import com.alibaba.cloud.ai.toolcalling.baidumap.BaiduMapSearchInfoService;

/**
 * 百度地图获取地址
 * @author yjw
 * @date 2023/07/07
 */

public class AddressInformationTools {

    private final BaiduMapSearchInfoService service;

    public AddressInformationTools(BaiduMapSearchInfoService service) {
        this.service = service;
    }

    public String getAddressInformation(String address) {

        return service.apply(new BaiduMapSearchInfoService.Request(address)).message();
    }

}
