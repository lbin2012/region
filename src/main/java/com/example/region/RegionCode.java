package com.example.region;

import java.io.IOException;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 使用 Jsoup 采集民政部行政区划编码
 * 
 * @author : lin bin
 * @date : 2020/7/2
 */
public class RegionCode {

    public static void main(String[] args) throws IOException {

        // 民政部行政区划编码
        String url = "http://www.mca.gov.cn/article/sj/xzqh/2020/2020/202003301019.html";
        Document doc = Jsoup.connect(url).maxBodySize(0).timeout(100000).get();
        Elements trs = doc.select("tr");
        List<Code> codes = new ArrayList<>();

        for (Element tr : trs) {

            Elements tds = tr.select("td");
            if (tds.size() > 3) {
                String regionCode = tds.get(1).text();
                String regionArea = tds.get(2).text();
                String parentCode = "";

                if (validCode(regionCode)) {
                    int level;
                    if (!regionCode.endsWith("00")) {
                        level = 3;
                        parentCode = regionCode.substring(0, 4);
                    } else if (regionCode.endsWith("0000")) {
                        level = 1;
                        parentCode = "0";
                        regionCode = regionCode.substring(0, 2);
                    } else {
                        level = 2;
                        parentCode = regionCode.substring(0, 2);
                        regionCode = regionCode.substring(0, 4);
                    }
                    codes.add(new Code(regionCode, regionArea, parentCode, level));
                }
            }
        }

        make(codes);
        System.out.println("总数量为：" + codes.size());

        codes.sort(Comparator.comparing(Code::getRegionCode));
        codes.forEach(code -> {
            String content = String.format(
                "insert into s_region (region_code, region_name, parent_code, level)"
                    + " values ('%s', '%s', '%s', %s );",
                code.getRegionCode(), code.getRegionArea(), code.getParentCode(), code.getLevel());
            System.out.println(content);
        });
    }

    /**
     * 判断是否是行政编码
     * 
     * @param code
     * @return
     */
    public static boolean validCode(String code) {
        try {
            Integer.parseInt(code);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 数据清洗
     * 
     * @param codes
     * @return
     */
    public static List<Code> make(List<Code> codes) {

        List<Code> level2Codes = new ArrayList<>();
        List<Code> level3Codes = new ArrayList<>();
        Set<String> defect = new HashSet<>();

        for (Code code : codes) {

            String parentCode = code.getParentCode();
            String regionCode = code.getRegionCode();
            Integer level = code.getLevel();
            boolean level2exits = false;
            boolean level3exits = false;

            for (Code code2 : codes) {
                if (parentCode.equals(code2.getRegionCode())) {
                    level2exits = true;
                }
                if (regionCode.equals(code2.getParentCode())) {
                    level3exits = true;
                }
            }

            // 二级行政编码缺少
            if (!level2exits && level != 1) {
                defect.add(parentCode);
            }

            // 三级行政编码缺少，自动补齐同名市级为第三级
            if (!level3exits && level == 2) {
                level3Codes.add(new Code(regionCode + "00", code.getRegionArea(), regionCode, 3));
            }
        }

        for (String s : defect) {

            // 后缀为1 为直辖市
            if (s.endsWith("1")) {
                for (Code code : codes) {
                    if (s.substring(0, 2).equals(code.getRegionCode())) {
                        level2Codes.add(new Code(s, code.getRegionArea(), s.substring(0, 2), 2));
                        break;
                    }
                }
            } else {
                level2Codes.add(new Code(s, "省直辖县级行政区划", s.substring(0, 2), 2));
            }
        }
        codes.addAll(level2Codes);
        codes.addAll(level3Codes);

        // 港澳台增加编码 三级下拉框对齐
        codes.add(new Code("7100", "台湾省", "71", 2));
        codes.add(new Code("710000", "台湾省", "7100", 3));
        codes.add(new Code("8100", "香港特别行政区", "81", 2));
        codes.add(new Code("810000", "香港特别行政区", "8100", 3));
        codes.add(new Code("8200", "澳门特别行政区", "82", 2));
        codes.add(new Code("820000", "澳门特别行政区", "8200", 3));
        return codes;
    }
}
