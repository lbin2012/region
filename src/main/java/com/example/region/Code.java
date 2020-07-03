package com.example.region;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author : lin bin
 * @date : 2020/7/3
 */
@Data
@AllArgsConstructor
public class Code {
    private String regionCode;
    private String regionArea;
    private String parentCode;
    private Integer level;
}
