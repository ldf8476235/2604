package com.deltatrade.platform.modules.listing.service;

import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChinaCityCatalog {

    private final List<ProvinceNode> provinces;
    private final Map<String, ProvinceNode> provinceMap;

    public ChinaCityCatalog(ObjectMapper objectMapper) {
        try (InputStream inputStream = new ClassPathResource("china-city-data.json").getInputStream()) {
            List<ProvinceNode> loaded = objectMapper.readValue(inputStream, new TypeReference<List<ProvinceNode>>() {
            });
            this.provinces = Collections.unmodifiableList(loaded);
            Map<String, ProvinceNode> mapping = new LinkedHashMap<String, ProvinceNode>();
            for (ProvinceNode province : loaded) {
                mapping.put(province.getCode(), province);
            }
            this.provinceMap = Collections.unmodifiableMap(mapping);
        } catch (IOException exception) {
            throw new IllegalStateException("加载行政区数据失败", exception);
        }
    }

    public List<ProvinceNode> getProvinces() {
        return provinces;
    }

    public ResolvedCity resolve(String provinceCode, String cityCode) {
        ProvinceNode province = provinceMap.get(provinceCode);
        if (province == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择有效的省份");
        }
        for (CityNode city : province.getCities()) {
            if (city.getCode().equals(cityCode)) {
                return new ResolvedCity(province.getCode(), province.getName(), city.getCode(), city.getName());
            }
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择有效的城市");
    }

    public static class ProvinceNode {
        private String code;
        private String name;
        private List<CityNode> cities = Collections.emptyList();

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<CityNode> getCities() {
            return cities;
        }

        public void setCities(List<CityNode> cities) {
            this.cities = cities;
        }
    }

    public static class CityNode {
        private String code;
        private String name;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ResolvedCity {
        private final String provinceCode;
        private final String provinceName;
        private final String cityCode;
        private final String cityName;

        public ResolvedCity(String provinceCode, String provinceName, String cityCode, String cityName) {
            this.provinceCode = provinceCode;
            this.provinceName = provinceName;
            this.cityCode = cityCode;
            this.cityName = cityName;
        }

        public String getProvinceCode() {
            return provinceCode;
        }

        public String getProvinceName() {
            return provinceName;
        }

        public String getCityCode() {
            return cityCode;
        }

        public String getCityName() {
            return cityName;
        }
    }
}
